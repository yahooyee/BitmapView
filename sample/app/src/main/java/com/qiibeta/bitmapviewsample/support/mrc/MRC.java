package com.qiibeta.bitmapviewsample.support.mrc;


import java.util.concurrent.atomic.AtomicInteger;

public class MRC {
    private AtomicInteger mCount = new AtomicInteger();

    public void retain() {
        this.mCount.addAndGet(1);
    }

    public void release() {
        if (this.mCount.decrementAndGet() == 0) {
            destroy();
        }
    }

    protected void destroy() {

    }
}
