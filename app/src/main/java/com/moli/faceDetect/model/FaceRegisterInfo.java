package com.moli.faceDetect.model;

public class FaceRegisterInfo {
    private byte[] featureData;
    private String name;
//    private Integer age;
//    private Integer sex;

    public FaceRegisterInfo(byte[] featureData, String name) {
        this.featureData = featureData;
        this.name = name;
//        this.age = age;
//        this.sex = sex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public Integer getAge() {
//        return age;
//    }
//
//    public void setAge(Integer age) {
//        this.age = age;
//    }
//
//    public Integer getSex() {
//        return sex;
//    }
//
//    public void setSex(Integer sex) {
//        this.sex = sex;
//    }
//
    public byte[] getFeatureData() { return featureData;}

    public void setFeatureData(byte[] featureData) {
        this.featureData = featureData;
    }
}
