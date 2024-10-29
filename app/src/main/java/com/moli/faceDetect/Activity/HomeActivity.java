package com.moli.faceDetect.Activity;

import static android.widget.Toast.LENGTH_LONG;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
//import android.support.v4.app.ActivityCompat;
//import android.support.v4.widget.SwipeRefreshLayout;
//import android.support.v7.widget.DefaultItemAnimator;
//import android.support.v7.widget.GridLayoutManager;
//import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.moli.faceDetect.R;
import com.moli.faceDetect.common.Constants;
import com.moli.faceDetect.model.CompareResult;
import com.moli.faceDetect.faceserver.FaceServer;
import com.moli.faceDetect.model.ImageResult;
//import com.moli.faceDetect.model.ResultItem;
//import com.moli.faceDetect.fragment.PermissionDegreeDialog;
//import com.moli.faceDetect.fragment.RegisterDialog;
import com.moli.faceDetect.model.DrawInfo;
import com.moli.faceDetect.model.FacePreviewInfo;
import com.moli.faceDetect.storage.HomeFaceDataHelper;
import com.moli.faceDetect.storage.UserFaceDatasHelper;
import com.moli.faceDetect.model.UserFaceInfos;
import com.moli.faceDetect.util.ConfigUtil;
import com.moli.faceDetect.util.DrawHelper;
import com.moli.faceDetect.util.camera.CameraHelper;
import com.moli.faceDetect.util.camera.CameraListener;
import com.moli.faceDetect.util.face.FaceHelper;
import com.moli.faceDetect.util.face.FaceListener;
import com.moli.faceDetect.util.face.LivenessType;
import com.moli.faceDetect.util.face.RecognizeColor;
import com.moli.faceDetect.util.face.RequestFeatureStatus;
import com.moli.faceDetect.util.face.RequestLivenessStatus;
import com.moli.faceDetect.widget.FaceRectView;
import com.moli.faceDetect.widget.FaceSearchResultAdapter;
import com.arcsoft.face.ActiveFileInfo;
import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceFeature;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.VersionInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.scwang.smart.refresh.footer.ClassicsFooter;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnLoadMoreListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeActivity extends BaseActivity implements ViewTreeObserver.OnGlobalLayoutListener {
    private static final String TAG = "HomeActivity";
    int activeCode;
    private static final int MAX_DETECT_NUM = 10;
    /** 当FR成功，活体未成功时，FR等待活体的时间 */
    private static final int WAIT_LIVENESS_INTERVAL = 100;
    /** 失败重试间隔时间（ms）*/
    private static final long FAIL_RETRY_INTERVAL = 1000;
    /** 出错重试最大次数 */
    private static final int MAX_RETRY_TIME = 3;

    private CameraHelper cameraHelper;
    private DrawHelper drawHelper;
    private Camera.Size previewSize;

    /** 优先打开的摄像头，本界面主要用于单目RGB摄像头设备，因此默认打开前置 */
    private Integer rgbCameraID = Camera.CameraInfo.CAMERA_FACING_FRONT;

    /** VIDEO模式人脸检测引擎，用于预览帧人脸追踪 */
    private FaceEngine ftEngine;

    /** 用于特征提取的引擎 */
    private FaceEngine frEngine;

    /** IMAGE模式活体检测引擎，用于预览帧人脸活体检测 */
    private FaceEngine flEngine;

    private int ftInitCode = -1;
    private int frInitCode = -1;
    private int flInitCode = -1;
    private FaceHelper faceHelper;
    private List<CompareResult> compareResultList;
    private FaceSearchResultAdapter adapter;

    /** 活体检测的开关*/
    private boolean livenessDetect = true;

    /** 注册人脸状态码，准备注册 */
    private static final int REGISTER_STATUS_READY = 0;

    /** 注册人脸状态码，注册中 */
    private static final int REGISTER_STATUS_PROCESSING = 1;

    /** 注册人脸状态码，注册结束（无论成功失败）*/
    private static final int REGISTER_STATUS_DONE = 2;

    /** 设置注册状态 */
    private int registerStatus = REGISTER_STATUS_DONE;

    /** 用于记录人脸识别相关状态 */
    private ConcurrentHashMap<Integer, Integer> requestFeatureStatusMap = new ConcurrentHashMap<>();

    /** 用于记录人脸特征提取出错重试次数 */
    private ConcurrentHashMap<Integer, Integer> extractErrorRetryMap = new ConcurrentHashMap<>();

    /** 用于存储活体值 */
    private ConcurrentHashMap<Integer, Integer> livenessMap = new ConcurrentHashMap<>();

    /** 用于存储活体检测出错重试次数 */
    private ConcurrentHashMap<Integer, Integer> livenessErrorRetryMap = new ConcurrentHashMap<>();

    private CompositeDisposable getFeatureDelayedDisposables = new CompositeDisposable();
    private CompositeDisposable delayFaceTaskCompositeDisposable = new CompositeDisposable();

    /** 相机预览显示的控件，可为SurfaceView或TextureView */
    private View previewView;

    /** 绘制人脸框的控件 */
    private FaceRectView faceRectView;

    /** 活体开关 */
    private Switch switchLivenessDetect;

    /** 注册提示框 */
//    RegisterDialog registerDialog;

    /** 无数据显示视图 */
    TextView listTvV;

    /** 权限 */
    private static final int ACTION_REQUEST_PERMISSIONS = 0x001;

    /** 识别阈值 */
    private static final float SIMILAR_THRESHOLD = 0.8F;

    /** 所需的所有权限信息 */
    private static final String[] NEEDED_PERMISSIONS = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE
    };

    /** 注册的用户信息 */
    UserFaceInfos userInfo;

    /** 记录识别通过的标识 */
    String requestStr;

    /** 当前Activity是否在最上层 */
    Boolean isBackground = false;

    /** camera抓的人脸视图byte[]数据 */
    byte[] faceNv21;

    /** 抓的所有人脸信息 */
    List<FacePreviewInfo> facePreviewInfoList = new  ArrayList<FacePreviewInfo>();

    /** 记录识别对比时间 */
    long currentTime = 0;

    /** 查询home本地数据索引 */
    int startIndex = 1;

    /** 分页数据量 */
    int listSize = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        // 保持亮屏
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Activity启动后就锁定为启动时的方向
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WindowManager.LayoutParams attributes = getWindow().getAttributes();
            attributes.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            getWindow().setAttributes(attributes);
        }

        initView();
        /** 激活引擎 */
        activeEngine();
        /** 本地人脸库初始化 */
        FaceServer.getInstance().init(this);
        /** 注册信息数据库初始化 */
        UserFaceDatasHelper.getInstance(HomeActivity.this);
        /** 清除本地存储人脸识别对此数据、清除注册的信息 */
//        clearRegisterFaceData();
        /** 加载本地存储的人脸识别对比数据 */
        loadLocalHomeFaceList();
    }

    /**
     * 加载本地存储的人脸识别对比数据
     */
    private void loadLocalHomeFaceList() {
        startIndex = 1;
        queryAndShowCompareInfoListData(startIndex, null);
    }

    private void queryAndShowCompareInfoListData(final int index, final RefreshLayout refreshLayout) {
        Observable.create(new ObservableOnSubscribe<List<CompareResult>>() {
            @Override
            public void subscribe(ObservableEmitter<List<CompareResult>> emitter) throws Exception {
                List<CompareResult> list = HomeFaceDataHelper.getInstance(HomeActivity.this).queryHomeFacesListData(index);
                emitter.onNext(list);
            }
        }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<CompareResult>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(List<CompareResult> list) {
                        if (startIndex == 1) {
                            if (refreshLayout != null) { refreshLayout.finishRefresh(); }
                        }

                        if (startIndex > 1) {
                            refreshLayout.finishLoadMore();
                        }

                        if (list.size() > 0) {
                            listTvV.setText("");
                            compareResultList.clear();
                            compareResultList.addAll(list);
                            adapter.setResultList(compareResultList);
                            adapter.notifyDataSetChanged();
                        }

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * 清除本地存储人脸识别对此数据、清除注册的信息
     */
    private void clearRegisterFaceData() {
        FaceServer.getInstance().clearAllFaces(this);
        UserFaceDatasHelper.getInstance(this).clearAllRegisterData();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void activeEngine() {
        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
        int res = FaceEngine.getActiveFileInfo(HomeActivity.this, activeFileInfo);
        if (res == ErrorInfo.MOK) {
            Log.i(TAG, activeFileInfo.toString());
            return;
        }
        activeCode = FaceEngine.activeOnline(HomeActivity.this, Constants.APP_ID, Constants.SDK_KEY);
        if (activeCode == ErrorInfo.MOK) {
            showToast(getString(R.string.active_success));
        } else if (activeCode == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            showToast(getString(R.string.already_activated));
        } else {
            showToast(getString(R.string.active_failed, activeCode));
        }
    }


    private void initView() {
        setSubViewsFrame();
        previewView = findViewById(R.id.single_camera_texture_preview);
        previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        faceRectView = findViewById(R.id.single_camera_face_rect_view);
//        switchLivenessDetect = findViewById(R.id.single_camera_switch_liveness_detect);
//        switchLivenessDetect.setChecked(livenessDetect);
//        switchLivenessDetect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                livenessDetect = isChecked;
//            }
//        });
        RefreshLayout refreshLayout = (RefreshLayout)findViewById(R.id.home_face_refresh);
        refreshLayout.setRefreshFooter(new ClassicsFooter(this));
        refreshLayout.setRefreshHeader(new ClassicsHeader(this));
        refreshLayout.setOnRefreshLoadMoreListener(new OnRefreshLoadMoreListener() {
            @Override
            public void onLoadMore(@NonNull RefreshLayout refreshLayout) {
                startIndex += 20;
                queryAndShowCompareInfoListData(startIndex, refreshLayout);
            }

            @Override
            public void onRefresh(@NonNull RefreshLayout refreshLayout) {
                startIndex = 1;
                queryAndShowCompareInfoListData(startIndex, refreshLayout);
            }
        });

        RecyclerView recyclerShowFaceInfo = findViewById(R.id.single_camera_recycler_view_person);
        compareResultList = new ArrayList<>();
        adapter = new FaceSearchResultAdapter(getCompareResultList(), this);
        adapter.setCallback(new FaceSearchResultAdapter.Callback() {
            @Override
            public void onRegister(CompareResult item) {
                // 跳转到注册
                JumpToNextActivity(ImageRegisterActivity.class, item);
            }

            @Override
            public void onDelete(CompareResult item) {
                if (item == null) {
                    return;
                }
                compareResultList.remove(item);
                adapter.setResultList(compareResultList);
                adapter.notifyDataSetChanged();
                HomeFaceDataHelper.getInstance(HomeActivity.this).deleteHomeFaceData(item);
                if (compareResultList.size() < 1) { listTvV.setText(R.string.list_describe); }
            }
        });
        recyclerShowFaceInfo.setAdapter(adapter);
//        DisplayMetrics dm = getResources().getDisplayMetrics();
//        int spanCount = (int) (dm.widthPixels / (getResources().getDisplayMetrics().density * 100 + 0.5f));
        recyclerShowFaceInfo.setLayoutManager(new GridLayoutManager(this,1,GridLayoutManager.VERTICAL,false));
        recyclerShowFaceInfo.setItemAnimator(new DefaultItemAnimator());

        listTvV = findViewById(R.id.face_list_describe);
    }


    /**
     * 设置识别列表的frame
     */
    private void setSubViewsFrame() {
        View leftView = findViewById(R.id.face_left_view);
        View listView = findViewById(R.id.face_list_view);
//        View rightView = findViewById(R.id.face_right_view);
//        View recycleView = findViewById(R.id.single_camera_recycler_view_person);
        View rectView = findViewById(R.id.single_camera_face_rect_view);
//        View preView = findViewById(R.id.single_camera_texture_preview);

        int width = getDisplayMetrics().widthPixels;
        int height = getDisplayMetrics().heightPixels;
        int Vwidth = width/2;
        int Vheight = height;
        layoutViewParas(leftView, Vwidth, height);

        listView.setBackgroundResource(R.drawable.ic_face_background);
        rectView.setBackgroundResource(R.drawable.ic_face_clear_corner_bg);
    }

    /**
     * 调整控件
     * @param view 需要调整的控件
     * @param width 调整的宽度
     * @param height 调整的高度
     */
    private void layoutViewParas(View view, int width, int height) {

        ViewGroup.LayoutParams  params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    /**
     *
     * @return 获取屏幕的宽高
     */
    private DisplayMetrics getDisplayMetrics() {
        // 获取屏幕宽度
        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getSystemService(this.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics;
    }


    private List<CompareResult> getCompareResultList() {
        return compareResultList;
    }

    /**
     * 初始化引擎
     */
    private void initEngine() {
        ftEngine = new FaceEngine();
        ftInitCode = ftEngine.init(this, DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_DETECT);

        frEngine = new FaceEngine();
        frInitCode = frEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_FACE_RECOGNITION);

        flEngine = new FaceEngine();
        flInitCode = flEngine.init(this, DetectMode.ASF_DETECT_MODE_IMAGE, DetectFaceOrientPriority.ASF_OP_ALL_OUT,
                16, MAX_DETECT_NUM, FaceEngine.ASF_LIVENESS);

        Log.i(TAG, "initEngine:  init: " + ftInitCode);

        if (ftInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "ftEngine", ftInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
        if (frInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "frEngine", frInitCode);
            Log.i(TAG, "t: " + error);
            showToast(error);
        }
        if (flInitCode != ErrorInfo.MOK) {
            String error = getString(R.string.specific_engine_init_failed, "flEngine", flInitCode);
            Log.i(TAG, "initEngine: " + error);
            showToast(error);
        }
    }

    /**
     * 销毁引擎，faceHelper中可能会有特征提取耗时操作仍在执行，加锁防止crash
     */
    private void unInitEngine() {
        if (ftInitCode == ErrorInfo.MOK && ftEngine != null) {
            synchronized (ftEngine) {
                int ftUnInitCode = ftEngine.unInit();
                Log.i(TAG, "unInitEngine: " + ftUnInitCode);
            }
        }
        if (frInitCode == ErrorInfo.MOK && frEngine != null) {
            synchronized (frEngine) {
                int frUnInitCode = frEngine.unInit();
                Log.i(TAG, "unInitEngine: " + frUnInitCode);
            }
        }
        if (flInitCode == ErrorInfo.MOK && flEngine != null) {
            synchronized (flEngine) {
                int flUnInitCode = flEngine.unInit();
                Log.i(TAG, "unInitEngine: " + flUnInitCode);
            }
        }
    }

    @Override
    protected void onRestart() {
        if (isBackground) {
            previewView.getViewTreeObserver().addOnGlobalLayoutListener(this);
        }
        if (flEngine == null || frEngine == null || frEngine == null) {
//            if (isBackground) {
//                initView();
//                activeEngine();
//                initEngine();
//                initCamera();
//            }
        }
        isBackground = false;
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        isBackground = true;
//        unInitEngine();
    }

    @Override
    protected void onDestroy() {

        if (cameraHelper != null) {
            cameraHelper.release();
            cameraHelper = null;
        }

        unInitEngine();
        if (getFeatureDelayedDisposables != null) {
            getFeatureDelayedDisposables.clear();
        }
        if (delayFaceTaskCompositeDisposable != null) {
            delayFaceTaskCompositeDisposable.clear();
        }
        if (faceHelper != null) {
            ConfigUtil.setTrackedFaceCount(this, faceHelper.getTrackedFaceCount());
            faceHelper.release();
            faceHelper = null;
        }

        FaceServer.getInstance().unInit();
        super.onDestroy();
    }

    private void initCamera() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        final FaceListener faceListener = new FaceListener() {
            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "onFail: " + e.getMessage());
            }

            //请求FR的回调
            @Override
            public void onFaceFeatureInfoGet(@Nullable final FaceFeature faceFeature, final Integer requestId, final Integer errorCode) {
                //FR成功
                if (faceFeature != null) {
//                    Log.i(TAG, "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId);
                    Integer liveness = livenessMap.get(requestId);
                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测通过，搜索特征
                    else if (liveness != null && liveness == LivenessInfo.ALIVE) {
                        searchFace(faceFeature, requestId);
                    }
                    //活体检测未出结果，或者非活体，延迟执行该函数
                    else {
                        if (requestFeatureStatusMap.containsKey(requestId)) {
                            Observable.timer(WAIT_LIVENESS_INTERVAL, TimeUnit.MILLISECONDS)
                                    .subscribe(new Observer<Long>() {
                                        Disposable disposable;

                                        @Override
                                        public void onSubscribe(Disposable d) {
                                            disposable = d;
                                            getFeatureDelayedDisposables.add(disposable);
                                        }

                                        @Override
                                        public void onNext(Long aLong) {
                                            onFaceFeatureInfoGet(faceFeature, requestId, errorCode);
                                        }

                                        @Override
                                        public void onError(Throwable e) {

                                        }

                                        @Override
                                        public void onComplete() {
                                            getFeatureDelayedDisposables.remove(disposable);
                                        }
                                    });
                        }
                    }

                }
                //特征提取失败
                else {
                    if (increaseAndGetValue(extractErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        extractErrorRetryMap.put(requestId, 0);

                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ExtractCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        // 在尝试最大次数后，特征提取仍然失败，则认为识别未通过
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                        retryRecognizeDelayed(requestId);
                    } else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                    }
                }
            }

            @Override
            public void onFaceLivenessInfoGet(@Nullable LivenessInfo livenessInfo, final Integer requestId, Integer errorCode) {
                if (livenessInfo != null) {
                    int liveness = livenessInfo.getLiveness();
                    livenessMap.put(requestId, liveness);
                    // 非活体，重试
                    if (liveness == LivenessInfo.NOT_ALIVE) {
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_ALIVE"));
                        // 延迟 FAIL_RETRY_INTERVAL 后，将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        retryLivenessDetectDelayed(requestId);
                    }
                } else {
                    if (increaseAndGetValue(livenessErrorRetryMap, requestId) > MAX_RETRY_TIME) {
                        livenessErrorRetryMap.put(requestId, 0);
                        String msg;
                        // 传入的FaceInfo在指定的图像上无法解析人脸，此处使用的是RGB人脸数据，一般是人脸模糊
                        if (errorCode != null && errorCode == ErrorInfo.MERR_FSDK_FACEFEATURE_LOW_CONFIDENCE_LEVEL) {
                            msg = getString(R.string.low_confidence_level);
                        } else {
                            msg = "ProcessCode:" + errorCode;
                        }
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, msg));
                        retryLivenessDetectDelayed(requestId);
                    } else {
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                    }
                }
            }


        };


        CameraListener cameraListener = new CameraListener() {
            @Override
            public void onCameraOpened(Camera camera, int cameraId, int displayOrientation, boolean isMirror) {
                Camera.Size lastPreviewSize = previewSize;
                previewSize = camera.getParameters().getPreviewSize();
                drawHelper = new DrawHelper(previewSize.width, previewSize.height, previewView.getWidth(), previewView.getHeight(), displayOrientation
                        , cameraId, isMirror, false, false);
                Log.i(TAG, "onCameraOpened: " + drawHelper.toString());
                // 切换相机的时候可能会导致预览尺寸发生变化
                if (faceHelper == null ||
                        lastPreviewSize == null ||
                        lastPreviewSize.width != previewSize.width || lastPreviewSize.height != previewSize.height) {
                    Integer trackedFaceCount = null;
                    // 记录切换时的人脸序号
                    if (faceHelper != null) {
                        trackedFaceCount = faceHelper.getTrackedFaceCount();
                        faceHelper.release();
                    }
                    faceHelper = new FaceHelper.Builder()
                            .ftEngine(ftEngine)
                            .frEngine(frEngine)
                            .flEngine(flEngine)
                            .frQueueSize(MAX_DETECT_NUM)
                            .flQueueSize(MAX_DETECT_NUM)
                            .previewSize(previewSize)
                            .faceListener(faceListener)
                            .trackedFaceCount(trackedFaceCount == null ? ConfigUtil.getTrackedFaceCount(HomeActivity.this.getApplicationContext()) : trackedFaceCount)
                            .build();
                }
            }


            @Override
            public void onPreview(final byte[] nv21, Camera camera) {
//                if (isBackground) { return; }
                if (faceRectView != null) {
                    faceRectView.clearFaceInfo();
                }
                faceNv21 = nv21;
                facePreviewInfoList = faceHelper.onPreviewFrame(faceNv21);
                
                if (facePreviewInfoList != null && faceRectView != null && drawHelper != null) {
                    drawPreviewInfo(facePreviewInfoList);
                }
                
//                registerFace(faceNv21, facePreviewInfoList);
//                clearLeftFace(facePreviewInfoList);

                if (facePreviewInfoList != null && facePreviewInfoList.size() > 0 && previewSize != null) {
                    for (int i = 0; i < facePreviewInfoList.size(); i++) {
                        Integer status = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());
                        /**
                         * 在活体检测开启，在人脸识别状态不为成功或人脸活体状态不为处理中（ANALYZING）且不为处理完成（ALIVE、NOT_ALIVE）时重新进行活体检测
                         */
                        if (livenessDetect && (status == null || status != RequestFeatureStatus.SUCCEED)) {
                            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
                            if (liveness == null
                                    || (liveness != LivenessInfo.ALIVE && liveness != LivenessInfo.NOT_ALIVE && liveness != RequestLivenessStatus.ANALYZING)) {
                                if (!isToLinessRequest()) {
//                                    facePreviewInfoList = null;
//                                    clearLeftFace(null);
                                    return;
                                }
                                livenessMap.put(facePreviewInfoList.get(i).getTrackId(), RequestLivenessStatus.ANALYZING);
                                faceHelper.requestFaceLiveness(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId(), LivenessType.RGB);
                            }
                        }
                        /**
                         * 对于每个人脸，若状态为空或者为失败，则请求特征提取（可根据需要添加其他判断以限制特征提取次数），
                         * 特征提取回传的人脸特征结果在{@link FaceListener#onFaceFeatureInfoGet(FaceFeature, Integer, Integer)}中回传
                         */
                        if (status == null
                                || status == RequestFeatureStatus.TO_RETRY) {
                            if (!isToLinessRequest()) {
//                                facePreviewInfoList = null;
//                                clearLeftFace(null);
                                return;
                            }
                            requestFeatureStatusMap.put(facePreviewInfoList.get(i).getTrackId(), RequestFeatureStatus.SEARCHING);
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
//                            Log.i(TAG, "onPreview: fr start = " + System.currentTimeMillis() + " trackId = " + facePreviewInfoList.get(i).getTrackedFaceCount());
                        }

                        // 继续识别成功的人脸
                        if (status != null && status == RequestFeatureStatus.SUCCEED) {
                            if (!isToLinessRequest()) {
//                                facePreviewInfoList = null;
//                                clearLeftFace(null);
                                return;
                            }
                            faceHelper.requestFaceFeature(nv21, facePreviewInfoList.get(i).getFaceInfo(), previewSize.width, previewSize.height, FaceEngine.CP_PAF_NV21, facePreviewInfoList.get(i).getTrackId());
                        }
                    }
                }
            }

            @Override
            public void onCameraClosed() {
                Log.i(TAG, "onCameraClosed: ");
            }

            @Override
            public void onCameraError(Exception e) {
                Log.i(TAG, "onCameraError: " + e.getMessage());
            }

            @Override
            public void onCameraConfigurationChanged(int cameraID, int displayOrientation) {
                if (drawHelper != null) {
                    drawHelper.setCameraDisplayOrientation(displayOrientation);
                }
                Log.i(TAG, "onCameraConfigurationChanged: " + cameraID + "  " + displayOrientation);
            }
        };

        cameraHelper = new CameraHelper.Builder()
                .previewViewSize(new Point(previewView.getMeasuredWidth(), previewView.getMeasuredHeight()))
                .rotation(getWindowManager().getDefaultDisplay().getRotation())
                .specificCameraId(rgbCameraID != null ? rgbCameraID : Camera.CameraInfo.CAMERA_FACING_FRONT)
                .isMirror(false)
                .previewOn(previewView)
                .cameraListener(cameraListener)
                .build();
        cameraHelper.init();
        cameraHelper.start();
    }

    private void registerFace(final byte[] nv21, final List<FacePreviewInfo> facePreviewInfoList) {
        if (registerStatus == REGISTER_STATUS_READY && facePreviewInfoList != null && facePreviewInfoList.size() > 0) {
            registerStatus = REGISTER_STATUS_PROCESSING;
            Observable.create(new ObservableOnSubscribe<Boolean>() {
                        @Override
                        public void subscribe(ObservableEmitter<Boolean> emitter) {

                            boolean success = FaceServer.getInstance().registerNv21(HomeActivity.this, nv21.clone(), previewSize.width, previewSize.height,
                                    facePreviewInfoList.get(0).getFaceInfo(), "registered " + faceHelper.getTrackedFaceCount());
                            int trackId = facePreviewInfoList.get(0).getTrackId();
                            String age = userInfo.getAge();
                            String sex = userInfo.getSex();
                            String name = userInfo.getName();
                            userInfo = new UserFaceInfos(name, age, sex, "registered " + faceHelper.getTrackedFaceCount());
                            emitter.onNext(success);
                        }
                    })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<Boolean>() {
                        @Override
                        public void onSubscribe(Disposable d) {

                        }

                        @Override
                        public void onNext(Boolean success) {
                            String result = success ? "register success!" : "register failed!";
                            showToast(result);
                            registerStatus = REGISTER_STATUS_DONE;

                            // 保存注册的用户信息
                            if (success) {
                                UserFaceDatasHelper.getInstance(HomeActivity.this).registerData(userInfo);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            showToast("register failed!");
                            registerStatus = REGISTER_STATUS_DONE;
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        }
    }

    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        if (facePreviewInfoList == null) {
            return;
        }
        List<DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            String name = faceHelper.getName(facePreviewInfoList.get(i).getTrackId());
            Integer liveness = livenessMap.get(facePreviewInfoList.get(i).getTrackId());
            Integer recognizeStatus = requestFeatureStatusMap.get(facePreviewInfoList.get(i).getTrackId());
//            Integer trackId = facePreviewInfoList.get(i).getTrackId();

            int pAge = 0;
            String pSex;
            String pName = "";

            // 根据识别结果和活体结果设置颜色
            int color = RecognizeColor.COLOR_UNKNOWN;
            if (recognizeStatus != null) {
                if (recognizeStatus == RequestFeatureStatus.FAILED) {
                    color = RecognizeColor.COLOR_FAILED;
                }
                if (recognizeStatus == RequestFeatureStatus.SUCCEED) {
                    color = RecognizeColor.COLOR_SUCCESS;
                }
            }
            if (liveness != null && liveness == LivenessInfo.NOT_ALIVE) {
                color = RecognizeColor.COLOR_FAILED;
            }

            // 获取注册的用户
//            UserFaceInfos info = UserFaceDatasHelper.getInstance(HomeAtivity.this).queryData(this.requestStr, HomeAtivity.this);
//            if (info != null) {
//                pSex = (info.getSex() == "男" || info.getSex() == "MALE") ? "MALE" : "FEMALE";
//                pAge = Integer.parseInt(info.getAge());
//                pName = info.getName() + "\n" + info.getSex() + "\n" + info.getAge();
//
//            }

            drawInfoList.add(new DrawInfo(drawHelper.adjustRect(facePreviewInfoList.get(i).getFaceInfo().getRect()),
                    -1, pAge, liveness == null ? LivenessInfo.UNKNOWN : liveness, color,
                    "" ));
        }
        drawHelper.draw(faceRectView, drawInfoList);
    }

    @Override
    void afterRequestPermission(int requestCode, boolean isAllGranted) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            if (isAllGranted) {
                initEngine();
                initCamera();
            } else {
                showToast(getString(R.string.permission_denied));
            }
        }
    }

    /**
     * 删除已经离开的人脸
     *
     * @param facePreviewInfoList 人脸和trackId列表
     */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
//        if (compareResultList != null) {
//            for (int i = compareResultList.size() - 1; i >= 0; i--) {
//                if (!requestFeatureStatusMap.containsKey(compareResultList.get(i).getTrackId())) {
//                    compareResultList.remove(i);
//                    adapter.notifyItemRemoved(i);
//                }
//            }
//        }
        if (facePreviewInfoList == null || facePreviewInfoList.size() == 0) {
            requestFeatureStatusMap.clear();
            livenessMap.clear();
            livenessErrorRetryMap.clear();
            extractErrorRetryMap.clear();
            if (getFeatureDelayedDisposables != null) {
                getFeatureDelayedDisposables.clear();
            }
            return;
        }
        Enumeration<Integer> keys = requestFeatureStatusMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                requestFeatureStatusMap.remove(key);
                livenessMap.remove(key);
                livenessErrorRetryMap.remove(key);
                extractErrorRetryMap.remove(key);
            }
        }


    }

    private void searchFace(final FaceFeature frFace, final Integer requestId) {

        Observable
                .create(new ObservableOnSubscribe<CompareResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<CompareResult> emitter) {
                        //      Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                        CompareResult compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace);
                        //    Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                        if (compareResult == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.setName(requestId, getString(R.string.recognize_success_notice, ""));
                            searchFaceImage(requestId, null);
                        }else {
                            emitter.onNext(compareResult);
                        }
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CompareResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

//                    public void on

                    @Override
                    public void onNext(CompareResult compareResult) {
//                        Toast.makeText(this , (compareResult.getUserName() == null) ? "" : compareResult.getUserName(), LENGTH_LONG).show();
                        if (compareResult == null || compareResult.getUserName() == null) {
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
                            faceHelper.setName(requestId, "VISITOR " + requestId);
                            return;
                        }

//                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                        if (compareResult.getSimilar() > SIMILAR_THRESHOLD) {
//                            // 人脸识别通过
//                            if (compareResult.getUserName() != null) {
//                                requestStr = compareResult.getUserName();
//                            }

//                            if (!isAdded(requestId)) {
                                //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
//                                if (compareResultList.size() >= MAX_DETECT_NUM) {
//                                    compareResultList.remove(0);
//                                    adapter.notifyItemRemoved(0);
//                                }

//                                listTvV.setText("");

//                                adapter.refreshData(compareResultList);
//                                adapter.notifyItemInserted(0);
//                                compareResult.setTrackId(requestId);
//                                compareResultList.add(compareResult);
//                                adapter.refreshData(compareResultList, adapter);
//                                adapter.notifyDataSetChanged();
//                                adapter.notifyItemInserted(compareResultList.size() - 1);
//                            }
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
                            faceHelper.setName(requestId, getString(R.string.recognize_success_notice, ""));
                            searchFaceImage(requestId, compareResult);

                        } else {
                            CompareResult result = new CompareResult("未识别", requestId);
                            requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED);
//                            faceHelper.setName(requestId, getString(R.string.recognize_success_notice, compareResult.getUserName()));
                            faceHelper.setName(requestId, getString(R.string.recognize_success_notice, ""));
                            searchFaceImage(requestId, result);
//                            faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
//                            retryRecognizeDelayed(requestId);
                        }
                        // 清除屏幕数据
//                        facePreviewInfoList
                    }

                    @Override
                    public void onError(Throwable e) {
                        faceHelper.setName(requestId, getString(R.string.recognize_failed_notice, "NOT_REGISTERED"));
                        retryRecognizeDelayed(requestId);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private boolean isToLinessRequest() {
        long timeT = System.currentTimeMillis();
        if (currentTime == 0) {
            currentTime = timeT;
            return false;
        }
        if (timeT - currentTime > 1500) {
            currentTime = timeT;
            return true;
        }

        return false;
    }

    private boolean isAdded(int requestId) {
        if (compareResultList == null || compareResultList.size() < 1) {
            return false;
        }
        for (CompareResult compareResult1 : compareResultList) {
            if (compareResult1.getTrackId() == requestId) {
                return true;
            }
        }
        return false;
    }

    private void
    searchFaceImage(final Integer requestId, final CompareResult info) {
        Observable.create(new ObservableOnSubscribe<ImageResult>() {
                    @Override
                    public void subscribe(ObservableEmitter<ImageResult> emitter) throws Exception {
                        ImageResult imageResult = FaceServer.getInstance().getImageBitmap(faceNv21, previewSize.width, previewSize.height, facePreviewInfoList.get(0).getFaceInfo());
                        emitter.onNext(imageResult);
                    }
                }).subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ImageResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                    }

                    @Override
                    public void onNext(ImageResult imageResult) {
                        if (imageResult.getBitmap() == null || imageResult.getByteArr() == null) { return; }
                        CompareResult compareResult = new CompareResult("未识别", 0);
                        if (info != null) {
                            compareResult.setUserName(info.getUserName() != null ? info.getUserName() : "");
                            compareResult.setSimilar(info.getSimilar() > 0 ? info.getSimilar() : 0);
                            if (info.getSimilar() > SIMILAR_THRESHOLD) {
                                UserFaceInfos faceInfo = fetchUserFaceInfos(info.getUserName() == null ? "" : info.getUserName());
                                compareResult.setName(faceInfo.getName() != null ? faceInfo.getName() : "未识别");
                                compareResult.setAge(faceInfo.getAge() != null? faceInfo.getAge() : "");
                                compareResult.setSex(faceInfo.getSex() != null? faceInfo.getSex() : "");
                            }
                        }
                        compareResult.setTrackId(requestId);
                        compareResult.setTimeS(System.currentTimeMillis());
                        compareResult.setBitmap(imageResult.getBitmap());
                        compareResult.setImageBytes(imageResult.getByteArr());
                        setFaceItemListData(compareResult);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }


    /**
     * 将准备注册的状态置为{@link #REGISTER_STATUS_READY}
     *
     * @param view 注册按钮
     */
    public void register(View view) {
//        registerDialog();
        // 跳转到本地相册注册
        JumpToNextActivity(ImageRegisterActivity.class, null);
    }

    void JumpToNextActivity(Class activityClass, CompareResult result) {
        if (result == null) {
            startActivity(new Intent(this, activityClass));
        }

        Intent intent = new Intent(this, activityClass);
        String name = (result.getName() == null) ? "" : result.getName();
        long timeS = result.getTimeS() > 0 ? result.getTimeS() : 0;
        String age = result.getAge() == null ? "" : result.getAge();
        String sex = result.getSex() == null ? "" : result.getSex();
        byte[] byteData = result.getImageBytes().length > 0 ? result.getImageBytes() : new byte[]{};

        intent.putExtra("age", age);
        intent.putExtra("sex", sex);
        intent.putExtra("name", name);
        intent.putExtra("currentTime", timeS);
        intent.putExtra("imageBytes", byteData);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == 100) {

            if (compareResultList.size() < 1) { return; }
            Intent intent = new Intent();
            long time = intent.getLongExtra("oldTime", 0L);
            String name = intent.getStringExtra("name");
            String age = intent.getStringExtra("age");
            String sex = intent.getStringExtra("sex");

            List<CompareResult> list = new ArrayList<>();
            for (CompareResult item : compareResultList) {
                if (item.getTimeS() > 0 && time > 0) {
                    if (item.getTimeS() == time) {
                        CompareResult result = item;
                        result.setName(name);
                        result.setAge(age);
                        result.setSex(sex);
                        list.add(result);
                    }else {
                        list.add(item);
                    }
                }
            }

            if (list.size() > 0) {
                compareResultList.clear();
                compareResultList.addAll(list);
                adapter.setResultList(compareResultList);
                adapter.notifyDataSetChanged();
            }
        }
    }

    /**
     * 切换相机。注意：若切换相机发现检测不到人脸，则极有可能是检测角度导致的，需要销毁引擎重新创建或者在设置界面修改配置的检测角度
     *
     * @param view
     */
    public void switchCamera(View view) {
        if (cameraHelper != null) {
            boolean success = cameraHelper.switchCamera();
            if (!success) {
                showToast(getString(R.string.switch_camera_failed));
            } else {
                showLongToast(getString(R.string.notice_change_detect_degree));
            }
        }
    }

    /**
     * 在{@link #previewView}第一次布局完成后，去除该监听，并且进行引擎和相机的初始化
     */
    @Override
    public void onGlobalLayout() {
        previewView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
        if (!checkPermissions(NEEDED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, NEEDED_PERMISSIONS, ACTION_REQUEST_PERMISSIONS);
        } else {
            initEngine();
            initCamera();
        }
    }

    /**
     * 将map中key对应的value增1回传
     *
     * @param countMap map
     * @param key      key
     * @return 增1后的value
     */
    public int increaseAndGetValue(Map<Integer, Integer> countMap, int key) {
        if (countMap == null) {
            return 0;
        }
        Integer value = countMap.get(key);
        if (value == null) {
            value = 0;
        }
        countMap.put(key, ++value);
        return value;
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行活体检测
     *
     * @param requestId 人脸ID
     */
    private void retryLivenessDetectDelayed(final Integer requestId) {
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸状态置为UNKNOWN，帧回调处理时会重新进行活体检测
                        if (livenessDetect) {
                            faceHelper.setName(requestId, Integer.toString(requestId));
                        }
                        livenessMap.put(requestId, LivenessInfo.UNKNOWN);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

    /**
     * 延迟 FAIL_RETRY_INTERVAL 重新进行人脸识别
     *
     * @param requestId 人脸ID
     */
    private void retryRecognizeDelayed(final Integer requestId) {
        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED);
        Observable.timer(FAIL_RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .subscribe(new Observer<Long>() {
                    Disposable disposable;

                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                        delayFaceTaskCompositeDisposable.add(disposable);
                    }

                    @Override
                    public void onNext(Long aLong) {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        // 将该人脸特征提取状态置为FAILED，帧回调处理时会重新进行活体检测
                        faceHelper.setName(requestId, Integer.toString(requestId));
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.TO_RETRY);
                        delayFaceTaskCompositeDisposable.remove(disposable);
                    }
                });
    }

//    public void registerDialog() {
//        if (registerDialog == null) {
//            registerDialog = new RegisterDialog();
//            registerDialog.setCallback(new RegisterDialog.Callback() {
//
//                @Override
//                public void register(boolean isRegister, String name, String age, String sex) {
//                    if (isRegister) {
//                        userInfo = new UserFaceInfos(name, age, sex,"");
//                        if (registerStatus == REGISTER_STATUS_DONE) {
//                            registerStatus = REGISTER_STATUS_READY;
//                        }
//                    }
//                }
//            });
//        }
//        if (registerDialog.isAdded()) {
//            registerDialog.dismiss();
//        }
//        registerDialog.show(getSupportFragmentManager(), PermissionDegreeDialog.class.getSimpleName());
//    }

    private void setFaceItemListData(CompareResult compareResult) {
//        List list = UserFaceDatasHelper.getInstance(this).queryRegisterListData();

        if (compareResult == null) { return; }
//        compareResult.setTimeS(System.currentTimeMillis());
//        compareResult.setBitmap(imageResult.getBitmap());
//            Bitmap bitmap = BitmapFactory.decodeByteArray(faceNv21, 0, faceNv21.length);
//            if (bitmap == null) {return;}
//            CompareResult compareResult1 = new CompareResult("", 0);
//            compareResult1.setTrackId(requestId);
//            compareResult1.setTimeS(System.currentTimeMillis());
//            compareResult1.setBitmap(bitmap);
//            compareResult = compareResult1;
//
//        }else if (compareResult != null && compareResult.getSimilar() > SIMILAR_THRESHOLD) {
//            //添加显示人员时，保存其trackId
//            compareResult.setTrackId(requestId);
//            compareResult.setTimeS(System.currentTimeMillis());
//        }
//        if (!isToAdd()) {return;}

        showToast(String.valueOf (compareResult.getSimilar()));
        if (compareResultList.size() > 0) {
            List<CompareResult> list = new ArrayList<>();
            list.add(compareResult);
            for(CompareResult item : compareResultList){ list.add(item); }
            if (list.size() > 0) {
                compareResultList.clear();
                compareResultList.addAll(list);
                restoreHomeFacesData(compareResult);
            }
            adapter.notifyDataSetChanged();
        }else {
            compareResultList.add(compareResult);
            adapter.notifyDataSetChanged();
            restoreHomeFacesData(compareResult);
        }

        listTvV.setText("");
        // 清除屏幕数据
//        clearLeftFace(null);

    }

    private  void restoreHomeFacesData(CompareResult info) {
        if (info ==null) { return; }

        HomeFaceDataHelper.getInstance(HomeActivity.this).addHomeFaceData(info);
    }

    private UserFaceInfos fetchUserFaceInfos(String userName) {
        UserFaceDatasHelper sHelper = UserFaceDatasHelper.getInstance(HomeActivity.this);
//        String name = this.compareResultList.get(position).getUserName();
        List<UserFaceInfos> list = sHelper.getList();
        UserFaceInfos info = sHelper.queryData(userName, list);
        return info;
    }
}
