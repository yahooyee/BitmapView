package org.qiibeta.bitmapview;


import android.graphics.BitmapFactory;

import java.io.InputStream;

class ImageFormatUtility {
    static boolean isGif(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        return "image/gif".equals(options.outMimeType);
    }

    static boolean isGif(InputStream inputStream) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, options);
        return "image/gif".equals(options.outMimeType);
    }
}
