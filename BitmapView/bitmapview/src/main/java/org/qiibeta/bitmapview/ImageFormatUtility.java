package org.qiibeta.bitmapview;


import android.graphics.BitmapFactory;

class ImageFormatUtility {
    static boolean isGif(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return "image/gif".equals(options.outMimeType);
    }
}
