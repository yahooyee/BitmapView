package org.qiibeta.bitmapview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import org.qiibeta.bitmapview.image.AppImage;
import org.qiibeta.bitmapview.image.TileImage;
import org.qiibeta.bitmapview.utility.MatrixUtility;

class BitmapView extends View implements AppImage.Callback {
    private static final String TAG = "BitmapView";
    private boolean mIsDebug;

    private Uri mUri;

    private Matrix mDrawMatrix = new Matrix();
    private Matrix[] mBaseMatrixArray;//3个BaseMatrix，通过双击切换
    private Matrix[] mBaseVerticalMatrixArray;//3个BaseMatrix，通过双击切换

    private Matrix mBaseMatrix = new Matrix();

    private RectF mViewRect = new RectF();

    private RectF mImageBackgroundRect;
    private Matrix mImageBackgroundMatrix = new Matrix();
    private Paint mBackgroundPaint = new Paint();

    private AppImage mThumbnailImage;
    private AppImage mFullImage;

    private RectF mThumbnailImageRect;
    private RectF mFullImageRect;
    private Matrix mFull2ThumbnailMatrix;
    private Matrix mFullImageDrawMatrix = new Matrix();

    public BitmapView(Context context) {
        super(context);
    }

    public BitmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BitmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDebug(boolean value) {
        this.mIsDebug = value;
    }

    @Override
    public boolean isDebug() {
        return this.mIsDebug;
    }

    @Override
    public void invalidateAppImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }

    public void setBackgroundSize(float imageWidth, float imageHeight) {
        this.mBackgroundPaint.setColor(Color.LTGRAY);
        this.mImageBackgroundRect = new RectF(0, 0, imageWidth, imageHeight);
        invalidateDraw();
    }

    public void setBitmapSource(BitmapSource source) {
        this.mImageBackgroundRect = null;

        if (source != null && source.getUri().equals(mUri)) {
            return;
        }

        this.mUri = null;
        if (this.mThumbnailImage != null) {
            this.mThumbnailImage.destroy();
            this.mThumbnailImage = null;
        }
        if (this.mFullImage != null) {
            this.mFullImage.destroy();
            this.mFullImage = null;
        }

        if (source != null) {
            this.mUri = source.getUri();
            setThumbnailImage(source.getBitmapImage());
            TileImageLoader.getInstance().load(source, new TileImageLoader.Callback() {
                @Override
                public void onResult(Uri uri, TileImage image) {
                    if (uri.equals(BitmapView.this.mUri)) {
                        setFullImage(image);
                    }
                }
            });
        }
        invalidate();
    }

    private void setThumbnailImage(AppImage thumbnailImage) {
        this.mThumbnailImage = thumbnailImage;
        this.mThumbnailImage.setView(this);
        this.mThumbnailImageRect = new RectF(0, 0, mThumbnailImage.getWidth(), mThumbnailImage.getHeight());

        if (getWidth() != 0 && getHeight() != 0) {
            calculateBaseMatrixArray();
            calculateMinAndMaxScale();
        }
    }

    private void setFullImage(AppImage fullImage) {
        if (fullImage == null) {
            return;
        }
        this.mFullImage = fullImage;
        this.mFullImage.setView(this);
        this.mFullImageRect = new RectF(0, 0, mFullImage.getWidth(), mFullImage.getHeight());
        this.mFull2ThumbnailMatrix = new Matrix();
        this.mFull2ThumbnailMatrix.setRectToRect(this.mFullImageRect, this.mThumbnailImageRect, Matrix.ScaleToFit.CENTER);
        if (getWidth() != 0 && getHeight() != 0) {
            fixThumbnailImageFullMatrix();
            calculateMinAndMaxScale();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mThumbnailImage != null) {
            this.mThumbnailImage.onDraw(canvas, mDrawMatrix);
        }
        if (this.mFullImage != null) {
            if (!this.mFull2ThumbnailMatrix.isIdentity()) {
                this.mFullImageDrawMatrix.set(this.mFull2ThumbnailMatrix);
                this.mFullImageDrawMatrix.postConcat(this.mDrawMatrix);
                this.mFullImage.onDraw(canvas, this.mFullImageDrawMatrix);
                log("thumbnail size is small than original size, draw original");
            } else {
                log("thumbnail size and original size is same, skip draw original");
            }
        }

        if (this.mThumbnailImage == null && this.mFullImage == null && this.mImageBackgroundRect != null) {
            mImageBackgroundMatrix.setRectToRect(this.mImageBackgroundRect, mViewRect, Matrix.ScaleToFit.CENTER);
            mImageBackgroundMatrix.mapRect(this.mImageBackgroundRect);
            canvas.drawRect(this.mImageBackgroundRect, this.mBackgroundPaint);
        }
    }

    private void calculateBaseMatrixArray() {
        mBaseMatrixArray = MatrixUtility.calcBaseMatrix(getWidth(), getHeight(), this.mThumbnailImage.getWidth(), this.mThumbnailImage.getHeight());
        mBaseVerticalMatrixArray = MatrixUtility.calcVerticalBaseMatrix(getWidth(), getHeight(), this.mThumbnailImage.getWidth(), this.mThumbnailImage.getHeight());

        mBaseMatrix = mBaseMatrixArray[1];
        mDrawMatrix.reset();
        mDrawMatrix.postConcat(mBaseMatrix);
        invalidateDraw();
    }

    //保证全尺寸的时候能看清大图
    private void fixThumbnailImageFullMatrix() {
        float thumbnail2FullScale = (float) this.mFullImage.getWidth() / (float) this.mThumbnailImage.getWidth();
        this.mBaseMatrixArray[MatrixUtility.FULL_CENTER].postScale(thumbnail2FullScale, thumbnail2FullScale, this.mThumbnailImage.getWidth() / 2, this.mThumbnailImage.getHeight() / 2);
        this.mBaseVerticalMatrixArray[MatrixUtility.FULL_CENTER].postScale(thumbnail2FullScale, thumbnail2FullScale, this.mThumbnailImage.getWidth() / 2, this.mThumbnailImage.getHeight() / 2);
    }

    protected void calculateMinAndMaxScale() {

    }

    protected int getImageWidth() {
        if (mThumbnailImage == null) {
            return 0;
        }
        return mThumbnailImage.getWidth();
    }

    protected int getImageHeight() {
        if (mThumbnailImage == null) {
            return 0;
        }
        return mThumbnailImage.getHeight();
    }

    protected Matrix[] getBaseMatrixArray() {
        return mBaseMatrixArray;
    }

    protected RectF getInitImageRect() {
        return new RectF(0f, 0f, getImageWidth(), getImageHeight());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mViewRect.set(0, 0, w, h);

        if (mThumbnailImage != null) {
            calculateBaseMatrixArray();
            calculateMinAndMaxScale();
        }

        if (mFullImage != null) {
            fixThumbnailImageFullMatrix();
            calculateMinAndMaxScale();
        }
    }

    private boolean isCurrentBaseMatrixVertical() {
        return !MatrixUtility.contain(this.mBaseMatrixArray, getCurrentBaseMatrix());
    }

    /**
     * 如果尺寸大于fit_center，那么一律先fit_center
     */
    protected Matrix getNextBaseMatrix() {
        //因为切换BaseMatrx动画,有可能不是百分比匹配的,因为要根据手势位置进行缩放,有些移动,所以不能直接比较equal,得比较scale

        Matrix[] matrixArray = isCurrentBaseMatrixVertical() ? this.mBaseVerticalMatrixArray : this.mBaseMatrixArray;

        float currentScale = MatrixUtility.getScale(getCurrentDrawMatrix());
        float fitCenterScale = MatrixUtility.getScale(matrixArray[MatrixUtility.FIT_CENTER]);
        float cropCenterScale = MatrixUtility.getScale(matrixArray[MatrixUtility.CROP_CENTER]);
        float fullCenterScale = MatrixUtility.getScale(matrixArray[MatrixUtility.FULL_CENTER]);

        if (MatrixUtility.equal(currentScale, fitCenterScale)) {
            return matrixArray[MatrixUtility.CROP_CENTER];
        } else if (MatrixUtility.equal(currentScale, cropCenterScale)) {
            return matrixArray[MatrixUtility.FULL_CENTER];
        } else if (MatrixUtility.equal(currentScale, fullCenterScale)) {
            return matrixArray[MatrixUtility.FIT_CENTER];
        } else {
            return matrixArray[MatrixUtility.FIT_CENTER];
        }
    }

    protected Matrix getCurrentVerticalMatrix() {
        if (isCurrentBaseMatrixVertical()) {
            return getCurrentBaseMatrix();
        }

        return this.mBaseVerticalMatrixArray[MatrixUtility.indexOf(this.mBaseMatrixArray, getCurrentBaseMatrix())];
    }

    protected Matrix getCurrentHorizontalMatrix() {
        if (!isCurrentBaseMatrixVertical()) {
            return getCurrentBaseMatrix();
        }

        return this.mBaseMatrixArray[MatrixUtility.indexOf(this.mBaseVerticalMatrixArray, getCurrentBaseMatrix())];
    }

    protected Matrix getMinBaseMatrix() {
        Matrix matrix = mBaseMatrixArray[0];
        float scale = MatrixUtility.getScale(matrix);
        for (int i = 1; i < mBaseMatrixArray.length; i++) {
            Matrix baseMatrix = mBaseMatrixArray[i];
            float baseMatrixScale = MatrixUtility.getScale(baseMatrix);
            if (baseMatrixScale < scale) {
                matrix = baseMatrix;
                scale = baseMatrixScale;
            }
        }
        return matrix;
    }

    //只有fitCenter才需要重新计算垂直下的情况,其他无所谓的
    protected Matrix getMinVerticalBaseMatrix() {
        Matrix matrix = mBaseVerticalMatrixArray[0];
        float scale = MatrixUtility.getScale(matrix);
        for (int i = 1; i < mBaseVerticalMatrixArray.length; i++) {
            Matrix baseMatrix = mBaseVerticalMatrixArray[i];
            float baseMatrixScale = MatrixUtility.getScale(baseMatrix);
            if (baseMatrixScale < scale) {
                matrix = baseMatrix;
                scale = baseMatrixScale;
            }
        }
        return matrix;
    }

    protected Matrix getMaxBaseMatrix() {
        Matrix matrix = mBaseMatrixArray[0];
        float scale = MatrixUtility.getScale(matrix);
        for (int i = 1; i < mBaseMatrixArray.length; i++) {
            Matrix baseMatrix = mBaseMatrixArray[i];
            float baseMatrixScale = MatrixUtility.getScale(baseMatrix);
            if (baseMatrixScale > scale) {
                matrix = baseMatrix;
                scale = baseMatrixScale;
            }
        }
        return matrix;
    }

    protected void invalidateDraw() {
        invalidate();
    }

    @Override
    public void invalidate() {
        if (mFullImage != null && !this.mFull2ThumbnailMatrix.isIdentity()) {
            this.mFullImageDrawMatrix.set(this.mFull2ThumbnailMatrix);
            this.mFullImageDrawMatrix.postConcat(this.mDrawMatrix);
            mFullImage.update(mViewRect, this.mFullImageDrawMatrix);
        }
        super.invalidate();
    }

    protected RectF getViewRect() {
        return mViewRect;
    }

    protected void translate(float dx, float dy) {
        mDrawMatrix.postTranslate(dx, 0);
        mDrawMatrix.postTranslate(0, dy);
    }

    protected void scale(float scaleX, float scaleY, float pivotX, float pivotY) {
        mDrawMatrix.postScale(scaleX, scaleY, pivotX, pivotY);
    }

    protected void rotate(float rotateBy, float pivotX, float pivotY) {
        mDrawMatrix.postRotate(rotateBy, pivotX, pivotY);
    }

    protected Matrix getCurrentDrawMatrix() {
        return mDrawMatrix;
    }

    protected void setCurrentDrawMatrix(Matrix matrix) {
        this.mDrawMatrix.set(matrix);
    }

    protected void postCurrentDrawMatrix(Matrix matrix) {
        this.mDrawMatrix.postConcat(matrix);
    }

    protected void setBaseMatrix(Matrix matrix) {
        this.mBaseMatrix = matrix;
    }

    protected Matrix getCurrentBaseMatrix() {
        return mBaseMatrix;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mFullImage != null) {
            mFullImage.destroy();
        }

        if (mThumbnailImage != null) {
            mThumbnailImage.destroy();
        }
    }

    private void log(String log) {
        if (isDebug()) {
            Log.d(TAG, log);
        }
    }
}
