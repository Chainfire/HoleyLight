/*
 * Copyright (C) 2019 Jorrit "Chainfire" Jongma
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package eu.chainfire.holeylight.animation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;
import android.widget.ImageView;

import androidx.annotation.NonNull;

@SuppressWarnings({ "deprecation", "FieldCanBeLocal" })
public class SpritePlayer extends AbsoluteLayout {
    public enum Mode { SWIRL, BLINK, SINGLE }

    public interface OnSpriteSheetNeededListener {
        SpriteSheet onSpriteSheetNeeded(int width, int height, Mode mode);
    }

    public interface OnAnimationListener {
        boolean onAnimationFrameStart(boolean draw);
        void onAnimationFrameEnd(boolean draw);
        boolean onAnimationComplete();
    }

    private final HandlerThread handlerThread;
    private final Handler handler;
    private Choreographer choreographer;

    private final SurfaceView surfaceView;
    private final ImageView imageView;

    private OnSpriteSheetNeededListener onSpriteSheetNeededListener = null;
    private OnAnimationListener onAnimationListener = null;

    private int frame = -1;
    private SpriteSheet spriteSheetSwirl = null;
    private SpriteSheet spriteSheetBlink = null;
    private SpriteSheet spriteSheetSingle = null;
    private Rect dest = new Rect();
    private Rect destDouble = new Rect();
    private Paint paint = new Paint();
    private boolean surfaceInvalidated = true;
    private boolean draw = false;
    private boolean wanted = false;
    private int width = -1;
    private int height = -1;
    private int[] colors = null;
    private float speed = 1.0f;
    private Mode drawMode = Mode.SWIRL;
    private Bitmap bitmap = null;
    private int[] bitmapLastColors = null;

    public SpritePlayer(Context context) {
        super(context);

        handlerThread = new HandlerThread("SpritePlayer");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);

        handler.post(() -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            choreographer = Choreographer.getInstance();
        });
        
        AbsoluteLayout.LayoutParams params = new AbsoluteLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 0, 0);

        surfaceView = new SurfaceView(context);
        surfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        surfaceView.getHolder().addCallback(surfaceCallback);
        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.setLayoutParams(new AbsoluteLayout.LayoutParams(params));
        addView(surfaceView);

        imageView = new ImageView(context);
        imageView.setVisibility(View.INVISIBLE);
        imageView.setLayoutParams(new AbsoluteLayout.LayoutParams(params));
        addView(imageView);

        while (choreographer == null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // no action
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        handlerThread.quitSafely();
        super.finalize();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        evaluate();
    }

    private SurfaceHolder.Callback2 surfaceCallback = new SurfaceHolder.Callback2() {
        @Override
        public void surfaceRedrawNeeded(SurfaceHolder holder) {
            surfaceInvalidated = true;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceInvalidated = true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            synchronized (SpritePlayer.this) {
                SpritePlayer.this.width = width;
                SpritePlayer.this.height = height;
                dest.set(0, 0, width, height);
                destDouble.set(dest.centerX() - width, dest.centerY() - height, dest.centerX() + width, dest.centerY() + height);
                callOnSpriteSheetNeeded(width, height);
                surfaceInvalidated = true;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            cancelNextFrame();
        }
    };

    private boolean colorsChanged(int[] lastColors) {
        if ((lastColors == null) != (colors == null)) return true;
        if (lastColors == null) return false;
        if (lastColors.length != colors.length) return true;
        if (lastColors.length == 0) return false;
        for (int i = 0; i < lastColors.length; i++) {
            if (lastColors[i] != colors[i]) return true;
        }
        return false;
    }

    private Bitmap renderSingleFrame() {
        if (drawMode != Mode.SINGLE) return null;
        if ((width == -1) || (height == -1)) return null;
        if (spriteSheetSingle == null) return null;
        if ((spriteSheetSingle.getWidth() != width) || (spriteSheetSingle.getHeight() != height)) {
            callOnSpriteSheetNeeded(width, height);
        }
        boolean colorsChanged = colorsChanged(bitmapLastColors);
        if ((bitmap == null) || (bitmap.getWidth() != width) || (bitmap.getHeight() != height)) {
            if (bitmap != null) bitmap.recycle();
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            colorsChanged = true;
        }
        if (bitmap == null) {
            return bitmap;
        }
        if (!colorsChanged) {
            return bitmap;
        }
        Canvas canvas = new Canvas(bitmap);
        renderFrame(canvas, spriteSheetSingle, 0);
        handler.post(() -> imageView.setImageBitmap(bitmap));
        return bitmap;
    }

    private void renderFrame(Canvas canvas, SpriteSheet spriteSheet, int frame) {
        if (!canvas.isHardwareAccelerated()) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        paint.setXfermode(null);
        paint.setColor(Color.WHITE);
        if ((colors != null) && (colors.length == 1)) {
            // fast single-color mode
            paint.setColorFilter(new PorterDuffColorFilter(colors[0], PorterDuff.Mode.SRC_ATOP));
        } else {
            paint.setColorFilter(null);
        }
        SpriteSheet.Sprite sprite = spriteSheet.getFrame(frame);
        Bitmap bitmap = sprite.getBitmap();
        if (!bitmap.isRecycled()) {
            canvas.drawBitmap(sprite.getBitmap(), sprite.getArea(), dest, paint);
        }
        if ((colors != null) && (colors.length > 1)) {
            // slower multi-colored mode
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

            float startAngle = 0;
            float anglePerColor = 360f / colors.length;
            for (int i = 0; i < colors.length; i++) {
                // we use double size here because the arc may cut off the larger S10+ animation otherwise
                paint.setColor(colors[i]);
                canvas.drawArc(destDouble.left, destDouble.top, destDouble.right, destDouble.bottom, startAngle + 270 + (anglePerColor * i), anglePerColor, true, paint);
            }
        }
    }

    private Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        private long startTimeNanos = 0;
        private int lastFrameDrawn = -1;
        private int[] lastColors = null;

        @Override
        public void doFrame(long frameTimeNanos) {
            synchronized (SpritePlayer.this) {
                SpriteSheet spriteSheet = getSpriteSheet();
                if (draw && (spriteSheet != null)) {
                    if (frame == -1) {
                        startTimeNanos = frameTimeNanos;
                        frame = 0;
                    } else {
                        double frameTime = (double)1000000000 / ((double)spriteSheet.getFrameRate() * (double)speed);
                        frame = (int)Math.floor((double)(frameTimeNanos - startTimeNanos)/frameTime);
                    }

                    int drawFrame = Math.max(Math.min(frame, spriteSheet.getFrames() - 1), 0);
                    boolean doDraw = ((drawFrame != lastFrameDrawn) || colorsChanged(lastColors) || surfaceInvalidated);
                    if (onAnimationListener != null) {
                        doDraw = onAnimationListener.onAnimationFrameStart(doDraw);
                    }
                    if (doDraw) {
                        surfaceInvalidated = false;
                        lastFrameDrawn = drawFrame;
                        lastColors = colors;

                        // Software canvas 2x quicker than hardware during tests
                        Canvas canvas = surfaceView.getHolder().lockCanvas();
                        if (canvas != null) {
                            try {
                                renderFrame(canvas, getSpriteSheet(), drawFrame);
                            } finally {
                                try {
                                    surfaceView.getHolder().unlockCanvasAndPost(canvas);
                                } catch (IllegalStateException e) {
                                    // no action
                                }
                            }
                        }
                    }
                    if (onAnimationListener != null) {
                        onAnimationListener.onAnimationFrameEnd(doDraw);
                    }
                    if (frame >= spriteSheet.getFrames()) {
                        frame = -1;
                        if ((onAnimationListener == null) || !onAnimationListener.onAnimationComplete()) {
                            draw = false;
                        }
                    }
                }
                if (draw) callNextFrame();
            }
        }
    };

    private void cancelNextFrame() {
        choreographer.removeFrameCallback(frameCallback);
    }

    private void callNextFrame() {
        cancelNextFrame();
        choreographer.postFrameCallback(frameCallback);
    }

    private void callOnSpriteSheetNeeded(int width, int height) {
        cancelNextFrame();
        resetSpriteSheet(null);
        handler.post(() -> {
            synchronized (SpritePlayer.this) {
                if (onSpriteSheetNeededListener != null) {
                    setSpriteSheet(onSpriteSheetNeededListener.onSpriteSheetNeeded(width, height, Mode.SWIRL), Mode.SWIRL);
                    setSpriteSheet(onSpriteSheetNeededListener.onSpriteSheetNeeded(width, height, Mode.BLINK), Mode.BLINK);
                    setSpriteSheet(onSpriteSheetNeededListener.onSpriteSheetNeeded(width, height, Mode.SINGLE), Mode.SINGLE);
                    renderSingleFrame();
                }
            }
        });
    }

    public synchronized void setOnSpriteSheetNeededListener(OnSpriteSheetNeededListener onSpriteSheetNeededListener) {
        if (this.onSpriteSheetNeededListener == onSpriteSheetNeededListener) return;

        this.onSpriteSheetNeededListener = onSpriteSheetNeededListener;
        if (
                (width != -1) && (height != -1) && 
                !((spriteSheetSwirl != null) && (spriteSheetSwirl.getWidth() == width) && spriteSheetSwirl.getHeight() == height) &&
                !((spriteSheetBlink != null) && (spriteSheetBlink.getWidth() == width) && spriteSheetBlink.getHeight() == height)
        ) {
            callOnSpriteSheetNeeded(width, height);
        }
    }

    public void setOnAnimationListener(OnAnimationListener onAnimationListener) {
        this.onAnimationListener = onAnimationListener;
    }

    private synchronized void resetSpriteSheet(Mode mode) {
        if ((mode == null) || (drawMode == mode)) {
            frame = -1;
        }
        if ((mode == null) || (mode == Mode.SWIRL)) {
            SpriteSheet old = spriteSheetSwirl;
            spriteSheetSwirl = null;
            if (old != null) old.recycle();
        }
        if ((mode == null) || (mode == Mode.BLINK)) {
            SpriteSheet old = spriteSheetBlink;
            spriteSheetBlink = null;
            if (old != null) old.recycle();
        }
        if ((mode == null) || (mode == Mode.SINGLE)) {
            SpriteSheet old = spriteSheetSingle;
            spriteSheetSingle = null;
            if (old != null) old.recycle();
        }
        if ((mode == null) || (drawMode == mode)) {
            surfaceInvalidated = true;
            try {
                Canvas canvas = surfaceView.getHolder().lockCanvas();
                try {
                    if (!canvas.isHardwareAccelerated()) {
                        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                    }
                } finally {
                    surfaceView.getHolder().unlockCanvasAndPost(canvas);
                }
            } catch (Throwable t) {
                // ...
            }
        }
    }

    public synchronized void setSpriteSheet(SpriteSheet spriteSheet, Mode mode) {
        if (
                ((mode == Mode.SWIRL) && (spriteSheet == this.spriteSheetSwirl)) ||
                ((mode == Mode.BLINK) && (spriteSheet == this.spriteSheetBlink)) ||
                ((mode == Mode.SINGLE) && (spriteSheet == this.spriteSheetSingle))
        ) return;
        resetSpriteSheet(mode);
        switch (mode) {
            case SWIRL:
                this.spriteSheetSwirl = spriteSheet;
                break;
            case BLINK:
                this.spriteSheetBlink = spriteSheet;
                break;
            case SINGLE:
                this.spriteSheetSingle = spriteSheet;
                break;
        }
        evaluate();
    }

    public void setColors(int[] colors) {
        this.colors = colors;
        renderSingleFrame();
    }

    private void startUpdating() {
        draw = true;
        callNextFrame();
    }

    private void stopUpdating() {
        draw = false;
        cancelNextFrame();
    }

    private synchronized void evaluate() {
        if (wanted && (getSpriteSheet() != null) && (getWindowVisibility() == View.VISIBLE) && (getVisibility() == View.VISIBLE)) {
            startUpdating();
        } else {
            stopUpdating();
        }
    }

    public synchronized void playAnimation() {
        wanted = true;
        frame = -1;
        evaluate();
    }

    public synchronized void cancelAnimation() {
        wanted = false;
        evaluate();
    }

    public boolean isAnimating() {
        return draw;
    }

    public synchronized void setSpeed(float speed) {
        frame = -1;
        this.speed = speed;
    }

    public Mode getMode() {
        return drawMode;
    }

    public synchronized void setMode(Mode mode) {
        if (mode != drawMode) {
            frame = -1;
            drawMode = mode;
            surfaceInvalidated = true;
            surfaceView.setVisibility(mode != Mode.SINGLE ? View.VISIBLE : View.INVISIBLE);
            imageView.setVisibility(mode == Mode.SINGLE ? View.VISIBLE : View.INVISIBLE);
            renderSingleFrame();
            evaluate();
        }
    }

    private synchronized SpriteSheet getSpriteSheet() {
        switch (drawMode) {
            case SWIRL: return spriteSheetSwirl;
            case BLINK: return spriteSheetBlink;
            case SINGLE: return spriteSheetSingle;
        }
        return null;
    }

    public synchronized void updateInternalViews(int width, int height) {
        AbsoluteLayout.LayoutParams params;

        params = (AbsoluteLayout.LayoutParams)surfaceView.getLayoutParams();
        params.width = width;
        params.height = height;
        surfaceView.setLayoutParams(params);

        params = (AbsoluteLayout.LayoutParams)imageView.getLayoutParams();
        params.width = width;
        params.height = height;
        imageView.setLayoutParams(params);

        this.width = width;
        this.height = height;

        renderSingleFrame();
    }
}
