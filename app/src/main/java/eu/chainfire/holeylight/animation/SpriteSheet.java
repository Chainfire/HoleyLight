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

@SuppressWarnings({ "WeakerAccess", "unused", "UnusedReturnValue" })
public class SpriteSheet {
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
        } else if (mode == SpritePlayer.Mode.BLINK){
            int frames = (int)lottieComposition.getDurationFrames();

            ss = new SpriteSheet(width, height, 2, 1);

            LottieDrawable lottieDrawable = new LottieDrawable();
            lottieDrawable.setComposition(lottieComposition);

            Bitmap frame = Bitmap.createBitmap(lottieDrawable.getIntrinsicWidth(), lottieDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            frame.eraseColor(Color.TRANSPARENT);
            for (int i = 0; i < frames; i++) {
                Canvas frame_canvas = new Canvas(frame);
                lottieDrawable.setFrame(i);
                lottieDrawable.draw(frame_canvas);
            }
            ss.addFrame(frame);

            frame.eraseColor(Color.TRANSPARENT);
            ss.addFrame(frame);
        } else if (mode == SpritePlayer.Mode.SINGLE) {
            int frames = (int)lottieComposition.getDurationFrames();

            ss = new SpriteSheet(width, height, 1, 1);

            LottieDrawable lottieDrawable = new LottieDrawable();
            lottieDrawable.setComposition(lottieComposition);

            Bitmap frame = Bitmap.createBitmap(lottieDrawable.getIntrinsicWidth(), lottieDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);

            frame.eraseColor(Color.TRANSPARENT);
            for (int i = 0; i < frames; i++) {
                Canvas frame_canvas = new Canvas(frame);
                lottieDrawable.setFrame(i);
                lottieDrawable.draw(frame_canvas);
            }
            ss.addFrame(frame);
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

    private List<Sheet> sheets = new ArrayList<>();
    private List<Sprite> sprites = new ArrayList<>();

    private final int width;
    private final int height;
    private final int frames;
    private final int frameRate;

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

    public boolean isValid() {
        return (sprites.size() == frames);
    }

    public void recycle() {
        for (Sheet sheet : sheets) {
            Bitmap bitmap = sheet.getBitmap();
            if ((bitmap != null) && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        recycle();
        super.finalize();
    }
}
