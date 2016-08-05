package org.qiibeta.bitmapview;


import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.qiibeta.bitmapview.image.TileImage;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

class TileImageLoader {
    interface Callback {
        void onResult(Uri uri, TileImage image);
    }

    private static TileImageLoader sInstance = new TileImageLoader();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private Executor mExecutor = Executors.newSingleThreadExecutor();

    static TileImageLoader getInstance() {
        return sInstance;
    }

    void load(final BitmapSource source, final Callback callback) {
        this.mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final TileImage image = source.getTileImage();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback.onResult(source.getUri(), image);
                    }
                });
            }
        });
    }
}
