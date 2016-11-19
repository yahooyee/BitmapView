package com.qiibeta.bitmapviewsample.support.imageloader;


public class ImageLoaderOption {
    public static final int SCALE_TYPE_FIT = 0;
    public static final int SCALE_TYPE_CROP = 1;

    private int mWidth;
    private int mHeight;
    private int mScaleType;
    private ImageLoader.Callback mCallback;

    public ImageLoaderOption(int width, int height, int scaleType, ImageLoader.Callback callback) {
        this.mWidth = width;
        this.mHeight = height;
        this.mScaleType = scaleType;
        this.mCallback = callback;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getScaleType(){
        return mScaleType;
    }

    public ImageLoader.Callback getCallback() {
        return mCallback;
    }
}
