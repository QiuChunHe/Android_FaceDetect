package com.moli.faceDetect.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.moli.faceDetect.model.UserFaceInfos;
import java.util.ArrayList;
import java.util.List;

public class UserFaceDatasHelper extends SQLiteOpenHelper {
    private static UserFaceDatasHelper sHelper;
    private static final String DB_NAME = "myFaceData.db";
    private static final int DB_VERSION = 1;
    List<UserFaceInfos> list;

    public UserFaceDatasHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    //创建单例，供使用调用该类里面的的增删改查的方法
    public synchronized static UserFaceDatasHelper getInstance(Context context) {
        if (null == sHelper) {
            sHelper = new UserFaceDatasHelper(context);
        }
        return sHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS faces (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, age TEXT, sex TEXT, registered TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS faces";
        db.execSQL(sql);
        onCreate(db);
    }

    // 清空表所有数据
    public void clearAllRegisterData() {
        SQLiteDatabase db = sHelper.getWritableDatabase();
        db.execSQL("DELETE FROM faces");
    }

    // 注册
    public void registerData(UserFaceInfos info) {
        SQLiteDatabase db = sHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("name", info.getName());
        values.put("age", info.getAge());
        values.put("sex", info.getSex());
        values.put("registered", info.getRegistered());

        long id = db.insert("faces", null, values);

        db.close();
    }

    // 查询所有注册用户数据
    public List<UserFaceInfos> queryRegisterListData() {
        SQLiteDatabase db = sHelper.getReadableDatabase();
        list = new ArrayList<>();

        String[] projection = {"id", "name", "age", "sex", "registered"};
        Cursor cursor = db.query("faces", projection, null, null, null, null, null);

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String age = cursor.getString(cursor.getColumnIndexOrThrow("age"));
            String sex = cursor.getString(cursor.getColumnIndexOrThrow("sex"));
            String registered = cursor.getString(cursor.getColumnIndexOrThrow("registered"));
//            byte[] faceFeature = cursor.getBlob(cursor.getColumnIndexOrThrow("faceFeature"));

            // 处理查询结果
            UserFaceInfos info = new UserFaceInfos(name, age, sex, registered);
            list.add(info);
        }

        cursor.close();
        db.close();
        return list;
    }

    public List<UserFaceInfos> getList() {
        if (list != null && list.size() > 0) {
            return list;
        }else {
            list = queryRegisterListData();
        }
        return list;
    }

    public UserFaceInfos queryData(String registered, List<UserFaceInfos> arr) {
//        List<UserFaceInfos> arr = sHelper.getList();
        if (registered == null || arr == null || arr.size() < 0) {
            return null;
        }
        UserFaceInfos info = null;
        for (UserFaceInfos infoI : arr) {
            if (registered.equals(infoI.getRegistered())) {
                info = infoI;
                return info;
            }
        }
        return info;
    }

}
