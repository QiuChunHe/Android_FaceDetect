package com.moli.faceDetect.model;

import android.graphics.Bitmap;
//import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ResultItem implements Serializable {
//    private CompareResult compareResult;

    public String userName;
    public float similar;
    public int trackId;
    public long timeS;
    public byte[] imageBytes;
    public int age;
    public int sex;

}
