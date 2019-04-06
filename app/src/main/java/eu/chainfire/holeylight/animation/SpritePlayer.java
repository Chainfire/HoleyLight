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
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.Locale;

import androidx.annotation.NonNull;

public class SpritePlayer extends SurfaceView {
    public interface OnSpriteSheetNeededListener {
        SpriteSheet onSpriteSheetNeeded(int width, int height);
    }

    public interface OnAnimationCompleteListener {
        boolean onAnimationComplete();
    }

    private HandlerThread handlerThread = null;
    private Handler handler = null;
    private Choreographer choreographer = null;

    private OnSpriteSheetNeededListener onSpriteSheetNeededListener = null;
    private OnAnimationCompleteListener onAnimationCompleteListener = null;

    private int frame = -1;
    private SpriteSheet spriteSheet = null;
    private Rect dest = new Rect();
    private Paint paint = new Paint();
    private boolean surfaceInvalidated = true;
    private boolean draw = false;
    private boolean wanted = false;
    private int width = -1;
    private int height = -1;
    private int color = 0;
    private float speed = 1.0f;

    private void log(String fmt, Object... args) {
        Log.d("HoleyLight/SpritePlayer", String.format(Locale.ENGLISH, fmt, args));
    }

    public SpritePlayer(Context context) {
        super(context);
        init();
    }

    public SpritePlayer(Context context, OnSpriteSheetNeededListener onSpriteSheetNeededListener) {
        super(context);
        init();
        setOnSpriteSheetNeededListener(onSpriteSheetNeededListener);
    }

    public SpritePlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpritePlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public SpritePlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        handlerThread = new HandlerThread("SpritePlayer");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);

        handler.post(() -> choreographer = Choreographer.getInstance());

        getHolder().setFormat(PixelFormat.RGBA_8888);
        getHolder().addCallback(surfaceCallback);

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
                callOnSpriteSheetNeeded(width, height);
                surfaceInvalidated = true;
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            cancelNextFrame();
        }
    };

    private Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        private long startTimeNanos = 0;
        private int lastFrameDrawn = -1;

        @Override
        public void doFrame(long frameTimeNanos) {
            synchronized (SpritePlayer.this) {
                if (draw && (spriteSheet != null)) {
                    if (frame == -1) {
                        startTimeNanos = frameTimeNanos;
                        frame = 0;
                    } else {
                        double frameTime = (double)1000000000 / ((double)spriteSheet.getFrameRate() * (double)speed);
                        frame = (int)Math.floor((double)(frameTimeNanos - startTimeNanos)/frameTime);
                    }

                    int drawFrame = Math.max(Math.min(frame, spriteSheet.getFrames() - 1), 0);
                    if ((drawFrame != lastFrameDrawn) || surfaceInvalidated) {
                        surfaceInvalidated = false;
                        lastFrameDrawn = drawFrame;

                        // Software canvas 2x quicker than hardware during tests
                        Canvas canvas = getHolder().lockCanvas();
                        if (canvas != null) {
                            try {
                                if (!canvas.isHardwareAccelerated()) {
                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                                }
                                if (color != 0) {
                                    paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
                                }
                                SpriteSheet.Sprite sprite = spriteSheet.getFrame(drawFrame);
                                Bitmap bitmap = sprite.getBitmap();
                                if (!bitmap.isRecycled()) {
                                    canvas.drawBitmap(sprite.getBitmap(), sprite.getArea(), dest, paint);
                                }
                            } finally {
                                try {
                                    getHolder().unlockCanvasAndPost(canvas);
                                } catch (IllegalStateException e) {
                                    // no action
                                }
                            }
                        }
                    }
                    if (frame >= spriteSheet.getFrames()) {
                        frame = -1;
                        if ((onAnimationCompleteListener == null) || !onAnimationCompleteListener.onAnimationComplete()) {
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
        resetSpriteSheet();
        handler.post(() -> {
            synchronized (SpritePlayer.this) {
                if (onSpriteSheetNeededListener != null) {
                    setSpriteSheet(onSpriteSheetNeededListener.onSpriteSheetNeeded(width, height));
                }
            }
        });
    }

    public synchronized void setOnSpriteSheetNeededListener(OnSpriteSheetNeededListener onSpriteSheetNeededListener) {
        if (this.onSpriteSheetNeededListener == onSpriteSheetNeededListener) return;

        this.onSpriteSheetNeededListener = onSpriteSheetNeededListener;
        if ((width != -1) && (height != -1) && !((spriteSheet != null) && (spriteSheet.getWidth() == width) && spriteSheet.getHeight() == height)) {
            callOnSpriteSheetNeeded(width, height);
        }
    }

    public void setOnAnimationCompleteListener(OnAnimationCompleteListener onAnimationCompleteListener) {
        this.onAnimationCompleteListener = onAnimationCompleteListener;
    }

    private synchronized void resetSpriteSheet() {
        frame = -1;
        SpriteSheet old = spriteSheet;
        spriteSheet = null;
        if (old != null) {
            old.recycle();
        }
        surfaceInvalidated = true;
        try {
            Canvas canvas = getHolder().lockCanvas();
            try {
                if (!canvas.isHardwareAccelerated()) {
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                }
            } finally {
                getHolder().unlockCanvasAndPost(canvas);
            }
        } catch (Throwable t) {
            // ...
        }
    }

    public synchronized void setSpriteSheet(SpriteSheet spriteSheet) {
        if (spriteSheet == this.spriteSheet) return;
        resetSpriteSheet();
        this.spriteSheet = spriteSheet;
        evaluate();
    }

    public void setColor(int color) {
        this.color = color;
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
        if (wanted && (spriteSheet != null) && (getWindowVisibility() == View.VISIBLE) && (getVisibility() == View.VISIBLE)) {
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
}
