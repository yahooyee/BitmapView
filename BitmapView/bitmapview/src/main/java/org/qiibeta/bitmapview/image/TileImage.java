package org.qiibeta.bitmapview.image;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.Log;
import android.util.LruCache;

import org.qiibeta.bitmapview.OrientationInfoUtility;
import org.qiibeta.bitmapview.utility.DimenUtility;
import org.qiibeta.bitmapview.utility.MatrixUtility;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class TileImage extends AppImage {
    public static TileImage newInstance(int width, int height, @OrientationInfoUtility.ORIENTATION_ROTATE int orientation,
                                        BitmapRegionDecoder bitmapRegionDecoder) {
        TileImage image = new TileImage();
        image.mWidth = width;
        image.mHeight = height;
        image.mOrientation = orientation;
        image.mBitmapRegionDecoder = bitmapRegionDecoder;
        return image;
    }

    public static void clearBitmapRecyclePool() {
        synchronized (sRecycledBitmapQueue) {
            Object[] bitmaps = sRecycledBitmapQueue.toArray();
            sRecycledBitmapQueue.clear();
            for (int i = 0; i < bitmaps.length; i++) {
                ((Bitmap) bitmaps[i]).recycle();
            }
            Log.d("TileImage", "clearBitmapRecyclePool 回收了" + bitmaps.length + "个 Bitmap的内存");
        }
    }

    private static final String TAG = "TileImage";
    private static final boolean USE_IN_BITMAP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    private static final int TILE_SIZE = 256;
    private int mWidth;
    private int mHeight;
    private int mOrientation;

    private Matrix mFixOrientationMatrix = new Matrix();
    //View相对完整图的Matrix
    private Matrix mMatrix = new Matrix();
    //View在完整图中的矩形位置
    private volatile RectF mRect = new RectF();
    private int mIsSample = 1;
    private int mImageLeft = 0;
    private int mImageTop = 0;
    private int mImageRight = 0;
    private int mImageBottom = 0;

    private volatile boolean mStarted = false;
    private BitmapRegionDecoder mBitmapRegionDecoder;
    private Thread mThread = null;
    private LinkedBlockingQueue<Tile> mTaskQueue = new AppLinkedBlockingQueue();
    private AppLruCache mCache = new AppLruCache(calculateLruCacheSize());
    //全局复用
    private static final Queue<Bitmap> sRecycledBitmapQueue = new LinkedList<>();//todo 这个要不要加限制数量?
    private volatile long mCurrentTaskTileKey;//有可能当前正在执行这个Tile任务,重复添加

    //Debug
    private Paint mDebugPaint = new Paint();

    private void startTaskIfNeeded() {
        if (!mStarted) {
            if (mThread == null) {
                mThread = new Thread(mDecodeTask);
                mThread.start();
            }
            mStarted = true;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mThread != null) {
            mThread.interrupt();
            mThread = null;
        }
        mStarted = false;
        mCache.evictAll();
    }

    //todo 算不对,很奇怪
    private int calculateLruCacheSize() {
        int screenWidth = DimenUtility.getScreenWidth();
        int screenHeight = DimenUtility.getScreenHeight();

        int width = (int) (Math.ceil((double) screenWidth / TILE_SIZE) * TILE_SIZE);
        int height = (int) (Math.ceil((double) screenHeight / TILE_SIZE) * TILE_SIZE);
        return width * height * 4 * 2 * 3;//ARGB是4Bytes
    }

    @Override
    public void update(RectF viewRect, Matrix matrix) {
        mFixOrientationMatrix.set(matrix);
        if (mOrientation != 0) {
            if (mOrientation == 90) {
                mFixOrientationMatrix.preTranslate(mHeight, 0);
            } else if (mOrientation == 180) {
                mFixOrientationMatrix.preTranslate(mWidth, mHeight);
            } else if (mOrientation == 270) {
                mFixOrientationMatrix.preTranslate(0, mWidth);
            }
            mFixOrientationMatrix.preRotate(mOrientation);
        }
        mFixOrientationMatrix.invert(mMatrix);
        mMatrix.mapRect(mRect, viewRect);
        float viewScaleValue = MatrixUtility.getScale(mMatrix);

        int sample = 1;
        while (true) {
            if (sample * 2 > viewScaleValue) {
                break;
            } else {
                sample *= 2;
            }
        }

        this.mIsSample = sample;

        float left = Math.max(0, mRect.left);
        float top = Math.max(0, mRect.top);
        float right = Math.min(mWidth, mRect.right);
        float bottom = Math.min(mHeight, mRect.bottom);

        int titleSize = TILE_SIZE * sample;
        this.mImageLeft = (int) (titleSize * Math.floor(left / titleSize));
        this.mImageTop = (int) (titleSize * Math.floor(top / titleSize));
        this.mImageRight = (int) (titleSize * Math.ceil(right / titleSize));
        this.mImageBottom = (int) (titleSize * Math.ceil(bottom / titleSize));

//		Log.e("TileImage", mRect.toShortString());
        startTaskIfNeeded();
    }

    //针对缩放,区域解码也是带缩放的
    private Matrix mBitmapMatrix = new Matrix();

    @Override
    public void onDraw(Canvas canvas, Matrix matrix) {
        startTaskIfNeeded();//如果放到RecyclerView中,有可能onDetachedFromWindow中停止了当前,然后又滚回去,什么Matrix都没改,不会触发update,蛋疼
        mFixOrientationMatrix.set(matrix);
        if (mOrientation != 0) {
            if (mOrientation == 90) {
                mFixOrientationMatrix.preTranslate(mHeight, 0);
            } else if (mOrientation == 180) {
                mFixOrientationMatrix.preTranslate(mWidth, mHeight);
            } else if (mOrientation == 270) {
                mFixOrientationMatrix.preTranslate(0, mWidth);
            }
            mFixOrientationMatrix.preRotate(mOrientation);
        }

        canvas.save();
        canvas.concat(mFixOrientationMatrix);

        //调试用
        if (isDebug()) {
            mDebugPaint.setColor(0x66FF0000);
            canvas.drawRect(0, 0, mWidth, mHeight, mDebugPaint);
            mDebugPaint.setColor(Color.GREEN);
            canvas.drawRect(0, 0, 50, mHeight, mDebugPaint);
            canvas.drawRect(mWidth - 50, 0, mWidth, mHeight, mDebugPaint);
        }

        //这张jpg png无法用系统BitmapRegionDecoder解析,放弃
        if (this.mBitmapRegionDecoder == null) {
            return;
        }

        //一次请求区域解码最多4次
        int requestCount = 4;
        for (int top = mImageTop; top < mImageBottom; top += TILE_SIZE * mIsSample) {
            for (int left = mImageLeft; left < mImageRight; left += TILE_SIZE * mIsSample) {
                long tileKey = getCacheKey(left, top, mIsSample);
                Tile tile = mCache.get(tileKey);
                if (tile != null) {
                    Bitmap bitmap = tile.bitmap;
                    if (bitmap != null) {
                        mBitmapMatrix.setScale(tile.isSample, tile.isSample);
                        mBitmapMatrix.postTranslate(left, top);
                        canvas.drawBitmap(bitmap, mBitmapMatrix, null);
                    }
                } else {
                    if (requestCount >= 0) {
                        if (tileKey != mCurrentTaskTileKey) {
                            mTaskQueue.add(new Tile(left, top, mIsSample));
                        }
                        requestCount--;
                    }
                }
            }
        }

        canvas.restore();
    }

    @Override
    public int getWidth() {
        return mOrientation == 0 || mOrientation == 180 ? mWidth : mHeight;
    }

    @Override
    public int getHeight() {
        return mOrientation == 0 || mOrientation == 180 ? mHeight : mWidth;
    }

    private static class Tile {
        public int left;
        public int top;
        public int isSample;
        public Bitmap bitmap;

        Tile(int left, int top, int isSample) {
            this.left = left;
            this.top = top;
            this.isSample = isSample;
        }

        @Override
        public String toString() {
            return "left:" + left + " top:" + top + " isSample:" + isSample;
        }
    }

    //long 8bytes
    // 3byte left
    // 3byte top
    // 1byte isSample
    private long getCacheKey(int left, int top, int isSample) {
        long a = (long) left << 4 * 8;
        long b = (long) top << 8;
        //        return "left=" + left + ",top=" + top + ",isSample=" + isSample;
        return a + b + isSample;
    }

    private int getLeftFromKey(long key) {
        return (int) (key >>> (4 * 8));
    }

    private int getTopFromKey(long key) {
        long top = (long) (key >>> (8));
        top &= (0xFFF);
        return (int) top;
    }

    private int getIsSample(long key) {
        long sample = (long) (key);
        sample &= (0xF);
        return (int) sample;
    }

    private class AppLinkedBlockingQueue extends LinkedBlockingQueue<Tile> {
        private HashSet<Long> mTileIdentifySet = new HashSet<>();

        @Override
        public boolean add(Tile tile) {
            long key = getCacheKey(tile.left, tile.top, tile.isSample);
            //不添加重复的
            if (mTileIdentifySet.contains(key)) {
                return false;
            }

            //队列最多只能10个
            if (this.size() >= 10) {
                return false;
            }

            mTileIdentifySet.add(key);
            return super.add(tile);
        }

        @Override
        public Tile take() throws InterruptedException {
            Tile tile = super.take();
            if (tile != null) {
                long key = getCacheKey(tile.left, tile.top, tile.isSample);
                mTileIdentifySet.remove(key);
            }
            return tile;
        }
    }

    private Runnable mDecodeTask = new Runnable() {
        private static final int REFRESH_INTERVAL = 4;//每执行4次刷新下View
        private volatile int mRefreshViewCount = REFRESH_INTERVAL;
        private final BitmapFactory.Options mOptions = new BitmapFactory.Options();

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Tile tile = mTaskQueue.take();
                    mCurrentTaskTileKey = getCacheKey(tile.left, tile.top, tile.isSample);

                    int tileLeft = tile.left;
                    int tileTop = tile.top;
                    int tileRight = tile.left + TILE_SIZE * tile.isSample;
                    int tileBottom = tile.top + TILE_SIZE * tile.isSample;
//                    log("LruCache 任务 Tile: " + tile.toString());

                    //double check
                    if (mRect.intersects(tileLeft, tileTop, tileRight, tileBottom)) {
                        Bitmap bitmap = decode(tile, mOptions);
                        if (mRect.intersects(tileLeft, tileTop, tileRight, tileBottom)) {
                            tile.bitmap = bitmap;
                            addTileToLruCache(getCacheKey(tile.left, tile.top, tile.isSample), tile);
                        } else {
                            log("after decode, Tile is out of visible area, try to recycle it");
                            tryToRecycleBitmap(bitmap);
                        }
                    } else {
                        log("before decode, Tile is out of visible area, skip");
                    }

                    if (--mRefreshViewCount <= 0 || mTaskQueue.size() == 0) {
                        log("refresh view to continue to decode other Tiles");
                        postInvalidate();
                        mRefreshViewCount = REFRESH_INTERVAL;
                    } else {
//						log("刷新View失败,没到间隔 " + mRefreshViewCount);
                    }
                    mCurrentTaskTileKey = -1;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    e.printStackTrace();
                }
            }
        }
    };

    private Bitmap decode(Tile tile, BitmapFactory.Options options) {
        options.inMutable = false;//文档要求说要写这个,实际上我测试在decodeRegion又不需要,还是写上吧,true 有大坑
        options.inBitmap = null;
        options.inSampleSize = tile.isSample;

        int tileSize = TILE_SIZE * tile.isSample;

        int left = tile.left;
        int top = tile.top;
        //避免超过区域,不然有黑边
        int right = Math.min(mWidth, tile.left + tileSize);
        int bottom = Math.min(mHeight, tile.top + tileSize);

        //正方形,没有少掉部分
        Bitmap recycledBitmap = null;
        if (right - left == tileSize && bottom - top == tileSize) {
            synchronized (sRecycledBitmapQueue) {
                recycledBitmap = sRecycledBitmapQueue.poll();
            }
            if (recycledBitmap != null) {
                options.inBitmap = recycledBitmap;
                log("Tile decode use inBitmap to decode");
            } else {
                log("Tile decode inBitmap cache is empty");
            }
        } else {
            log("Tile decode can't use inBitmap because of size is not square");
        }

        Rect rect = new Rect(left, top, right, bottom);
        Bitmap bitmap = mBitmapRegionDecoder.decodeRegion(rect, options);
        if (bitmap != null) {
            log("Tile decode success");
            if (recycledBitmap != null) {
                if (bitmap != recycledBitmap) {
                    //直接扔掉
                    recycledBitmap.recycle();
                    log("Tile decode reuse inBitmap failed");
                } else {
                    log("Tile decode reuse inBitmap success");
                }
            }
        } else {
            //有可能图片边缘解析不了,遇到过多次
            //D/skia: --- decoder->decodeRegion returned false
            log("Tile decode failed");
        }
        return bitmap;
    }

    private void addTileToLruCache(long key, Tile tile) {
        if (mCache.get(key) == null) {
            mCache.put(key, tile);
//            log("LruCache 添加 Tile: " + tile.toString());
        } else {
            log("LruCache 已经有这个Tile了,放弃 Tile: " + tile.toString());
            if (tile.bitmap != null) {
                tryToRecycleBitmap(tile.bitmap);
            }
//            throw new IllegalStateException("LruCache 已经有这个Tile了,放弃 Tile: " + tile.toString());//不应该出现的情况,有可能出现的,因为RecyclerView不停拉出屏幕又回来
            //这样Executor有多个创建销毁,但是内部的任务还没结束,于是尴尬了
        }
    }

    private void tryToRecycleBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return;
        }
        if (USE_IN_BITMAP && bitmap.getWidth() == TILE_SIZE && bitmap.getHeight() == TILE_SIZE) {
            synchronized (sRecycledBitmapQueue) {
                if (!sRecycledBitmapQueue.contains(bitmap)) {
                    sRecycledBitmapQueue.add(bitmap);
                    log("recycle Bitmap");
                } else {
                    log("LruCache有可能重复移除同个东西,蛋疼");//错了,是options.inMutable = true + BitmapRegionDecoder的锅,会不同区域返回同个对象,有可能是优化
                }
            }
        } else {
            bitmap.recycle();
        }
    }

    private class AppLruCache extends LruCache<Long, Tile> {
        AppLruCache(int maxSize) {
            super(maxSize);
            log("LruCache init size:" + DimenUtility.bytes2MB(maxSize) + "MB");
        }

        @Override
        protected int sizeOf(Long key, Tile value) {
            return value.bitmap != null ? value.bitmap.getByteCount() : 0;
        }

        @Override
        protected void entryRemoved(boolean evicted, Long key, Tile tile, Tile newValue) {
            super.entryRemoved(evicted, key, tile, newValue);
            log("LruCache entryRemoved");
            if (tile.bitmap != null) {
                tryToRecycleBitmap(tile.bitmap);
            }
        }
    }

    private void log(String log) {
        if (isDebug()) {
            Log.d(TAG, log);
        }
    }
}
