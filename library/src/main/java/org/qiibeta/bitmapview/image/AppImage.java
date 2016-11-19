package org.qiibeta.bitmapview.image;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;

import java.lang.ref.WeakReference;

public abstract class AppImage {
    public static interface Callback {
        public boolean isDebug();

        public void invalidateAppImage();
    }

    private WeakReference<Callback> mCallback;

    public abstract void update(RectF viewRect, Matrix matrix);

    public abstract void onDraw(Canvas canvas, Matrix matrix);

    public abstract int getWidth();

    public abstract int getHeight();

    public void destroy() {

    }

    public void setView(Callback view) {
        this.mCallback = new WeakReference<Callback>(view);
    }

    protected boolean isDebug() {
        if (this.mCallback == null) {
            return false;
        }
        Callback callback = this.mCallback.get();
        if (callback != null) {
            return callback.isDebug();
        } else {
            return false;
        }
    }

    protected void postInvalidate() {
        Callback callback = this.mCallback.get();
        if (callback != null) {
            callback.invalidateAppImage();
        }
    }
}
