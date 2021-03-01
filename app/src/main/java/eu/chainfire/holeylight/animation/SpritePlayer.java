/*
 * Copyright (C) 2019-2021 Jorrit "Chainfire" Jongma
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
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import eu.chainfire.holeylight.misc.Settings;
import eu.chainfire.holeylight.misc.Slog;

@SuppressWarnings({ "deprecation", "FieldCanBeLocal", "unused", "UnusedReturnValue" })
public class SpritePlayer extends RelativeLayout {
    public static volatile Mode test_lastDrawMode = null;

    public enum Mode { SWIRL, BLINK, SINGLE, TSP, TSP_HIDE }

    private final int TSP_FAST_DRAW_TIME = 10000;
    private final int TSP_FIRST_DRAW_DELAY = 2000;

    public interface OnSpriteSheetNeededListener {
        SpriteSheet onSpriteSheetNeeded(int width, int height, Mode mode);
    }

    public interface OnAnimationListener {
        boolean onAnimationFrameStart(boolean draw);
        void onAnimationFrameEnd(boolean draw);
        boolean onAnimationComplete();
    }

    private final Object sync = new Object();

    private final HandlerThread handlerThreadRender;
    private final HandlerThread handlerThreadLoader;
    private final Handler handlerRender;
    private final Handler handlerLoader;
    private final Handler handlerMain;
    private volatile Choreographer choreographer;

    private final SurfaceView surfaceView;

    private volatile OnSpriteSheetNeededListener onSpriteSheetNeededListener = null;
    private volatile OnAnimationListener onAnimationListener = null;

    private final Paint paint = new Paint();
    private final Paint paintTsp = new Paint();
    private final Paint paintTspTransparent = new Paint();
    private final Paint paintTspTransparentDebug = new Paint();
    private final float dpToPx;

    private volatile int frame = -1;
    private volatile SpriteSheet spriteSheetSwirlPrevious = null;
    private volatile SpriteSheet spriteSheetBlinkPrevious = null;
    private volatile SpriteSheet spriteSheetSinglePrevious = null;
    private volatile SpriteSheet spriteSheetSwirl = null;
    private volatile SpriteSheet spriteSheetBlink = null;
    private volatile SpriteSheet spriteSheetSingle = null;
    private volatile int spriteSheetLoading = 0;
    private volatile long spriteSheetLoadingId = 0;
    private final Point lastSpriteSheetRequest = new Point(0, 0);
    private volatile boolean forceSpriteSheetReload = false;
    private final Rect dest = new Rect();
    private final Rect destDouble = new Rect();
    private volatile boolean surfaceInvalidated = true;
    private volatile boolean draw = false;
    private volatile boolean wanted = false;
    private volatile int width = -1;
    private volatile int height = -1;
    private volatile int[] colors = null;
    private volatile Drawable[] icons = null;
    private volatile float speed = 1.0f;
    private volatile Mode drawMode = Mode.SWIRL;
    private volatile boolean drawBackground = false;
    private volatile long modeStart = 0L;
    private volatile boolean surfaceReady = false;
    private volatile boolean tspBlank = false;
    private volatile boolean blackFill = false;
    private volatile Rect tspTransparentArea = null;
    private volatile boolean tspTransparentDrawn = false;

    public SpritePlayer(Context context) {
        super(context);

        // not adjusted for density but shouldn't be important here
        dpToPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getContext().getResources().getDisplayMetrics());

        handlerThreadRender = new HandlerThread("SpritePlayer#Render");
        handlerThreadRender.start();
        handlerRender = new Handler(handlerThreadRender.getLooper());
        handlerThreadLoader = new HandlerThread("SpritePlayer#Loader");
        handlerThreadLoader.start();
        handlerLoader = new Handler(handlerThreadLoader.getLooper());
        handlerMain = new Handler(Looper.getMainLooper());

        paint.setAntiAlias(false);
        paint.setDither(false);
        paint.setFilterBitmap(false);

        paintTsp.setAntiAlias(true);
        paintTsp.setDither(false);
        paintTsp.setFilterBitmap(false);

        paintTspTransparent.setColor(Color.TRANSPARENT);
        paintTspTransparent.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        paintTspTransparentDebug.setColor(0x4000FF00);

        handlerRender.post(() -> {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            choreographer = Choreographer.getInstance();
        });
        
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        surfaceView = new SurfaceView(context);
        surfaceView.setZOrderOnTop(true);
        surfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        surfaceView.getHolder().addCallback(surfaceCallback);
        surfaceView.setVisibility(View.VISIBLE);
        surfaceView.setLayoutParams(new RelativeLayout.LayoutParams(params));
        addView(surfaceView);

        while (choreographer == null) {
            try {
                //noinspection BusyWait
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // no action
            }
        }

        test_lastDrawMode = drawMode;
    }

    @Override
    protected void finalize() throws Throwable {
        handlerThreadRender.quitSafely();
        handlerThreadLoader.quitSafely();
        super.finalize();
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        evaluate();
    }

    private final SurfaceHolder.Callback2 surfaceCallback = new SurfaceHolder.Callback2() {
        @Override
        public void surfaceRedrawNeeded(SurfaceHolder holder) {
            surfaceInvalidated = true;
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            surfaceInvalidated = true;
            surfaceReady = true;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            synchronized (sync) {
                SpritePlayer.this.width = width;
                SpritePlayer.this.height = height;
                callOnSpriteSheetNeeded(width, height);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            surfaceReady = false;
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

    @Override
    protected void onDraw(Canvas canvas) {
        if (isTSPMode() && !tspBlank && tspTransparentArea != null && SystemClock.elapsedRealtime() - modeStart > TSP_FIRST_DRAW_DELAY) {
            Slog.d("Clock", "performDraw");
            canvas.drawRect(tspTransparentArea, Settings.DEBUG_OVERLAY ? paintTspTransparentDebug : paintTspTransparent);
            tspTransparentDrawn = true;
        }
    }

    private final Runnable scheduleBackgroundDraw = new Runnable() {
        @Override
        public void run() {
            handlerMain.removeCallbacks(scheduleBackgroundDraw);
            if (isTSPMode() && tspTransparentArea != null) {
                if (SystemClock.elapsedRealtime() - modeStart > TSP_FIRST_DRAW_DELAY) {
                    invalidate();
                } else {
                    handlerMain.postDelayed(scheduleBackgroundDraw, Math.max(50, TSP_FIRST_DRAW_DELAY - (SystemClock.elapsedRealtime() - modeStart)));
                }
            }
        }
    };

    @SuppressWarnings("all")
    private void renderFrame(Canvas canvas, SpriteSheet spriteSheet, int frame, float startAngle, float radiusDecrease) {
        if (drawBackground) {
            canvas.drawColor(Color.BLACK, PorterDuff.Mode.SRC);
        } else if (!canvas.isHardwareAccelerated()) {
            // on hardware accelerated canvas the content is already cleared
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
        if (Settings.DEBUG_OVERLAY) canvas.drawColor(0x80808080);
        if (isTSPMode()) {
            // TSP_HIDE is a no_op, background drawn already, we only handle it at all because
            // rendering here causes the rest of the screen to be updated as well
            //
            // We delay a short time to prevent the circle jumping around on first show, due to
            // AOD start TSP rect updates
            if (!tspBlank && (drawMode == Mode.TSP) && (SystemClock.elapsedRealtime() - modeStart > TSP_FIRST_DRAW_DELAY)) {
                paintTsp.setColorFilter(null);
                paintTsp.setXfermode(null);

                float left = radiusDecrease;
                float top = radiusDecrease;
                float width = this.width - (radiusDecrease * 2f);
                float height = this.height - (radiusDecrease * 2f);
                float right = left + width;
                float bottom = top + height;
                float cx = left + (width / 2f);
                float cy = top + (height / 2f);
                float radius = (width / 2f) - (8f * dpToPx);

                float anglePerColor = 360f / colors.length;

                int[] sweepColors;
                if (colors.length == 1) {
                    sweepColors = new int[] { colors[0], colors[0] };
                } else {
                    sweepColors = new int[colors.length*7 + 1];
                    for (int i = 0; i < 4; i++) sweepColors[i] = colors[0];
                    for (int i = 1; i < colors.length; i++) {
                        for (int j = 4 + (i - 1)*7; j < 4 + i*7; j++) {
                            sweepColors[j] = colors[i];
                        }
                    }
                    for (int i = sweepColors.length - 4; i < sweepColors.length; i++) sweepColors[i] = colors[0];
                }

                Shader sweep = new SweepGradient(cx, cy, sweepColors, null);
                Matrix matrix = new Matrix();
                matrix.preRotate(startAngle + 270, cx, cy);
                sweep.setLocalMatrix(matrix);
                paintTsp.setShader(sweep);
                canvas.drawArc(left, top, right, bottom, 0f, 360f, true, paintTsp);
                paintTsp.setShader(null);

                if (drawBackground) {
                    paintTsp.setColor(Color.BLACK);
                } else {
                    paintTsp.setColor(Color.TRANSPARENT);
                    paintTsp.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                }
                canvas.drawCircle(cx, cy, radius, paintTsp);

                if (icons.length == colors.length) {
                    int drawableIcons = 0;
                    for (int i = 0; i < icons.length; i++) {
                        if (icons[i] != null) {
                            drawableIcons++;
                        }
                    }

                    for (int i = 0; i < icons.length; i++) {
                        if (icons[i] != null) {
                            float x = (right + left)/2f;
                            float y = (bottom + top)/2f;
                            if (drawableIcons > 1) {
                                x += Math.cos(Math.toRadians(startAngle + 270 + (anglePerColor * i))) * radius/2;
                                y += Math.sin(Math.toRadians(startAngle + 270 + (anglePerColor * i))) * radius/2;
                            }

                            float w = 24 * dpToPx;
                            float h = 24 * dpToPx;
                            if (drawableIcons == 1) {
                                w *= 2f;
                                h *= 2f;
                            }
                            float l = x - w/2f;
                            float t = y - h/2f;
                            icons[i].setBounds(Math.round(l), Math.round(t), Math.round(l + w), Math.round(h + t));
                            icons[i].setColorFilter(null);
                            icons[i].setTintList(null);
                            icons[i].setTintBlendMode(BlendMode.SRC_IN);
                            icons[i].setTintMode(PorterDuff.Mode.SRC_IN);
                            icons[i].setTint(colors[i]);
                            icons[i].draw(canvas);
                        }
                    }
                }
            }
        } else if (spriteSheet != null) {
            paint.setXfermode(null);
            paint.setColor(Color.WHITE);
            SpriteSheet.Sprite sprite = spriteSheet.getFrame(frame);
            Bitmap bitmap = sprite.getBitmap();
            Rect area = sprite.getArea();
            Bitmap black = spriteSheet.getBlackFrame();
            if ((colors != null) && (colors.length == 1)) {
                if (blackFill && !Settings.tuning) {
                    if (!black.isRecycled()) {
                        paint.setColorFilter(null);
                        canvas.drawBitmap(black, new Rect(0, 0, black.getWidth(), black.getHeight()), dest, paint);
                    }
                }

                // fast single-color mode
                paint.setColorFilter(new PorterDuffColorFilter(colors[0], PorterDuff.Mode.SRC_ATOP));
                if (!bitmap.isRecycled()) {
                    canvas.drawBitmap(bitmap, area, dest, paint);
                }
            } else {
                // slower multi-colored mode
                paint.setColorFilter(null);
                if (!bitmap.isRecycled()) {
                    canvas.drawBitmap(bitmap, area, dest, paint);
                }

                paint.setXfermode(new PorterDuffXfermode(drawBackground ? PorterDuff.Mode.MULTIPLY : PorterDuff.Mode.SRC_ATOP));

                float anglePerColor = 360f / colors.length;
                for (int i = 0; i < colors.length; i++) {
                    // we use double size here because the arc may cut off the larger S10+ animation otherwise
                    paint.setColor(colors[i]);
                    canvas.drawArc(destDouble.left, destDouble.top, destDouble.right, destDouble.bottom, startAngle + 270 + (anglePerColor * i), anglePerColor, true, paint);
                }

                if (blackFill && !Settings.tuning) {
                    if (!black.isRecycled()) {
                        paint.setColorFilter(null);
                        paint.setXfermode(null);
                        canvas.drawBitmap(black, new Rect(0, 0, black.getWidth(), black.getHeight()), dest, paint);
                    }
                }
            }
        }
    }

    private final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        private long startTimeNanos = 0;
        private int lastFrameDrawn = -1;
        private int[] lastColors = null;

        @Override
        public void doFrame(long frameTimeNanos) {
            synchronized (sync) {
                // Software canvas 2x quicker than hardware during tests
                SpriteSheet spriteSheet = getSpriteSheet();
                if (draw && surfaceReady) {
                    // AOD changes position every 10 minutes to prevent burn-in, so that's
                    // as good a base as any to base our own burn-in protection on.
                    // We rotate our colors throughout the circle, and change the radius
                    final int cycle_ms =  10 * 60 * 1000;
                    long diff = SystemClock.elapsedRealtime() - modeStart;
                    float cycle_part = (diff % cycle_ms) / (float)cycle_ms;

                    if (isTSPMode()) {
                        boolean draw;
                        if ((startTimeNanos == 0) || (diff <= TSP_FAST_DRAW_TIME)) {
                            draw = true;
                        } else {
                            draw = (frameTimeNanos - startTimeNanos >= 8000000000L);
                        }
                        draw |= surfaceInvalidated;
                        surfaceInvalidated = false;

                        if (onAnimationListener != null) {
                            onAnimationListener.onAnimationFrameStart(draw); // intentionally ignore result
                        }
                        if (draw) {
                            Canvas canvas = surfaceView.getHolder().lockCanvas();
                            if (canvas != null) {
                                try {
                                    renderFrame(canvas, null, 0, cycle_part * 360f, cycle_part * 16f * dpToPx);
                                } finally {
                                    try {
                                        surfaceView.getHolder().unlockCanvasAndPost(canvas);
                                    } catch (IllegalStateException e) {
                                        // no action
                                    }
                                }
                            }
                            startTimeNanos = frameTimeNanos;
                        }
                        if (onAnimationListener != null) {
                            onAnimationListener.onAnimationFrameEnd(draw);
                        }
                    } else if (spriteSheet == null) { // still loading
                        if (onAnimationListener != null) {
                            onAnimationListener.onAnimationFrameStart(true); // intentionally ignore result
                        }

                        Canvas canvas = surfaceView.getHolder().lockCanvas();
                        if (canvas != null) {
                            try {
                                renderFrame(canvas, null, 0, 0f, 0f);
                            } finally {
                                try {
                                    surfaceView.getHolder().unlockCanvasAndPost(canvas);
                                } catch (IllegalStateException e) {
                                    // no action
                                }
                            }
                        }

                        if (onAnimationListener != null) {
                            onAnimationListener.onAnimationFrameEnd(true);
                        }
                    } else { // ready
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

                            Canvas canvas = surfaceView.getHolder().lockCanvas();
                            if (canvas != null) {
                                try {
                                    renderFrame(canvas, spriteSheet, drawFrame, cycle_part * 360f, 0f);
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
                }
                if (draw) callNextFrame(!surfaceReady);
            }
        }
    };

    private final Runnable tspFrame = () -> frameCallback.doFrame(System.nanoTime());

    private void cancelNextFrame() {
        handlerRender.removeCallbacks(tspFrame);
        choreographer.removeFrameCallback(frameCallback);
    }

    private void callNextFrame(boolean immediately) {
        cancelNextFrame();
        if (immediately) surfaceInvalidated = true;
        if (isTSPMode() && ((Math.abs(SystemClock.elapsedRealtime() - modeStart) > TSP_FAST_DRAW_TIME) || immediately)) {
            handlerRender.postDelayed(tspFrame, immediately ? 0 : 250);
        } else {
            choreographer.postFrameCallback(frameCallback);
        }
    }

    private void callOnSpriteSheetNeeded(int width, int height) {
        if (width == -1 || height == -1) return;
        synchronized (sync) {
            if (isTSPMode()) {
                surfaceInvalidated = true;
                evaluate();
                return;
            }
            if (onSpriteSheetNeededListener == null) return;
            if (
                !forceSpriteSheetReload &&
                (spriteSheetSwirl != null) && (spriteSheetSwirl.getWidth() == width) && (spriteSheetSwirl.getHeight() == height) &&
                (spriteSheetBlink != null) && (spriteSheetBlink.getWidth() == width) && (spriteSheetBlink.getHeight() == height) &&
                (spriteSheetSingle != null) && (spriteSheetSingle.getWidth() == width) && (spriteSheetSingle.getHeight() == height)
            ) return;
            if (!forceSpriteSheetReload && (lastSpriteSheetRequest.x == width) && (lastSpriteSheetRequest.y == height)) return;
            lastSpriteSheetRequest.set(width, height);
            dest.set(0, 0, width, height);
            destDouble.set(dest.centerX() - width, dest.centerY() - height, dest.centerX() + width, dest.centerY() + height);
            if (
                !forceSpriteSheetReload &&
                (spriteSheetSwirlPrevious != null) && (spriteSheetSwirlPrevious.getWidth() == width) && (spriteSheetSwirlPrevious.getHeight() == height) &&
                (spriteSheetBlinkPrevious != null) && (spriteSheetBlinkPrevious.getWidth() == width) && (spriteSheetBlinkPrevious.getHeight() == height) &&
                (spriteSheetSinglePrevious != null) && (spriteSheetSinglePrevious.getWidth() == width) && (spriteSheetSinglePrevious.getHeight() == height)
            ) {
                swapSpriteSheet(null);
                evaluate();
                return;
            } else if (spriteSheetLoading == 0) {
                resetSpriteSheet(null);
            }
            spriteSheetLoading++;
            spriteSheetLoadingId++;
            final long callbackId = spriteSheetLoadingId;
            final boolean handleForceReload = forceSpriteSheetReload;
            handlerLoader.post(() -> {
                OnSpriteSheetNeededListener listener;
                synchronized (sync) {
                    listener = onSpriteSheetNeededListener;
                }
                if ((listener != null) && (callbackId == spriteSheetLoadingId)) {
                    if (handleForceReload) {
                        if (spriteSheetSwirlPrevious != null) {
                            spriteSheetSwirlPrevious.recycle();
                            spriteSheetSwirlPrevious = null;
                        }
                        if (spriteSheetBlinkPrevious != null) {
                            spriteSheetBlinkPrevious.recycle();
                            spriteSheetBlinkPrevious = null;
                        }
                        if (spriteSheetSinglePrevious != null) {
                            spriteSheetSinglePrevious.recycle();
                            spriteSheetSinglePrevious = null;
                        }
                        forceSpriteSheetReload = false;
                    }

                    SpriteSheet spriteSheetSwirl = listener.onSpriteSheetNeeded(width, height, Mode.SWIRL);
                    SpriteSheet spriteSheetBlink = listener.onSpriteSheetNeeded(width, height, Mode.BLINK);
                    SpriteSheet spriteSheetSingle = listener.onSpriteSheetNeeded(width, height, Mode.SINGLE);
                    synchronized (sync) {
                        setSpriteSheet(spriteSheetSwirl, Mode.SWIRL);
                        setSpriteSheet(spriteSheetBlink, Mode.BLINK);
                        setSpriteSheet(spriteSheetSingle, Mode.SINGLE);
                        spriteSheetLoading--;
                        surfaceInvalidated = true;
                        evaluate();
                    }
                } else {
                    synchronized (sync) {
                        spriteSheetLoading--;
                    }
                }
            });
        }
    }

    public void setOnSpriteSheetNeededListener(OnSpriteSheetNeededListener onSpriteSheetNeededListener) {
        synchronized (sync) {
            if (this.onSpriteSheetNeededListener == onSpriteSheetNeededListener) return;

            this.onSpriteSheetNeededListener = onSpriteSheetNeededListener;
            callOnSpriteSheetNeeded(width, height);
        }
    }

    public void setOnAnimationListener(OnAnimationListener onAnimationListener) {
        this.onAnimationListener = onAnimationListener;
    }

    private void resetSpriteSheet(Mode mode) {
        synchronized (sync) {
            if ((mode == null) || (drawMode == mode)) {
                frame = -1;
            }
            if (((mode == null) || (mode == Mode.SWIRL)) && spriteSheetSwirl != null) {
                SpriteSheet recycle = spriteSheetSwirlPrevious;
                spriteSheetSwirlPrevious = spriteSheetSwirl;
                spriteSheetSwirl = null;
                if (recycle != null) recycle.recycle();
            }
            if (((mode == null) || (mode == Mode.BLINK)) && spriteSheetBlink != null) {
                SpriteSheet recycle = spriteSheetBlinkPrevious;
                spriteSheetBlinkPrevious = spriteSheetBlink;
                spriteSheetBlink = null;
                if (recycle != null) recycle.recycle();
            }
            if (((mode == null) || (mode == Mode.SINGLE)) && spriteSheetSingle != null) {
                SpriteSheet recycle = spriteSheetSinglePrevious;
                spriteSheetSinglePrevious = spriteSheetSingle;
                spriteSheetSingle = null;
                if (recycle != null) recycle.recycle();
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
    }

    private void swapSpriteSheet(Mode mode) {
        synchronized (sync) {
            if ((mode == null) || (drawMode == mode)) {
                frame = -1;
            }
            SpriteSheet tmp;
            tmp = spriteSheetSwirl;
            spriteSheetSwirl = spriteSheetSwirlPrevious;
            spriteSheetSwirlPrevious = tmp;
            tmp = spriteSheetBlink;
            spriteSheetBlink = spriteSheetBlinkPrevious;
            spriteSheetBlinkPrevious = tmp;
            tmp = spriteSheetSingle;
            spriteSheetSingle = spriteSheetSinglePrevious;
            spriteSheetSinglePrevious = tmp;
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
    }

    public void setSpriteSheet(SpriteSheet spriteSheet, Mode mode) {
        synchronized (sync) {
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
    }

    public void setColors(int[] colors) {
        synchronized (sync) {
            this.colors = colors;
            callNextFrame(true);
        }
    }

    public void setIcons(Drawable[] icons) {
        synchronized (sync) {
            this.icons = icons;
        }
    }

    private void startUpdating() {
        synchronized (sync) {
            if (!draw) {
                modeStart = SystemClock.elapsedRealtime();
                handlerMain.post(scheduleBackgroundDraw);
            }
            draw = true;
            callNextFrame(true);
        }
    }

    private void stopUpdating() {
        synchronized (sync) {
            draw = false;
            cancelNextFrame();
        }
    }

    private void evaluate() {
        synchronized (sync) {
            if (wanted && ((getSpriteSheet() != null) || (spriteSheetLoading > 0) || isTSPMode()) && (getWindowVisibility() == View.VISIBLE) && (getVisibility() == View.VISIBLE)) {
                startUpdating();
            } else {
                stopUpdating();
            }
        }
    }

    public void playAnimation() {
        synchronized (sync) {
            wanted = true;
            frame = -1;
            evaluate();
        }
    }

    public void cancelAnimation() {
        synchronized (sync) {
            wanted = false;
            evaluate();
        }
    }

    public boolean isAnimating() {
        return draw;
    }

    public void setSpeed(float speed) {
        synchronized (sync) {
            frame = -1;
            this.speed = speed;
        }
    }

    public Mode getMode() {
        return drawMode;
    }

    public void setMode(Mode mode, boolean blackFill) {
        synchronized (sync) {
            boolean redraw = false;
            if (mode != drawMode) {
                frame = -1;
                if (mode == Mode.TSP && drawMode == Mode.TSP_HIDE) {
                    modeStart = SystemClock.elapsedRealtime() - (TSP_FIRST_DRAW_DELAY / 4) * 3;
                } else {
                    modeStart = SystemClock.elapsedRealtime();
                }
                drawMode = mode;
                redraw = true;
                handlerMain.post(scheduleBackgroundDraw);
            }
            test_lastDrawMode = drawMode;
            if (blackFill != this.blackFill) {
                this.blackFill = blackFill;
                redraw = true;
            }
            if (redraw) {
                surfaceInvalidated = true;
                evaluate();
            }
        }
    }

    private SpriteSheet getSpriteSheet() {
        synchronized (sync) {
            switch (drawMode) {
                case SWIRL: return spriteSheetSwirl;
                case BLINK: return spriteSheetBlink;
                case SINGLE: return spriteSheetSingle;
            }
            return null;
        }
    }

    public void updateTransparentArea(Rect rect) {
        synchronized (sync) {
            tspTransparentArea = rect == null ? null : new Rect(rect);
        }
    }

    public void updateDisplayArea(Rect rect) {
        updateDisplayArea(rect.left, rect.top, rect.width(), rect.height());
    }

    public void updateDisplayArea(int x, int y, int width, int height) {
        synchronized (sync) {
            RelativeLayout.LayoutParams params;

            params = (RelativeLayout.LayoutParams)surfaceView.getLayoutParams();
            params.leftMargin = x;
            params.topMargin = y;
            params.width = width;
            params.height = height;
            surfaceView.setLayoutParams(params);

            this.width = width;
            this.height = height;

            callOnSpriteSheetNeeded(width, height);
        }
    }

    public void invalidateDisplayArea() {
        synchronized (sync) {
            if (draw) {
                surfaceInvalidated = true;
                callNextFrame(true);
            }
        }
    }

    public void setDrawBackground(boolean drawBackground) {
        synchronized (sync) {
            if (this.drawBackground != drawBackground) {
                this.drawBackground = drawBackground;
                surfaceInvalidated = true;
            }
        }
    }

    public Object getSynchronizer() {
        return sync;
    }

    public boolean isTSPMode() {
        return isTSPMode(drawMode);
    }

    public boolean isTSPMode(Mode mode) {
        return (mode == Mode.TSP) || (mode == Mode.TSP_HIDE);
    }

    public boolean isMultiColorMode() {
        return isMultiColorMode(drawMode);
    }

    public boolean isMultiColorMode(Mode mode) {
        return (mode == Mode.SINGLE) || isTSPMode(mode);
    }

    public boolean isTSPBlank() {
        return tspBlank;
    }

    public void setTSPBlank(boolean value) {
        Slog.d("SpritePlayer", "tspBlank --> %s", value ? "TRUE" : "FALSE");
        tspBlank = value;
    }

    public void forceReload() {
        forceSpriteSheetReload = true;
    }
}
