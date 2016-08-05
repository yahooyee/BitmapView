package com.qiibeta.bitmapviewsample;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import org.qiibeta.bitmapview.image.TileImage;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GalleryApplication extends Application {
    public static interface TrimMemoryCallback {
        public void onTrimMemory();
    }

    private static GalleryApplication sInstance;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ArrayList<WeakReference<TrimMemoryCallback>> mTrimMemoryCallbackList = new ArrayList<>();
    private Executor mBackgroundExecutor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static GalleryApplication getInstance() {
        return sInstance;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void addTrimMemoryCallback(TrimMemoryCallback callback) {
        this.mTrimMemoryCallbackList.add(new WeakReference<TrimMemoryCallback>(callback));
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        for (WeakReference<TrimMemoryCallback> reference : mTrimMemoryCallbackList) {
            TrimMemoryCallback callback = reference.get();
            if (callback != null) {
                callback.onTrimMemory();
            }
        }
        TileImage.clearBitmapRecyclePool();
    }

    public void submitBackgroundTask(Runnable runnable) {
        this.mBackgroundExecutor.execute(runnable);
    }
}
