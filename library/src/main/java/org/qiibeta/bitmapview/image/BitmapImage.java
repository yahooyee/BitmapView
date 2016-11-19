package org.qiibeta.bitmapview.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

public class BitmapImage extends AppImage {
    public static BitmapImage newInstance(int orientation, Bitmap bitmap) {
        BitmapImage image = new BitmapImage();
        image.mOrientation = orientation;
        image.mBitmap = bitmap;
        return image;
    }

    private int mOrientation;
    private Bitmap mBitmap;
    private Matrix mFixOrientation = new Matrix();

    @Override
    public void update(RectF viewRect, Matrix matrix) {
        //empty
    }

    @Override
    public void onDraw(Canvas canvas, Matrix matrix) {
        if (this.mBitmap != null) {
            mFixOrientation.set(matrix);
            if (mOrientation != 0) {
                if (mOrientation == 90) {
                    mFixOrientation.preTranslate(this.mBitmap.getHeight(), 0);
                } else if (mOrientation == 180) {
                    mFixOrientation.preTranslate(this.mBitmap.getWidth(), this.mBitmap.getHeight());
                } else if (mOrientation == 270) {
                    mFixOrientation.preTranslate(0, this.mBitmap.getWidth());
                }
                mFixOrientation.preRotate(mOrientation);
            }

            canvas.drawBitmap(this.mBitmap, mFixOrientation, null);
        }
    }

    @Override
    public int getWidth() {
        return this.mBitmap != null ? (mOrientation == 0 || mOrientation == 180 ? this.mBitmap.getWidth() : this.mBitmap.getHeight()) : 0;
    }

    @Override
    public int getHeight() {
        return this.mBitmap != null ? (mOrientation == 0 || mOrientation == 180 ? this.mBitmap.getHeight() : this.mBitmap.getWidth()) : 0;
    }

    @Override
    public void destroy() {
        super.destroy();
     }
}
