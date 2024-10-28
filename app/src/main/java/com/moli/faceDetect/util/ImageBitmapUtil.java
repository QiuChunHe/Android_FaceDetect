package com.moli.faceDetect.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;

public class ImageBitmapUtil {

     public byte[] bitmapToBytes(Bitmap bmp) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitmapByte = bos.toByteArray();
        return bitmapByte;
    }

    public Bitmap bytesToBitmap(byte[] bos) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(bos, 0, bos.length);
        return  bitmap;
    }

}
