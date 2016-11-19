package org.qiibeta.bitmapview.gesture;

import android.view.MotionEvent;

class RotateGestureDetector {
    interface OnGestureListener {
        void onRotateBegin(float pivotX, float pivotY, float rotateBy);

        void onRotateEnd(float pivotX, float pivotY);
    }

    private static final float ROTATE_SLOP_DEGREE = 10;
    private float mInitDegree = 0f;
    private float mPreviousDegree = 0f;
    private float mRotatePivotX = 0f;
    private float mRotatePivotY = 0f;
    private boolean mCanRotate = false;
    private boolean mIsRotating = false;
    private int mFirstFingerId = 0;
    private int mSecondFingerId = 0;
    private OnGestureListener mOnGestureListener;

    RotateGestureDetector(OnGestureListener onGestureListener) {
        this.mOnGestureListener = onGestureListener;
    }

    boolean onTouchEvent(MotionEvent e) {
        final int action = e.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mCanRotate = false;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                int touchCount = e.getPointerCount();
                boolean onlyTwoFingers = touchCount == 2;
                if (onlyTwoFingers && !mIsRotating) {
                    mFirstFingerId = e.getPointerId(0);
                    mSecondFingerId = e.getPointerId(1);
                    mInitDegree = mPreviousDegree = rotation(e);
                    mCanRotate = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mCanRotate) {
                    float nowDegree = rotation(e);
                    if (!mIsRotating) {
                        mIsRotating = Math.abs(nowDegree - mInitDegree) > ROTATE_SLOP_DEGREE;
                        mPreviousDegree = nowDegree;
                    }
                    if (mIsRotating) {
                        mRotatePivotX = ((e.getX(0) + e.getX(1)) / 2);
                        mRotatePivotY = ((e.getY(0) + e.getY(1)) / 2);
                        mOnGestureListener.onRotateBegin(mRotatePivotX, mRotatePivotY, nowDegree - mPreviousDegree);
                        mPreviousDegree = nowDegree;
                    }
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int index = e.getActionIndex();
                int id = e.getPointerId(index);
                if (id == mSecondFingerId || id == mFirstFingerId) {
                    finishRotate();
                    mCanRotate = false;
                }
                break;
            case MotionEvent.ACTION_UP:
                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return false;
    }

    private void finishRotate() {
        if (mIsRotating) {
            mOnGestureListener.onRotateEnd(mRotatePivotX, mRotatePivotY);
            mInitDegree = 0f;
            mPreviousDegree = 0f;
            mIsRotating = false;
            mRotatePivotX = 0f;
            mRotatePivotY = 0f;
        }
        mInitDegree = 0f;
        mPreviousDegree = 0f;
        mFirstFingerId = 0;
        mSecondFingerId = 0;
    }

    private float rotation(MotionEvent event) {
        int firstIndex = event.findPointerIndex(mFirstFingerId);
        int secondIndex = event.findPointerIndex(mSecondFingerId);
        double deltaX = (event.getX(firstIndex) - event.getX(secondIndex));
        double deltaY = (event.getY(firstIndex) - event.getY(secondIndex));
        double radians = Math.atan2(deltaY, deltaX);
        // Log.d("Rotation ~~~~~~~~~~~~~~~~~", deltaX + " ## " + deltaY + " ## " + radians + " ## "
        // + Math.toDegrees(radians));
        return (float) Math.toDegrees(radians);
    }
}