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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.airbnb.lottie.LottieComposition;
import com.airbnb.lottie.LottieDrawable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({ "WeakerAccess", "UnusedReturnValue", "FieldCanBeLocal" })
public class SpriteSheet {
    private static Bitmap superimposedFrame(LottieComposition lottieComposition) {
        int frames = (int)lottieComposition.getDurationFrames();

        LottieDrawable lottieDrawable = new LottieDrawable();
        lottieDrawable.setComposition(lottieComposition);

        Bitmap frame = Bitmap.createBitmap(lottieDrawable.getIntrinsicWidth(), lottieDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

        frame.eraseColor(Color.TRANSPARENT);

        for (int i = 0; i < frames; i++) {
            Canvas frame_canvas = new Canvas(frame);
            lottieDrawable.setFrame(i);
            lottieDrawable.draw(frame_canvas);
        }

        return frame;
    }

    private static Bitmap blackFrame(Bitmap superimposedFrame) {
        Bitmap bigFrame = Bitmap.createBitmap(superimposedFrame.getWidth() * 4, superimposedFrame.getHeight() * 4, Bitmap.Config.ARGB_8888);
        Canvas bigCanvas = new Canvas(bigFrame);

        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setAntiAlias(true);
        bigCanvas.drawBitmap(superimposedFrame, new Rect(0, 0, superimposedFrame.getWidth(), superimposedFrame.getHeight()), new Rect(0, 0, bigFrame.getWidth(), bigFrame.getHeight()), paint);

        int[] pixels = new int[bigFrame.getWidth() * bigFrame.getHeight()];
        int[] pixelsBlack = new int[bigFrame.getWidth() * bigFrame.getHeight()];

        bigFrame.getPixels(pixels, 0, bigFrame.getWidth(), 0, 0, bigFrame.getWidth(), bigFrame.getHeight());
        for (int y = 0; y < bigFrame.getHeight(); y++) {
            int last = 0;
            boolean inside = false;
            int start = -1;
            int end = -1;

            int index = y * bigFrame.getWidth();
            for (int x = 0; x < bigFrame.getWidth(); x++) {
                int p = pixels[index];
                if ((p != 0xFFFFFFFF) && (last == 0xFFFFFFFF)) {
                    start = x;
                    inside = true;
                } else if (inside && (p == 0xFFFFFFFF) && (last != 0xFFFFFFFF)) {
                    end = x;
                    break;
                }
                last = p;
                index++;
            }

            if ((start >= 0) && (end >= 0) && (end >= start)) {
                index = y * bigFrame.getWidth();
                for (int x = start; x < end; x++) {
                    pixelsBlack[index + x] = (int)0xFF000000;
                }
            }
        }

        Bitmap bigBlackFrame = Bitmap.createBitmap(bigFrame.getWidth(), bigFrame.getHeight(), Bitmap.Config.ARGB_8888);
        bigBlackFrame.setPixels(pixelsBlack, 0, bigFrame.getWidth(), 0, 0, bigFrame.getWidth(), bigFrame.getHeight());

        Bitmap blackFrame = Bitmap.createBitmap(superimposedFrame.getWidth(), superimposedFrame.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas blackCanvas = new Canvas(blackFrame);
        blackCanvas.drawBitmap(bigBlackFrame, new Rect(0, 0, bigBlackFrame.getWidth(), bigBlackFrame.getHeight()), new Rect(0, 0, blackFrame.getWidth(), blackFrame.getHeight()), paint);
        
        bigFrame.recycle();
        bigBlackFrame.recycle();

        return blackFrame;
    }

    public static SpriteSheet fromLottieComposition(LottieComposition lottieComposition, int width, int height, SpritePlayer.Mode mode) {
        SpriteSheet ss;

        if (mode == SpritePlayer.Mode.SWIRL) {
            int frames = (int)lottieComposition.getDurationFrames();
            int frameRate = (int)lottieComposition.getFrameRate();

            ss = new SpriteSheet(width, height, frames, frameRate);

            LottieDrawable lottieDrawable = new LottieDrawable();
            lottieDrawable.setComposition(lottieComposition);

            Bitmap frame = Bitmap.createBitmap(lottieDrawable.getIntrinsicWidth(), lottieDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            for (int i = 0; i < frames; i++) {
                frame.eraseColor(Color.TRANSPARENT);

                Canvas frame_canvas = new Canvas(frame);
                lottieDrawable.setFrame(i);
                lottieDrawable.draw(frame_canvas);

                ss.addFrame(frame);
            }

            SpriteSheet ss2 = fromLottieComposition(lottieComposition, width, height, SpritePlayer.Mode.SINGLE);
            ss.setBlackFrame(ss2.getBlackFrame());
            ss2.setBlackFrame(null);
        } else if (mode == SpritePlayer.Mode.BLINK || mode == SpritePlayer.Mode.SINGLE) {
            ss = new SpriteSheet(width, height, mode == SpritePlayer.Mode.BLINK ? 2 : 1, 1);

            Bitmap frame = superimposedFrame(lottieComposition);
            ss.addFrame(frame);

            ss.setBlackFrame(blackFrame(frame));

            if (mode == SpritePlayer.Mode.BLINK) {
                frame.eraseColor(Color.TRANSPARENT);
                ss.addFrame(frame);
            }
        } else {
            return null;
        }

        if (ss.isValid()) {
            return ss;
        } else {
            ss.recycle();
            return null;
        }
    }

    private static final int SHEET_DIM = 1024;

    public class Sheet {
        private final Bitmap bitmap;
        private final int cols;
        private final int rows;
        private final int capacity;
        private int used = 0;

        private Sheet() {
            this(null);
        }

        private Sheet(Bitmap bitmap) {
            if (bitmap == null) bitmap = Bitmap.createBitmap(SHEET_DIM, SHEET_DIM, Bitmap.Config.ARGB_8888);
            this.bitmap = bitmap;
            cols = bitmap.getWidth() / width;
            rows = bitmap.getHeight() / height;
            capacity = cols * rows;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

        public Rect nextSprite() {
            if (used >= capacity) {
                return null;
            }
            Rect rect = new Rect();
            rect.left = (used % cols) * width;
            rect.top = (used / cols) * height;
            rect.right = rect.left + width;
            rect.bottom = rect.top + height;
            used++;
            return rect;
        }
    }

    public class Sprite {
        private final Sheet sheet;
        private final Rect area;

        private Sprite(Sheet sheet, Rect area) {
            this.sheet = sheet;
            this.area = area;
        }

        public Rect getArea() {
            return area;
        }

        public Bitmap getBitmap() {
            return sheet.getBitmap();
        }
    }

    private final List<Sheet> sheets = new ArrayList<>();
    private final List<Sprite> sprites = new ArrayList<>();

    private final int width;
    private final int height;
    private final int frames;
    private final int frameRate;

    private Bitmap blackFrame = null;

    private SpriteSheet(int width, int height, int frames, int frameRate) {
        this.width = width;
        this.height = height;
        this.frames = frames;
        this.frameRate = frameRate;
    }

    private Sprite addFrame(Bitmap source) {
        Sheet sheet = null;
        Rect area = null;
        if (sheets.size() > 0) {
            sheet = sheets.get(sheets.size() - 1);
            area = sheet.nextSprite();
        }
        if (area == null) {
            sheet = new Sheet();
            sheets.add(sheet);
            area = sheet.nextSprite();
        }

        Canvas canvas = new Canvas(sheet.getBitmap());
        Paint paint = new Paint();
        paint.setFilterBitmap(true);
        paint.setDither(true);
        paint.setAntiAlias(true);
        canvas.drawBitmap(source, new Rect(0, 0, source.getWidth(), source.getHeight()), area, paint);

        Sprite sprite = new Sprite(sheet, area);
        sprites.add(sprite);
        return sprite;
    }

    private void setBlackFrame(Bitmap source) {
        blackFrame = source;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFrames() {
        return frames;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public Sprite getFrame(int index) {
        if (index < sprites.size()) {
            return sprites.get(index);
        }
        return null;
    }

    public Bitmap getBlackFrame() {
        return blackFrame;
    }

    public boolean isValid() {
        return (sprites.size() == frames) && (blackFrame != null);
    }

    public void recycle() {
        for (Sheet sheet : sheets) {
            Bitmap bitmap = sheet.getBitmap();
            if ((bitmap != null) && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        if (blackFrame != null) {
            blackFrame.recycle();
            blackFrame = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }
}
