package com.moli.faceDetect.model;

import android.content.ContentValues;
import android.content.Context;

public class UserFaceInfos {
    private String name;
    private String age;
    private String sex;
    private String registered;

    public UserFaceInfos(String name, String age, String sex, String registered) {
        this.name = name;
        this.age = age;
        this.sex = sex;
        this.registered = registered;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return this.age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return this.sex;
    }

    public void setSex(String sex) { this.sex = sex; }

    public String getRegistered() {
        return this.registered;
    }

    public void setRegistered(String registered) { this.registered = registered; }

}
