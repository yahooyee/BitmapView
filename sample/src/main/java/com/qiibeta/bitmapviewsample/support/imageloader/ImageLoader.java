package com.qiibeta.bitmapviewsample.support.imageloader;


import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import com.qiibeta.bitmapviewsample.support.mrc.BitmapMRC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageLoader {
    public static interface Callback {
        public void onGetImageSize(int imageWidth, int imageHeight);

        public void onSuccess(Uri uri, BitmapMRC bitmap, boolean timeConsumingUnder15ms);

        public void onFailure(Uri uri, Exception exception);
    }

    private static ImageLoader sInstance = new ImageLoader();
    private Executor mExecutor = Executors.newFixedThreadPool(4);
    private HashMap<String, LocalRunnable> mTaskMap = new HashMap<>();

    public static ImageLoader getInstance() {
        return sInstance;
    }

    private ImageLoader() {

    }

    public static String getKey(Uri uri, ImageLoaderOption option) {
        return getKey(uri, option.getWidth(), option.getHeight(), option.getScaleType());
    }

    private static String getKey(Uri uri, int width, int height, int scaleType) {
        return uri.toString() + "_width=" + width + "_height=" + height + "_scaleType=" + scaleType;
    }

    public void loadImage(Uri uri, int width, int height, int scaleType, Callback callback) {
        loadImage(uri, new ImageLoaderOption(width, height, scaleType, callback));
    }

    private void loadImage(final Uri uri, ImageLoaderOption option) {
        final String key = getKey(uri, option);

        LocalRunnable runnable = this.mTaskMap.get(key);
        final long taskStartTimestamp = System.currentTimeMillis();
        if (runnable == null) {
            runnable = new LocalRunnable(uri, new LocalRunnable.Callback() {
                @Override
                public void onResult(LocalRunnable runnable, Bitmap bitmap) {
                    ArrayList<ImageLoaderOption> options = runnable.getOptions();
                    if (bitmap != null) {
                        BitmapMRC bitmapMRC = new BitmapMRC(bitmap);

                        for (ImageLoaderOption option : options) {
                            if (option.getCallback() != null)
                                option.getCallback().onSuccess(uri, bitmapMRC, false);
                        }
                    } else {
                        for (ImageLoaderOption option : options) {
                            if (option.getCallback() != null)
                                option.getCallback().onFailure(uri, null);
                        }
                    }
                    mTaskMap.remove(key);
                }
            });
            runnable.addOption(option);
            this.mTaskMap.put(key, runnable);
            this.mExecutor.execute(runnable);
        } else {
            runnable.addOption(option);
        }
    }

    public void cancelLoadImage(Uri uri, int width, int height, int scaleType) {
        String key = getKey(uri, width, height, scaleType);
        LocalRunnable runnable = this.mTaskMap.get(key);
        if (runnable != null) {
            runnable.cancel();
            this.mTaskMap.remove(key);
            Log.d("ImageLoader", "取消任务");
        }
    }
}
