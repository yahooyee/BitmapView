package com.qiibeta.bitmapviewsample.support;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.qiibeta.bitmapviewsample.support.mrc.BitmapMRC;

public class FolderImageView extends View {
    private BitmapMRC mBitmap;
    private Rect mBitmapRect = new Rect();
    private Rect mViewRect = new Rect();
    private Paint mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public FolderImageView(Context context) {
        super(context);
    }

    public FolderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FolderImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY));
    }

    public void setImageBitmap(BitmapMRC bitmap) {
        if (this.mBitmap != null) {
            this.mBitmap.release();
        }
        this.mBitmap = bitmap;
        if (this.mBitmap == null) {
            invalidate();
            return;
        }
        this.mBitmap.retain();
        if (this.mBitmap != null) {
            this.mBitmapRect.set(0, 0, this.mBitmap.getWidth(), this.mBitmap.getHeight());
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mViewRect.set(0, 0, w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mBitmap != null) {
            canvas.drawBitmap(this.mBitmap.getBitmap(), this.mBitmapRect, this.mViewRect, this.mBitmapPaint);
        }
        super.onDraw(canvas);
    }
}
