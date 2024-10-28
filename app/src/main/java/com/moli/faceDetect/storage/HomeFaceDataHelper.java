package com.moli.faceDetect.storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.moli.faceDetect.model.CompareResult;

import java.util.ArrayList;
import java.util.List;

public class HomeFaceDataHelper extends SQLiteOpenHelper {
    private static HomeFaceDataHelper sHelper;
    private static final String DB_NAME = "homeFaceData.db";
    private static final int DB_VERSION = 1;
    private static List<CompareResult> list;
    private static int startIndex = 1;
    private static List<String> timeList;

    public HomeFaceDataHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    //创建单例，供使用调用该类里面的的增删改查的方法
    public synchronized static HomeFaceDataHelper getInstance(Context context) {
        if (null == sHelper) {
            list = new ArrayList<>();
            timeList = new ArrayList<>();
            sHelper = new HomeFaceDataHelper(context);
        }
        return sHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS homeFaces (id INTEGER PRIMARY KEY AUTOINCREMENT, userName TEXT, similar REAL, trackId INTEGER, timeS INTEGER, imageBytes BLOB, name TEXT, age TEXT, sex TEXT)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS homeFaces";
        db.execSQL(sql);
        onCreate(db);
    }

    // 清空表所有数据
    public void clearAllRegisterData() {
        SQLiteDatabase db = sHelper.getWritableDatabase();
        db.execSQL("DELETE FROM homeFaces");
    }

    // 添加数据
    public void addHomeFaceData(CompareResult info) {
        SQLiteDatabase db = sHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        if (info.getName() != null) {
            values.put("name", info.getName());
        }

        if (info.getAge() != null) {
            values.put("age", info.getAge());
        }

        if (info.getSex() != null) {
            values.put("sex", info.getSex());
        }

        if (info.getUserName() != null) {
            values.put("userName", info.getUserName());
        }

        if (info.getTimeS() > 0) {
            values.put("timeS", info.getTimeS());
        }

        if (info.getImageBytes() != null) {
            values.put("imageBytes", info.getImageBytes());
        }

        if (info.getSimilar() > 0) {
            values.put("similar", info.getSimilar());
        }

        if (info.getTrackId() > 0) {
            values.put("trackId", info.getTrackId());
        }

        long id = db.insert("homeFaces", null, values);
        if (id > 0) {
            List<CompareResult> newList = new ArrayList<>();
            List<CompareResult> arr = sHelper.getList();
            if (!timeList.contains(String.valueOf(info.getTimeS()))) {
                timeList.add(String.valueOf(info.getTimeS()));
            }
            newList.add(info);
            for (CompareResult item : arr) {
                newList.add(item);
            }
            list = newList;

        }

        db.close();
    }

    // 删除数据
    public void deleteHomeFaceData(CompareResult info) {
        if (info == null) { return; }

        long timeS =  info.getTimeS();
        boolean isExist = false;
        List<CompareResult> arr = sHelper.getList();
        for (CompareResult item : arr) {
            long time = item.getTimeS();
            if (time == timeS) {
                isExist = true;
            }
        }

        if (!isExist) { return; }
        String whereClause = "timeS = " + timeS;
//        String[] whereArgs = new String[]{String.valueOf(timeS)};
        SQLiteDatabase db = sHelper.getWritableDatabase();
        int delete = db.delete("homeFaces", whereClause, null);
        if (delete > 0) {
            if (list.contains(info)) {
                list.remove(info);
            }
            if (timeList.contains(String.valueOf(info.getTimeS()))) {
                timeList.remove(String.valueOf(info.getTimeS()));
            }
        }
        db.close();
    }

    public static List<CompareResult> getList() {
        if (list != null && list.size() > 0) {
            return  list;
        }else {
            list = sHelper.queryHomeFacesListData(startIndex);
        }
        return  list;
    }

    // 查询homeFaces数据库里的所有数据

    /**
     *
     * @param index 分页查询开始位置
     * @return  CompareResult的数组
     */
    public List<CompareResult> queryHomeFacesListData(int index) {
        SQLiteDatabase db = sHelper.getReadableDatabase();
        if (index == 1 || list == null || list.size() < 1) {
            list = new ArrayList<>();
            timeList = new ArrayList<>();
        }
//        startIndex = index;
        String orderStr = "timeS DESC LIMIT " + String.valueOf(index) + ",20";

        String[] projection = {"id", "userName", "similar", "trackId", "name", "age", "sex", "timeS", "imageBytes"};
        Cursor cursor = db.query("homeFaces", projection, null, null, null, null, "timeS DESC LIMIT 1,20");

        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
            String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
            String age = cursor.getString(cursor.getColumnIndexOrThrow("age"));
            String sex = cursor.getString(cursor.getColumnIndexOrThrow("sex"));
            String userName = cursor.getString(cursor.getColumnIndexOrThrow("userName"));
            float similar = cursor.getFloat(cursor.getColumnIndexOrThrow("similar"));
            int trackId = cursor.getInt(cursor.getColumnIndexOrThrow("trackId"));
            long timeS = cursor.getLong(cursor.getColumnIndexOrThrow("timeS"));
            byte[] imageBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("imageBytes"));

            // 处理查询结果
            CompareResult info = new CompareResult(userName, similar);
            info.setTrackId(trackId);
            info.setName(name);
            info.setAge(age);
            info.setSex(sex);
            info.setTimeS(timeS);
            info.setImageBytes(imageBytes);
            if (imageBytes != null && imageBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                info.setBitmap(bitmap);
            }
            if (!timeList.contains(String.valueOf(timeS))){
                timeList.add(String.valueOf(timeS));
                list.add(info);
            }
        }

        cursor.close();
        db.close();
        return list;
    }
}
