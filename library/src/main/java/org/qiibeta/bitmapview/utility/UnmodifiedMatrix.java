package org.qiibeta.bitmapview.utility;


import android.graphics.Matrix;
import android.graphics.RectF;

public class UnmodifiedMatrix extends Matrix {

    public UnmodifiedMatrix(Matrix matrix) {
        super(matrix);
    }

    @Override
    public void setValues(float[] values) {
        crash();
        super.setValues(values);
    }

    @Override
    public boolean postConcat(Matrix other) {
        crash();
        return super.postConcat(other);
    }

    @Override
    public boolean postSkew(float kx, float ky) {
        crash();
        return super.postSkew(kx, ky);
    }

    @Override
    public boolean postSkew(float kx, float ky, float px, float py) {
        crash();
        return super.postSkew(kx, ky, px, py);
    }

    @Override
    public boolean postRotate(float degrees) {
        crash();
        return super.postRotate(degrees);
    }

    @Override
    public boolean postRotate(float degrees, float px, float py) {
        crash();
        return super.postRotate(degrees, px, py);
    }

    @Override
    public boolean postScale(float sx, float sy) {
        crash();
        return super.postScale(sx, sy);
    }

    //todo 蛋疼啊,因为要修正载入大图后小图的原始尺寸
    @Override
    public boolean postScale(float sx, float sy, float px, float py) {
//        crash();
        return super.postScale(sx, sy, px, py);
    }

    @Override
    public boolean postTranslate(float dx, float dy) {
        crash();
        return super.postTranslate(dx, dy);
    }

    @Override
    public boolean preConcat(Matrix other) {
        crash();
        return super.preConcat(other);
    }

    @Override
    public boolean preSkew(float kx, float ky) {
        crash();
        return super.preSkew(kx, ky);
    }

    @Override
    public boolean preSkew(float kx, float ky, float px, float py) {
        crash();
        return super.preSkew(kx, ky, px, py);
    }

    @Override
    public boolean preRotate(float degrees) {
        crash();
        return super.preRotate(degrees);
    }

    @Override
    public boolean preRotate(float degrees, float px, float py) {
        crash();
        return super.preRotate(degrees, px, py);
    }

    @Override
    public boolean preScale(float sx, float sy) {
        crash();
        return super.preScale(sx, sy);
    }

    @Override
    public boolean preScale(float sx, float sy, float px, float py) {
        crash();
        return super.preScale(sx, sy, px, py);
    }

    @Override
    public boolean preTranslate(float dx, float dy) {
        crash();
        return super.preTranslate(dx, dy);
    }

    @Override
    public boolean setConcat(Matrix a, Matrix b) {
        crash();
        return super.setConcat(a, b);
    }

    @Override
    public void setSkew(float kx, float ky) {
        crash();
        super.setSkew(kx, ky);
    }

    @Override
    public void setSkew(float kx, float ky, float px, float py) {
        crash();
        super.setSkew(kx, ky, px, py);
    }

    @Override
    public void setSinCos(float sinValue, float cosValue) {
        crash();
        super.setSinCos(sinValue, cosValue);
    }

    @Override
    public void setSinCos(float sinValue, float cosValue, float px, float py) {
        crash();
        super.setSinCos(sinValue, cosValue, px, py);
    }

    @Override
    public void setRotate(float degrees) {
        crash();
        super.setRotate(degrees);
    }

    @Override
    public void setRotate(float degrees, float px, float py) {
        crash();
        super.setRotate(degrees, px, py);
    }

    @Override
    public void setScale(float sx, float sy) {
        crash();
        super.setScale(sx, sy);
    }

    @Override
    public void setScale(float sx, float sy, float px, float py) {
        crash();
        super.setScale(sx, sy, px, py);
    }

    @Override
    public void setTranslate(float dx, float dy) {
        crash();
        super.setTranslate(dx, dy);
    }

    @Override
    public void reset() {
        crash();
        super.reset();
    }

    @Override
    public void set(Matrix src) {
        crash();
        super.set(src);
    }

    @Override
    public boolean setRectToRect(RectF src, RectF dst, ScaleToFit stf) {
        crash();
        return super.setRectToRect(src, dst, stf);
    }

    @Override
    public boolean setPolyToPoly(float[] src, int srcIndex, float[] dst, int dstIndex, int pointCount) {
        crash();
        return super.setPolyToPoly(src, srcIndex, dst, dstIndex, pointCount);
    }

    private void crash() {
        throw new IllegalStateException("This matrix can't be modified");
    }
}
