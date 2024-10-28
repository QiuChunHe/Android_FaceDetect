package com.moli.faceDetect.model;

import android.graphics.Bitmap;

public class ImageResult {
    private Bitmap bitmap;
    private byte[] byteArr;
    private boolean success;

    public ImageResult(Bitmap bitmap, byte[] byteArr, boolean success) {
        this.bitmap = bitmap;
        this.byteArr = byteArr;
        this.success = success;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public boolean getSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public byte[] getByteArr() { return byteArr; }

    public void setByteArr(byte[] byteArr) { this.byteArr = byteArr; }
}
