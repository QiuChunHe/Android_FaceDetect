package com.moli.faceDetect.model;


import android.graphics.Bitmap;

public class CompareResult {
    private String userName;
    private float similar;
    private int trackId;
    private long timeS;
    private Bitmap bitmap;
    private byte[] imageBytes;
    private String age;
    private String sex;
    private String name;

    public CompareResult(String userName, float similar) {
        this.userName = userName;
        this.similar = similar;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public float getSimilar() {
        return similar;
    }

    public void setSimilar(float similar) {
        this.similar = similar;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    public long getTimeS() {
        return timeS;
    }

    public void setTimeS(long timeS) {
        this.timeS = timeS;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getAge() {
        return age;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getSex() {
        return sex;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
