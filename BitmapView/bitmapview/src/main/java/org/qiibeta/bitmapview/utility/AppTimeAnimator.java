package org.qiibeta.bitmapview.utility;


import android.os.Build;
import android.util.Log;
import android.view.View;

import java.lang.ref.WeakReference;

public class AppTimeAnimator {
    public static abstract class AnimatorListener {
        public void onAnimationUpdate(AppTimeAnimator animation) {
        }

        public void onAnimationCancel(AppTimeAnimator animation) {
        }

        public void onAnimationEnd(AppTimeAnimator animation) {
        }
    }

    private WeakReference<View> mViewReference;
    private AnimatorListener mListener;
    private Runnable mAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            View view = mViewReference.get();
            if (view != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    view.postOnAnimation(mAnimationRunnable);
                } else {
                    view.postDelayed(mAnimationRunnable, 15);
                }
            }
            if (mListener != null) {
                mListener.onAnimationUpdate(AppTimeAnimator.this);
            }
        }
    };

    public AppTimeAnimator(View view) {
        this.mViewReference = new WeakReference<View>(view);
    }

    public void setListener(AnimatorListener listener) {
        this.mListener = listener;
    }

    public void start() {
        View view = this.mViewReference.get();
        if (view != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.postOnAnimation(mAnimationRunnable);
            } else {
                view.post(mAnimationRunnable);
            }
        }
    }

    public void end() {
        View view = this.mViewReference.get();
        if (view != null) {
            view.removeCallbacks(mAnimationRunnable);
        }
        if (this.mListener != null) {
            this.mListener.onAnimationEnd(this);
        }
    }

    public void cancel() {
        if (this.mListener != null) {
            this.mListener.onAnimationCancel(this);
        }
        end();
    }

    private void log(String log) {
        Log.d("AppTimeAnimator", this.toString() + "_" + log);
    }
}
