package com.qiibeta.bitmapviewsample.support.mrc;


import android.graphics.Bitmap;

public class BitmapMRC extends MRC {
    private Bitmap mBitmap;

    public BitmapMRC(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    @Override
    protected void destroy() {
        super.destroy();
        this.mBitmap.recycle();
    }

    public int getWidth() {
        return this.mBitmap.getWidth();
    }

    public int getHeight() {
        return this.mBitmap.getHeight();
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }
}
