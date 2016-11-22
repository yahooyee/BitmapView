package org.qiibeta.bitmapview.utility;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Matrix;
import android.graphics.RectF;

public class MatrixUtility {
    public static final int FULL_CENTER = 0;
    public static final int FIT_CENTER = 1;
    public static final int CROP_CENTER = 2;

    public static Matrix calcFitCenterMatrixVertical(int widgetWidth, int widgetHeight, int drawableWidth, int drawableHeight) {
        RectF drawableRect = new RectF(0, 0, drawableWidth, drawableHeight);
        Matrix fitCenterVerticalMatrix = new Matrix();
        fitCenterVerticalMatrix.postRotate(90, drawableRect.centerX(), drawableRect.centerY());

        float scale = Math.min((float) widgetWidth / (float) drawableHeight, (float) widgetHeight / (float) drawableWidth);
        fitCenterVerticalMatrix.postScale(scale, scale, drawableRect.centerX(), drawableRect.centerY());
        fitCenterVerticalMatrix.postTranslate(widgetWidth / 2 - drawableRect.centerX(), widgetHeight / 2 - drawableRect.centerY());
        return fitCenterVerticalMatrix;
    }

    public static Matrix calcCropCenterMatrixVertical(int widgetWidth, int widgetHeight, int drawableWidth, int drawableHeight) {
        RectF drawableRect = new RectF(0, 0, drawableWidth, drawableHeight);
        Matrix fitCenterVerticalMatrix = new Matrix();
        fitCenterVerticalMatrix.postRotate(90, drawableRect.centerX(), drawableRect.centerY());

        float scale = Math.max((float) widgetWidth / (float) drawableHeight, (float) widgetHeight / (float) drawableWidth);
        fitCenterVerticalMatrix.postScale(scale, scale, drawableRect.centerX(), drawableRect.centerY());
        fitCenterVerticalMatrix.postTranslate(widgetWidth / 2 - drawableRect.centerX(), widgetHeight / 2 - drawableRect.centerY());
        return fitCenterVerticalMatrix;
    }

    public static Matrix[] calcBaseMatrix(int widgetWidth, int widgetHeight, int drawableWidth, int drawableHeight) {
        Matrix[] matrices = new Matrix[3];

        //图片原始尺寸，只要移动就好, no scaling
//		Matrix zeroMatrix = new Matrix(mBaseMatrix);
//		zeroMatrix.postScale(0.7f, 0.7f, widgetWidth / 2, widgetHeight / 2);
//		matrices[0] = zeroMatrix;

        Matrix centerMatrix = new Matrix();
        centerMatrix.postTranslate((widgetWidth - drawableWidth) / 2, (widgetHeight - drawableHeight) / 2);
        matrices[FULL_CENTER] = new UnmodifiedMatrix(centerMatrix);

        //贴合屏幕Fit_Center
        Matrix fitCenterMatrix = new Matrix();
        fitCenterMatrix.setRectToRect(new RectF(0, 0, drawableWidth, drawableHeight), new RectF(0, 0, widgetWidth, widgetHeight), Matrix.ScaleToFit.CENTER);
        matrices[FIT_CENTER] = new UnmodifiedMatrix(fitCenterMatrix);

        //放大填充满Crop_Center
        RectF baseRect = new RectF();
        fitCenterMatrix.mapRect(baseRect, new RectF(0, 0, drawableWidth, drawableHeight));
        float scale = Math.max((float) widgetWidth / (float) baseRect.width(), (float) widgetHeight / (float) baseRect.height());
        Matrix cropCenterMatrix = new Matrix(fitCenterMatrix);
        cropCenterMatrix.postScale(scale, scale, widgetWidth / 2, widgetHeight / 2);
        matrices[CROP_CENTER] = new UnmodifiedMatrix(cropCenterMatrix);

//		matrices[2] = new Matrix();
//		Matrix largeMatrix = matrices[2];
//		largeMatrix.setRectToRect(new RectF(0, 0, drawableWidth, drawableHeight), new RectF(0, 0, widgetWidth, widgetHeight), Matrix.ScaleToFit.END);

//		int mWidth = widgetWidth;
//		int mHeight = widgetHeight;
//
//		float mIntrinsicWidth = drawableWidth;
//		float mIntrinsicHeight = drawableHeight;
//		float mScale = (float) mWidth / (float) mIntrinsicWidth;
//		int paddingHeight = 0;
//		int paddingWidth = 0;
//		// scaling vertical
//		if (mScale * mIntrinsicHeight > mHeight) {
//			mScale = (float) mHeight / (float) mIntrinsicHeight;
//			mBaseMatrix.setScale(mScale, mScale);
//			paddingWidth = (int) ((mWidth - mIntrinsicWidth * mScale) / 2);
//			paddingHeight = 0;
//		} else {
//			// scaling horizontal
//			mBaseMatrix.setScale(mScale, mScale);
//			paddingHeight = (int) ((mHeight - mIntrinsicHeight * mScale) / 2);
//			paddingWidth = 0;
//		}
//		mBaseMatrix.postTranslate(paddingWidth, 0);
//		mBaseMatrix.postTranslate(0, paddingHeight);
        return matrices;
    }

    public static Matrix[] calcVerticalBaseMatrix(int widgetWidth, int widgetHeight, int drawableWidth, int drawableHeight) {
        Matrix[] matrices = new Matrix[3];

        //图片原始尺寸，只要移动就好, no scaling
        Matrix centerMatrix = new Matrix();
        centerMatrix.postTranslate((widgetWidth - drawableWidth) / 2, (widgetHeight - drawableHeight) / 2);
        centerMatrix.postRotate(90, widgetWidth / 2, widgetHeight / 2);
        matrices[FULL_CENTER] = new UnmodifiedMatrix(centerMatrix);

        //贴合屏幕Fit_Center
        Matrix fitCenterMatrix = calcFitCenterMatrixVertical(widgetWidth, widgetHeight, drawableWidth, drawableHeight);
        matrices[FIT_CENTER] = new UnmodifiedMatrix(fitCenterMatrix);

        //放大填充满Crop_Center
        Matrix cropCenterMatrix = calcCropCenterMatrixVertical(widgetWidth, widgetHeight, drawableWidth, drawableHeight);
        matrices[CROP_CENTER] = new UnmodifiedMatrix(cropCenterMatrix);
        return matrices;
    }

    //因为fullCenter在水平垂直都一样的,所以不能用equal
    public static boolean contain(Matrix[] matrixArray, Matrix matrix) {
        for (int i = 0; i < matrixArray.length; i++) {
            if (matrix == matrixArray[i]) {
                return true;
            }
        }
        return false;
    }

    public static int indexOf(Matrix[] matrixArray, Matrix matrix) {
        for (int i = 0; i < matrixArray.length; i++) {
            if (matrix.equals(matrixArray[i])) {
                return i;
            }
        }
        return 0;
    }

    public static float getScale(Matrix matrix) {
        float[] mMatrixValues = new float[9];

        matrix.getValues(mMatrixValues);
        float scaleX = mMatrixValues[Matrix.MSCALE_X];
        float skewY = mMatrixValues[Matrix.MSKEW_Y];
        float rScale = (float) Math.sqrt(scaleX * scaleX + skewY * skewY);
        return rScale;
    }

    public static float getTransRotation(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        float rAngle = (float) (Math.atan2(values[Matrix.MSKEW_Y], values[Matrix.MSCALE_Y]) * (180D / Math.PI));
        return rAngle;
    }

    public static float getTranslateX(Matrix mDrawMatrix) {
        return getValue(mDrawMatrix, Matrix.MTRANS_X);
    }

    protected static float getTranslateY(Matrix mDrawMatrix) {
        return getValue(mDrawMatrix, Matrix.MTRANS_Y);
    }

    protected static float getValue(Matrix matrix, int whichValue) {
        final float[] mMatrixValues = new float[9];

        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    public static boolean equal(Matrix left, Matrix right) {
        float[] leftValues = new float[9];
        left.getValues(leftValues);

        float[] rightValues = new float[9];
        right.getValues(rightValues);

        return equal(leftValues[0], rightValues[0])
                && equal(leftValues[1], rightValues[1])
                && equal(leftValues[2], rightValues[2])
                && equal(leftValues[3], rightValues[3])
                && equal(leftValues[4], rightValues[4])
                && equal(leftValues[5], rightValues[5])
                && equal(leftValues[6], rightValues[6])
                && equal(leftValues[7], rightValues[7])
                && equal(leftValues[8], rightValues[8]);
    }

    public static boolean equal(float left, float right) {
        float value = left - right;
        if (Math.abs(value) <= 0.001) {
            return true;
        } else {
            return false;
        }
    }

    public static int compare(float left, float right) {
        float value = left - right;
        if (Math.abs(value) <= 0.00001) {
            return 0;
        } else if (value > 0) {
            return 1;
        } else {
            return -1;
        }
    }

    public static interface AnimatorCallback {
        public void onAnimate(Matrix drawMatrix);

        //有可能因为各种浮点计算，出现一些偏差，所以最终要直接设置成目标Matrix
        public void onEnd(boolean isCanceled, Matrix dstMatrix);
    }

    private static class MatrixEvaluator implements TypeEvaluator<Matrix> {
        private float[] startEntries = new float[9];
        private float[] endEntries = new float[9];
        private float[] currentEntries = new float[9];
        private Matrix matrix = new Matrix();

        public Matrix evaluate(float fraction,
                               Matrix startValue,
                               Matrix endValue) {
            startValue.getValues(startEntries);
            endValue.getValues(endEntries);

            for (int i = 0; i < 9; i++)
                currentEntries[i] = (1 - fraction) * startEntries[i]
                        + fraction * endEntries[i];

            matrix.setValues(currentEntries);
            return matrix;
        }
    }

    public static Animator getMatrix2MatrixAnimator(final Matrix src, final Matrix dst, final AnimatorCallback callback) {
        final ValueAnimator anim = ValueAnimator.ofObject(new MatrixEvaluator(), new Matrix(src), new Matrix(dst));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Matrix matrix = (Matrix) animation.getAnimatedValue();
                callback.onAnimate(matrix);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            private boolean isCanceled = false;

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
                isCanceled = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                callback.onEnd(isCanceled, dst);
            }
        });
        return anim;
    }
}
