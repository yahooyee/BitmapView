package org.qiibeta.bitmapview;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import org.qiibeta.bitmapview.gesture.AppGestureDetector;
import org.qiibeta.bitmapview.utility.AppTimeAnimator;
import org.qiibeta.bitmapview.utility.DimenUtility;
import org.qiibeta.bitmapview.utility.MatrixUtility;

import static org.qiibeta.bitmapview.utility.MatrixUtility.getScale;
import static org.qiibeta.bitmapview.utility.MatrixUtility.getTransRotation;

public class GestureBitmapView extends BitmapView {
    public interface OnClickListener {
        public void onClick(View v);
    }

    public interface OnLongClickListener {
        boolean onLongClick(View v);
    }

    private static final long RESTORING_ANIMATION_DURATION = 300;
    private static final long SWITCH_BASE_MATRIX_ANIMATION_DURATION = 300;

    private static final float MIN_SCALE_FACTOR = 0.3f;
    private static final float MAX_SCALE_FACTOR = 7.0f;
    private static final float MAX_SCALE_REBOUND_FACTOR = 0.8f;
    private float mMaxScale = 1.0f;
    private float mMinScale = 1.0f;

    private AppGestureDetector mGestureDetector;

    private AppTimeAnimator mFlingAnimator;//can be interrupted
    private Animator mRestoreAnimator;//can be interrupted
    private Animator mSwitchBaseMatrixAnimator;//can not be interrupted

    private OnClickListener mOnClickListener;
    private OnLongClickListener mOnLongClickListener;

    public GestureBitmapView(Context context) {
        this(context, null);
    }

    public GestureBitmapView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public GestureBitmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        this.mGestureDetector = new AppGestureDetector(getContext(), mGestureDetectorCallback);
    }

    public OnLongClickListener getOnLongClickListener() {
        return mOnLongClickListener;
    }

    public void setOnLongClickListener(OnLongClickListener onLongClickListener) {
        this.mOnLongClickListener = onLongClickListener;
    }

    public OnClickListener getOnClickListener() {
        return mOnClickListener;
    }

    public void setOnClickListener(OnClickListener onClickListener) {
        this.mOnClickListener = onClickListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetector.onTouch(event);
    }

    private float getMinScale() {
        return mMinScale;
    }

    //最小和最大缩放值也是计算出来的,最小的BaseMatrix缩放值基础上*0.3f,最大BaseMatrix的缩放值*5
    @Override
    protected void calculateMinAndMaxScale() {
        super.calculateMinAndMaxScale();
        Matrix minBaseMatrix = getMinBaseMatrix();
        float fitCenterScaleValue = MatrixUtility.getScale(minBaseMatrix);
        mMinScale = MIN_SCALE_FACTOR * fitCenterScaleValue;

        Matrix maxBaseMatrix = getMaxBaseMatrix();
        mMaxScale = MAX_SCALE_FACTOR * MatrixUtility.getScale(maxBaseMatrix);
    }

    private boolean forceStopRunningAnimator() {
        //切换Matrix动画是由于2次点击触发的,所以不能强制终止,不好处理
        if (mSwitchBaseMatrixAnimator != null) {
            log("因为SwitchBaseMatrixAnimator动画在执行,所以手势onDown失败");
            return false;
        }
        log("forceStopRunningAnimator");
        if (mFlingAnimator != null) {
            mFlingAnimator.cancel();
            mFlingAnimator = null;
        }

        if (mRestoreAnimator != null) {
            mRestoreAnimator.cancel();
            mRestoreAnimator = null;
        }

        return true;
    }

    private void forceStopFlingAndRestoreAnimator() {
        log("forceStopFlingAndRestoreAnimator");
        if (mFlingAnimator != null) {
            mFlingAnimator.cancel();
            mFlingAnimator = null;
        }

        if (mRestoreAnimator != null) {
            mRestoreAnimator.cancel();
            mRestoreAnimator = null;
        }
    }

    private AppGestureDetector.Callback mGestureDetectorCallback = new AppGestureDetector.Callback() {
        @Override
        public boolean onDown(MotionEvent e) {
            if (getImageWidth() == 0 || getImageHeight() == 0) {
                return false;
            }
            if (forceStopRunningAnimator()) {
                log("onDown:" + true);
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;
            } else {
                log("onDown:" + false);
                return false;
            }
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (GestureBitmapView.this.mOnClickListener != null) {
                GestureBitmapView.this.mOnClickListener.onClick(GestureBitmapView.this);
                return true;
            }
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            if (GestureBitmapView.this.mOnLongClickListener != null) {
                GestureBitmapView.this.mOnLongClickListener.onLongClick(GestureBitmapView.this);
            }
        }

        //手势移动
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//            log("onScroll");
            RectF oriRect = getInitImageRect();
            RectF dstRect = new RectF();
            getCurrentDrawMatrix().mapRect(dstRect, oriRect);

            //移动上下，超过边界后越来越难拉
            float deltaY = distanceY;
            if (dstRect.top >= 0) {//超过边界了
                int maxOverDistance = DimenUtility.dip2px(getContext(), 80);//最多超过50dp
                deltaY = (Math.max(0, maxOverDistance - dstRect.top)) / maxOverDistance * deltaY;
            } else if (dstRect.bottom <= getHeight()) {
                int maxOverDistance = DimenUtility.dip2px(getContext(), 80);//最多超过50dp
                deltaY = (Math.max(0, maxOverDistance - (getHeight() - dstRect.bottom))) / maxOverDistance * deltaY;
            }

            if (distanceX < 0) {
                float leftLeftSpace = -dstRect.left;
                if (leftLeftSpace <= 0 && Math.abs(distanceX) > Math.abs(distanceY)) {
                    //只有一只手指的时候才运行父容器捕获手势,两只手指有可能是要旋转
                    if (!isIsMultiPointers()) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                } else if (leftLeftSpace > 0) {
                    float move = Math.min(leftLeftSpace, Math.abs(distanceX));

                    translate(move, -deltaY);
                    invalidateDraw();
                } else {
                    //万一到左右边界，然后上下拉，然后x<y的情况
                    translate(0, -deltaY);
                    invalidateDraw();
                }
            } else if (distanceX > 0) {
                float rightLeftSpace = dstRect.right - getWidth();
                if (rightLeftSpace <= 0 && Math.abs(distanceX) > Math.abs(distanceY)) {
                    //只有一只手指的时候才运行父容器捕获手势,两只手指有可能是要旋转
                    if (!isIsMultiPointers()) {
                        getParent().requestDisallowInterceptTouchEvent(false);
                    }
                } else if (rightLeftSpace > 0) {
                    float move = Math.min(rightLeftSpace, Math.abs(distanceX));
                    translate(-move, -deltaY);
                    invalidateDraw();
                } else {
                    //万一到左右边界，然后上下拉，然后x<y的情况
                    translate(0, -deltaY);
                    invalidateDraw();
                }
            }

            boolean isScrollOutsideEdge;
            if (dstRect.top > 0 || dstRect.bottom < getHeight()) {
                isScrollOutsideEdge = true;
            } else if (dstRect.top <= 0 && dstRect.bottom >= getHeight()) {
                isScrollOutsideEdge = false;
            }

//			mMatrix.postTranslate(-distanceX, -distanceY);
//			setImageMatrix(mMatrix);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            log("onFling");

            if (mRestoreAnimator != null) {
                log("RestoreAnimator is executing, skip FlingAnimator");
                return false;
            }

            final OverScroller scroller = new OverScroller(getContext());

            final RectF currentRect = new RectF(0, 0, getImageWidth(), getImageHeight());
            getCurrentDrawMatrix().mapRect(currentRect);

            int minX = 0;
            int maxX = 0;
            int minY = 0;
            int maxY = 0;
            if (Math.signum(velocityX) > 0) {
                minX = 0;
                maxX = -(int) currentRect.left;
            } else {
                minX = (int) -(currentRect.right - getWidth());
                maxX = 0;
            }

            if (Math.signum(velocityY) > 0) {
                minY = 0;
                maxY = -(int) currentRect.top;
            } else {
                minY = (int) -(currentRect.bottom - getHeight());
                maxY = 0;
            }

            //什么都不用滚
            if (minX == 0 && maxX == 0 && minY == 0 && maxY == 0) {
//                getParent().requestDisallowInterceptTouchEvent(false);
                return false;
            }

            scroller.fling(0, 0, (int) velocityX, (int) velocityY, minX, maxX, minY, maxY, 0, 0);
            log("onFlingAnimator final position:" + scroller.getFinalY());

            mFlingAnimator = new AppTimeAnimator(GestureBitmapView.this);
            mFlingAnimator.setListener(new AppTimeAnimator.AnimatorListener() {
                int previousX = 0;
                int previousY = 0;
                boolean isCanceled = false;

                @Override
                public void onAnimationUpdate(AppTimeAnimator animation) {
                    super.onAnimationUpdate(animation);
                    if (!scroller.isFinished()) {
                        scroller.computeScrollOffset();
                        int x = scroller.getCurrX();
                        int y = scroller.getCurrY();
//                        Log.e("x", "value=" + x);
                        if (previousX != 0 || previousY != 0) {
                            translate(x - previousX, y - previousY);
                            invalidateDraw();
                        }
                        previousX = x;
                        previousY = y;
                        invalidate();
                    } else {
                        previousX = 0;
                        previousY = 0;
                        if (mFlingAnimator != null) {
                            mFlingAnimator.end();
                        }
                    }
                }

                @Override
                public void onAnimationCancel(AppTimeAnimator animation) {
                    super.onAnimationCancel(animation);
                    isCanceled = true;
                }

                @Override
                public void onAnimationEnd(AppTimeAnimator animation) {
                    super.onAnimationEnd(animation);
                    mFlingAnimator = null;
                    if (isCanceled) {
                        log("onFlingAnimator is canceled");
                    } else {
                        log("onFlingAnimator is finished");
                    }
                    restoreToBaseMatrix(-1, -1);
                }
            });
            mFlingAnimator.start();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
//            log("onScale");
            float targetScale = detector.getScaleFactor();
            Matrix drawMatrix = getCurrentDrawMatrix();
            if (getScale(drawMatrix) * targetScale < getMinScale()) {
                return false;
            }
            if (targetScale >= 1 && getScale(drawMatrix) * targetScale > mMaxScale) {
                return false;
            }

            float pivotX = detector.getFocusX();
            float pivotY = detector.getFocusY();

            scale(targetScale, targetScale, pivotX, pivotY);
            invalidateDraw();
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            super.onScaleEnd(detector);
            float pivotX = detector.getFocusX();
            float pivotY = detector.getFocusY();
            restoreToBaseMatrix(pivotX, pivotY);
        }

        //todo 中心貌似在移动图片后有问题
        @Override
        public void onRotateBegin(float pivotX, float pivotY, float rotateBy) {
//            log("onRotateBegin");
            getParent().requestDisallowInterceptTouchEvent(true);
            rotate(rotateBy, pivotX, pivotY);
            invalidateDraw();
        }

        @Override
        public void onRotateEnd(float pivotX, float pivotY) {
            super.onRotateEnd(pivotX, pivotY);
//            getParent().requestDisallowInterceptTouchEvent(false);
            restoreToBaseMatrix(pivotX, pivotY);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            log("onDoubleTap");
            //这个双击是延迟执行的,所有有可能这个时候其他动画已经在跑了
            forceStopFlingAndRestoreAnimator();
//			mBaseMatrix = matrix;
//			mDrawMatrix.set(mBaseMatrix);
//			invalidateDraw();

            if (mSwitchBaseMatrixAnimator != null) {
                log("SwitchBaseMatrixAnimator 还在执行,放弃");
                return false;
            }
            log("SwitchBaseMatrixAnimator 开始执行");

            float pivotX = e.getX();
            float pivotY = e.getY();

            final Matrix nextBaseMatrix = getNextBaseMatrix();
            float targetScale = MatrixUtility.getScale(nextBaseMatrix);
            float currentScale = MatrixUtility.getScale(getCurrentDrawMatrix());
            Matrix dstMatrix = new Matrix(getCurrentDrawMatrix());
            dstMatrix.postScale(targetScale / currentScale, targetScale / currentScale, pivotX, pivotY);

            //检查缩放后,是不是有贴边问题
            RectF srcRect = new RectF(getInitImageRect());
            dstMatrix.mapRect(srcRect);
            if (srcRect.left <= 0 && srcRect.top <= 0 && srcRect.right >= getWidth() && srcRect.bottom >= getHeight()) {
                //什么都不用做
            } else {
                //水平居中
                if (srcRect.left > 0 || srcRect.right < getWidth()) {
                    dstMatrix.postTranslate(getWidth() / 2 - srcRect.centerX(), 0);
                }

                //垂直居中
                if (srcRect.top > 0 || srcRect.bottom < getHeight()) {
                    dstMatrix.postTranslate(0, getHeight() / 2 - srcRect.centerY());
                }
            }

            mSwitchBaseMatrixAnimator = MatrixUtility.getMatrix2MatrixAnimator(
                    getCurrentDrawMatrix(), dstMatrix, new MatrixUtility.AnimatorCallback() {
                        @Override
                        public void onAnimate(Matrix drawMatrix) {
                            setCurrentDrawMatrix(drawMatrix);
                            invalidateDraw();
                        }

                        @Override
                        public void onEnd(boolean isCanceled, Matrix dstMatrix) {
                            if (isCanceled) {
                                log("SwitchBaseMatrixAnimator 被取消");
                            } else {
                                log("SwitchBaseMatrixAnimator 结束");
                                setBaseMatrix(nextBaseMatrix);
                                setCurrentDrawMatrix(dstMatrix);
                            }
                            invalidateDraw();
                            mSwitchBaseMatrixAnimator = null;
                            restoreToBaseMatrix(-1, -1);//万一旋转不对,很有可能的,在旋转后一瞬间双击,那么先切换Matrix动画,然后归位动画
                        }
                    });
            mSwitchBaseMatrixAnimator.setDuration(SWITCH_BASE_MATRIX_ANIMATION_DURATION);
            mSwitchBaseMatrixAnimator.start();
            return true;
        }

        @Override
        public void onGestureEnd() {
            log("onGestureEnd");
            restoreToBaseMatrix(-1, -1);
        }

        @Override
        public void onGestureCancel() {
            super.onGestureCancel();
            log("onGestureCancel");
            restoreToBaseMatrix(-1, -1);
        }
    };

    private boolean isRestoreAnimating = false;

    private void restoreToBaseMatrix(float previousScaleOrRotatePivotX, float previousScaleOrRotatePivotY) {
        if (mSwitchBaseMatrixAnimator != null) {
            log("RestoreAnimator 因为切换BaseMatrix动画没结束,所以放弃");
            return;
        }

        if (mFlingAnimator != null) {
            log("RestoreAnimator 因为Fling动画没结束,所以放弃");
            return;
        }

        Matrix mDrawMatrix = getCurrentDrawMatrix();
        final int mIntrinsicWidth = getImageWidth();
        final int mIntrinsicHeight = getImageHeight();

        //小于最小baseMatrix就缩放到最小matrix，其他情况旋转位置就好

        if (isRestoreAnimating) {
            log("RestoreAnimator 已经在执行归位动画了,放弃");
            return;
        }

        float rotation = MatrixUtility.getTransRotation(mDrawMatrix);
        RectF srcRect = new RectF();
        mDrawMatrix.mapRect(srcRect, new RectF(0, 0, mIntrinsicWidth, mIntrinsicHeight));

        if (rotation == 0) {
            if ((srcRect.left <= 0 && srcRect.right >= getWidth()) || (srcRect.top <= 0 && srcRect.bottom >= getHeight())) {
                if (Float.compare(srcRect.centerX(), getWidth() / 2) == 0 && Float.compare(srcRect.centerY(), getHeight() / 2) == 0) {
                    log("RestoreAnimator 当前图片不需要归位动画,都在屏幕外面或者贴边（正中心）,也没旋转");
                    return;
                }
            }
        } else {
            log("RestoreAnimator 旋转度数:" + rotation);
        }

        log("RestoreAnimator 执行归位动画");
        isRestoreAnimating = true;

        Matrix minBaseMatrix = getMinBaseMatrix();
        float currentScale = getScale(mDrawMatrix);
        float minBaseScale = getScale(minBaseMatrix);

        float targetRotate = (float) Math.floor((rotation + 45) / 90) * 90;

        final Matrix matrix;
        final Matrix baseMatrix;//有可能重置,有可能不重置
        //当前缩放的太小了,那么归位到fitCenter尺寸
        if (currentScale < minBaseScale) {
            if (targetRotate == 0) {
                baseMatrix = getMinBaseMatrix();
                matrix = new Matrix(baseMatrix);
            } else if (180 == targetRotate || -180 == targetRotate) {
                baseMatrix = getMinBaseMatrix();//重置baseMatrix
                matrix = new Matrix(baseMatrix);//fitcenter尺寸
                matrix.postRotate(180, getWidth() / 2, getHeight() / 2);
            } else {
                baseMatrix = getMinVerticalBaseMatrix();//重置baseMatrix
                matrix = new Matrix(baseMatrix);//如果本身图片太小,那么垂直无所谓的,但如果是fitCenter,需要重新计算

                if (targetRotate == 270 || targetRotate == -90) {
                    matrix.postRotate(180, getWidth() / 2, getHeight() / 2);
                }
            }

            log("RestoreAnimator 度数," + "初始度数:" + rotation + " 最终度数:" + targetRotate);
            log("RestoreAnimator 度数," + "dst 度数:" + getTransRotation(matrix));
        } else {
            //只是旋转角度和移动中心点
            if (targetRotate == 270 || targetRotate == -90 || targetRotate == 90) {
                baseMatrix = getCurrentVerticalMatrix();
            } else {
                //很有可能是从垂直切到水平,所以不能直接拿当前BaseMatrix
                baseMatrix = getCurrentHorizontalMatrix();
            }

            //上次双指旋转的中心点旋转
            final float pivotX = previousScaleOrRotatePivotX != -1 ? previousScaleOrRotatePivotX : srcRect.centerX();
            final float pivotY = previousScaleOrRotatePivotY != -1 ? previousScaleOrRotatePivotY : srcRect.centerY();

            Matrix dstMatrix = new Matrix(mDrawMatrix);
            dstMatrix.postRotate(targetRotate - rotation, pivotX, pivotY);

            //给人一种放大到很大,然后又回退到一点的感觉
            if (currentScale >= this.mMaxScale * MAX_SCALE_REBOUND_FACTOR) {
                float scale = (float) ((this.mMaxScale * MAX_SCALE_REBOUND_FACTOR) / currentScale);
                dstMatrix.postScale(scale, scale, pivotX, pivotY);
            }

            //如果旋转后，有没贴边就移动
            //todo 万一width超过，但是还是没贴边怎么办？
            //todo 旋转后90℃,高宽不都变了吗?
            RectF dstRect = new RectF();
            dstMatrix.mapRect(dstRect, new RectF(0, 0, mIntrinsicWidth, mIntrinsicHeight));

            RectF widgetRect = new RectF(0, 0, getWidth(), getHeight());

            //两条边都小于
            if (dstRect.width() <= widgetRect.width() && dstRect.height() <= widgetRect.height()) {
                dstMatrix.postTranslate(widgetRect.centerX() - dstRect.centerX(), widgetRect.centerY() - dstRect.centerY());//本身BaseMatrix就比屏幕小，移动就好
            } else {
                //一条边是大于，一条边是小于
                if (dstRect.width() > widgetRect.width() && dstRect.height() < widgetRect.height()) {
                    dstMatrix.postTranslate(0, widgetRect.centerY() - dstRect.centerY());
                    //haha,但是就算大于,也不一定贴边啊
                    if (dstRect.left > widgetRect.left) {
                        dstMatrix.postTranslate(widgetRect.left - dstRect.left, 0);
                    } else if (dstRect.right < widgetRect.right) {
                        dstMatrix.postTranslate(widgetRect.right - dstRect.right, 0);
                    }
                } else if (dstRect.width() < widgetRect.width() && dstRect.height() > widgetRect.height()) {
                    dstMatrix.postTranslate(widgetRect.centerX() - dstRect.centerX(), 0);
                    //保证贴边
                    if (dstRect.top > widgetRect.top) {
                        dstMatrix.postTranslate(0, widgetRect.top - dstRect.top);
                    } else if (dstRect.bottom < widgetRect.bottom) {
                        dstMatrix.postTranslate(0, widgetRect.bottom - dstRect.bottom);
                    }
                } else {
                    //保证贴边
                    if (dstRect.top > widgetRect.top) {
                        dstMatrix.postTranslate(0, widgetRect.top - dstRect.top);
                    }

                    if (dstRect.bottom < widgetRect.bottom) {
                        dstMatrix.postTranslate(0, widgetRect.bottom - dstRect.bottom);
                    }

                    if (dstRect.left > widgetRect.left) {
                        dstMatrix.postTranslate(widgetRect.left - dstRect.left, 0);
                    }

                    if (dstRect.right < widgetRect.right) {
                        dstMatrix.postTranslate(widgetRect.right - dstRect.right, 0);
                    }
                }
            }

            //todo 比较最小的baseMatrix 的rect，要跟他贴边

            matrix = dstMatrix;
        }

        mRestoreAnimator = MatrixUtility.getMatrix2MatrixAnimator(
                mDrawMatrix, matrix, new MatrixUtility.AnimatorCallback() {
                    @Override
                    public void onAnimate(Matrix drawMatrix) {
                        setCurrentDrawMatrix(drawMatrix);
                        invalidateDraw();
                    }

                    @Override
                    public void onEnd(boolean isCanceled, Matrix dstMatrix) {
                        if (!isCanceled) {
                            log("RestoreAnimator finish");
                            setBaseMatrix(baseMatrix);

//                            log("RestoreAnimator 当前Matrix scale:" + MatrixUtility.getScale(getCurrentDrawMatrix()));
//                            log("RestoreAnimator 动画最终强制设置的Matrix scale:" + MatrixUtility.getScale(dstMatrix));

                            setCurrentDrawMatrix(dstMatrix);
                        } else {
                            log("RestoreAnimator is canceled");
                        }
                        log("RestoreAnimator finish, final degree:" + getTransRotation(getCurrentDrawMatrix()));

                        invalidateDraw();
                        isRestoreAnimating = false;
                        mRestoreAnimator = null;
                    }
                });
        mRestoreAnimator.setInterpolator(new DecelerateInterpolator());
        mRestoreAnimator.setDuration(RESTORING_ANIMATION_DURATION);
        mRestoreAnimator.start();
    }

    //todo
    @Override
    public boolean canScrollHorizontally(int direction) {
        return super.canScrollHorizontally(direction);
    }

    //todo
    @Override
    public boolean canScrollVertically(int direction) {
        return super.canScrollVertically(direction);
    }

    private static void log(String log) {
        Log.d("GestureBitmapView", log);
    }
}
