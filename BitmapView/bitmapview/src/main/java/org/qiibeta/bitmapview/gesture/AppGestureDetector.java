package org.qiibeta.bitmapview.gesture;

import android.content.Context;
import android.os.Build;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class AppGestureDetector {
    public static class Callback implements ScaleGestureDetector.OnScaleGestureListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, RotateGestureDetector.OnGestureListener {
        private boolean mIsMultiPointers;

        public void setIsMultiPointers(boolean value) {
            this.mIsMultiPointers = value;
        }

        public boolean isIsMultiPointers() {
            return this.mIsMultiPointers;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        public void onLongPress(MotionEvent e) {
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return false;
        }

        public void onShowPress(MotionEvent e) {
        }

        public boolean onDown(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTap(MotionEvent e) {
            return false;
        }

        public boolean onDoubleTapEvent(MotionEvent e) {
            return false;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            return false;
        }

        public boolean onScale(ScaleGestureDetector detector) {
            return false;
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            // Intentionally empty

        }

        @Override
        public void onRotateBegin(float pivotX, float pivotY, float rotateBy) {
        }

        @Override
        public void onRotateEnd(float pivotX, float pivotY) {
        }

        public void onGestureEnd() {
        }

        public void onGestureCancel() {

        }
    }

    private Callback mCallback;
    private Context mContext;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetector mGestureDetector;
    private RotateGestureDetector mRotateGestureDetector;

    public AppGestureDetector(Context context, Callback callback) {
        this.mContext = context;
        this.mCallback = callback;
        this.mScaleGestureDetector = new ScaleGestureDetector(this.mContext, this.mCallback);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //quickScale is conflict with switch base matrix
            this.mScaleGestureDetector.setQuickScaleEnabled(false);
        }
        this.mGestureDetector = new GestureDetector(this.mContext, callback);
        this.mRotateGestureDetector = new RotateGestureDetector(callback);
    }

    public boolean onTouch(MotionEvent motionEvent) {
        this.mScaleGestureDetector.onTouchEvent(motionEvent);
        this.mGestureDetector.onTouchEvent(motionEvent);
        this.mRotateGestureDetector.onTouchEvent(motionEvent);
        switch (motionEvent.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                this.mCallback.setIsMultiPointers(false);
                return this.mCallback.onDown(motionEvent);
            case MotionEvent.ACTION_POINTER_DOWN:
                this.mCallback.setIsMultiPointers(true);
                break;
            case MotionEvent.ACTION_UP:
                this.mCallback.onGestureEnd();
                break;
            case MotionEvent.ACTION_CANCEL:
                this.mCallback.onGestureCancel();
                break;
        }
        return true;
    }
}