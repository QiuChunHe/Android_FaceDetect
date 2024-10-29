package com.moli.faceDetect.Activity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
import android.text.ParcelableSpan;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import com.moli.faceDetect.R;
import com.moli.faceDetect.faceserver.FaceServer;
//import com.moli.faceDetect.faceserver.ResultItem;
import com.moli.faceDetect.model.ResultItem;
import com.moli.faceDetect.storage.UserFaceDatasHelper;
import com.moli.faceDetect.model.UserFaceInfos;
//import com.moli.faceDetect.util.ImageBitmapUtil;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.Face3DAngle;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.FaceSimilar;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.CompareModel;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectModel;
import com.arcsoft.face.model.ArcSoftImageInfo;
import com.arcsoft.face.util.ImageUtils;
import com.arcsoft.imageutil.ArcSoftImageFormat;
import com.arcsoft.imageutil.ArcSoftImageUtil;
import com.arcsoft.imageutil.ArcSoftImageUtilError;
import com.bumptech.glide.Glide;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


public class ImageRegisterActivity extends BaseActivity {
    private static final String TAG = "ImageRegisterActivity";
    private ImageView ivShow;
    private int faceEngineCode = -1;
    /**
     * 请求权限的请求码
     */
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;
    /**
     * 请求选择本地图片文件的请求码
     */
    private static final int ACTION_CHOOSE_IMAGE = 0x201;
//    /**
//     * 提示对话框
//     */
//    private AlertDialog progressDialog;
    /**
     * 被处理的图片
     */
    private Bitmap mBitmap = null;

    /**
     * 所需的所有权限信息
     */
    private static String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.READ_PHONE_STATE
    };

    /**
     * 注册的信息
     */
    private EditText nameE;
    private EditText ageE;
    private Spinner genderSpinner;
//    private EditText sexE;
    String selectedGender;

    /**
     * 上级传来的信息
     */
    private ResultItem item;

    /**
     * 记录识别的时间
     */
    private long time;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_register);
        initView();
        /**
         * 在选择图片的时候，在android 7.0及以上通过FileProvider获取Uri，不需要文件权限
         */
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            List<String> permissionList = new ArrayList<>(Arrays.asList(NEEDED_PERMISSIONS));
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            NEEDED_PERMISSIONS = permissionList.toArray(new String[0]);
        }

        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        }

        assignmentViews();
    }

    private void assignmentViews() {
        Intent intent = this.getIntent();

        if (intent != null && intent.hasExtra("currentTime")) {
            time = intent.getLongExtra("currentTime", 0L);

        }
        if (intent != null && intent.hasExtra("age")) {
            String age = intent.getStringExtra("age");
            ageE.setText(age);
        }
        if (intent != null && intent.hasExtra("sex")) {
            String sex = intent.getStringExtra("sex");
            if (sex != null && sex == getString(R.string.sex_man)) { genderSpinner.setSelection(0); }
            if (sex != null && sex == getString(R.string.sex_female)) { genderSpinner.setSelection(1); }
//            sexE.setText(sex);
        }
        if (intent != null && intent.hasExtra("name")) {
            String name = intent.getStringExtra("name");
            nameE.setText(name);
        }

        if (intent != null && intent.hasExtra("imageBytes")) {
            byte[] imageBytes = intent.getByteArrayExtra("imageBytes");
            if (imageBytes != null && imageBytes.length > 0) {
                mBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                Glide.with(ivShow.getContext())
                        .load(mBitmap)
                        .into(ivShow);
            }
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
        }
        mBitmap = null;
    }

    private void initView() {
        ivShow = findViewById(R.id.iv_head);
        nameE = findViewById(R.id.ed_name);
//        sexE = findViewById(R.id.ed_sex);
        ageE = findViewById(R.id.ed_age);
        genderSpinner = findViewById(R.id.gender_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.gender_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedGender = parent.getItemAtPosition(pos).toString();
//                Toast.makeText(parent.getContext(), "Selected: " + selectedGender, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing here
            }
        });
    }


    /**
     * 追加提示信息
     *
     * @param stringBuilder 提示的字符串的存放对象
     * @param styleSpan     添加的字符串的格式
     * @param strings       字符串数组
     */
    private void addNotificationInfo(SpannableStringBuilder stringBuilder, ParcelableSpan styleSpan, String... strings) {
        if (stringBuilder == null || strings == null || strings.length == 0) {
            return;
        }
        int startLength = stringBuilder.length();
        for (String string : strings) {
            stringBuilder.append(string);
        }
        int endLength = stringBuilder.length();
        if (styleSpan != null) {
            stringBuilder.setSpan(styleSpan, startLength, endLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * 从本地选择文件
     *
     * @param view
     */
    public void chooseLocalImage(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, ACTION_CHOOSE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTION_CHOOSE_IMAGE) {
            if (data == null || data.getData() == null) {
                showToast(getString(R.string.get_picture_failed));
                return;
            }
            try {
                mBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            if (mBitmap == null) {
                showToast(getString(R.string.get_picture_failed));
                return;
            }
            Glide.with(ivShow.getContext())
                    .load(mBitmap)
                    .into(ivShow);
        }
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {

            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    /**
     * 本地图片注册
     *
     * @param view
     */
    public void register(View view) {
        if (!checkInputTextV()) {
            showToast(getString(R.string.please_enter_complete_information));
            return;
        }

        // 时间戳作为唯一标识来记录注册相关信息
        String timeS = String.valueOf(System.currentTimeMillis());
        handleRegister(timeS);
    }

    private boolean checkInputTextV() {
        boolean isInfoComplete = false;
        if (ageE.getText().length() > 0 && ageE.getText().length() > 0 && selectedGender.length() > 0) {
            isInfoComplete = true;
        }else {
            isInfoComplete = false;
        }
        return isInfoComplete;
    }

    private void handleRegister(String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                progressDialog.show();
            }
        });
//        int resourceId = R.mipmap.ic_me;
        Bitmap bitmap = mBitmap;
        bitmap = ArcSoftImageUtil.getAlignedBitmap(bitmap, true);
        byte[] bgr24 = ArcSoftImageUtil.createImageData(bitmap.getWidth(), bitmap.getHeight(), ArcSoftImageFormat.BGR24);
        int transformCode = ArcSoftImageUtil.bitmapToImageData(bitmap, bgr24, ArcSoftImageFormat.BGR24);
        if (transformCode != ArcSoftImageUtilError.CODE_SUCCESS) {
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    progressDialog.dismiss();
//                }
//            });
            return;
        }
        boolean success = FaceServer.getInstance().registerBgr24(ImageRegisterActivity.this,
                bgr24,
                bitmap.getWidth(),
                bitmap.getHeight(),
                s);
        if (success) {
//            progressDialog.dismiss();
            restoreRegisterInfo(s);
            refreshPreActivity(s);
            Toast.makeText(this, "注册成功", Toast.LENGTH_SHORT).show();
            finish();
        }else {
//            progressDialog.dismiss();
            Toast.makeText(this, "注册失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreRegisterInfo(String s) {
        UserFaceInfos userInfo = new UserFaceInfos(String.valueOf(nameE.getText()), String.valueOf(ageE.getText()), selectedGender, s);
        UserFaceDatasHelper.getInstance(ImageRegisterActivity.this).registerData(userInfo);
    }

    private void refreshPreActivity(String s) {
        String age = String.valueOf(ageE.getText());
        String sex = selectedGender;
        String name = String.valueOf(nameE.getText());

        Intent intent = new Intent();
        if (time > 0) {
            intent.putExtra("oldTime", time);
        }
        intent.putExtra("userName", s);
        intent.putExtra("age", age);
        intent.putExtra("sex", sex);
        intent.putExtra("name", name);
        setResult(RESULT_OK,intent);
    }
}
