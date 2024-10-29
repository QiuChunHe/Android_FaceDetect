package com.moli.faceDetect.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
//import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.moli.faceDetect.R;
import com.moli.faceDetect.model.CompareResult;
import com.moli.faceDetect.faceserver.FaceServer;
//import com.arcsoft.arcfacedemo.fragment.PermissionDegreeDialog;
import com.moli.faceDetect.storage.UserFaceDatasHelper;
import com.moli.faceDetect.model.UserFaceInfos;
import com.bumptech.glide.Glide;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;

public class FaceSearchResultAdapter extends RecyclerView.Adapter<FaceSearchResultAdapter.CompareResultHolder> {
    private List<CompareResult> compareResultList;
    private LayoutInflater inflater;
    private Context context;

    private Callback callback;

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setResultList(List<CompareResult> compareResultList) {
        this.compareResultList = compareResultList;
    }

    public FaceSearchResultAdapter(List<CompareResult> compareResultList, Context context) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.compareResultList = compareResultList;
    }

    // 刷新数据集的方法
//    public void refreshData(List<CompareResult> list, FaceSearchResultAdapter adapter) {
//        adapter.compareResultList.clear();
//        if (list != null) {
//            adapter.compareResultList.addAll(list);
//        }
//        adapter.notifyDataSetChanged(); // 通知适配器数据集已更改
//    }

    private UserFaceInfos fetchUserFaceInfos(Context context, int position) {
        UserFaceDatasHelper sHelper = UserFaceDatasHelper.getInstance(this.context);
        String name = this.compareResultList.get(position).getUserName();
        List<UserFaceInfos> list = sHelper.getList();
        UserFaceInfos info = sHelper.queryData(name, list);
        return info;
    }

    //

    @NonNull
    @Override
    public CompareResultHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.recycler_item_search_result, null, false);
        CompareResultHolder compareResultHolder = new CompareResultHolder(itemView);
        compareResultHolder.nameText = itemView.findViewById(R.id.tv_item_name);
        compareResultHolder.ageText = itemView.findViewById(R.id.tv_item_age);
        compareResultHolder.sexText = itemView.findViewById(R.id.tv_item_sex);
        compareResultHolder.timeText = itemView.findViewById(R.id.tv_item_time);
        compareResultHolder.imageView = itemView.findViewById(R.id.iv_item_head_img);
        compareResultHolder.registerBtn = itemView.findViewById(R.id.face_list_register);
        compareResultHolder.deleteBtn = itemView.findViewById(R.id.face_list_delete);
        return compareResultHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull CompareResultHolder holder, int position) {
        if (this.compareResultList == null) {
            return;
        }
        CompareResult item = this.compareResultList.get(position);
        if (item != null) {

//        if (compareResultList.get(position).getUserName() == "") {
            Bitmap bitmap = item.getBitmap();
            if (bitmap != null) {
                Glide.with(holder.imageView.getContext())
                        .load(bitmap)
                        .into(holder.imageView);
            }
//        }else {
//            File imgFile = new File(FaceServer.ROOT_PATH + File.separator + FaceServer.SAVE_IMG_DIR + File.separator + compareResultList.get(position).getUserName() + FaceServer.IMG_SUFFIX);
//            Glide.with(holder.imageView.getContext())
//                    .load(imgFile)
//                    .into(holder.imageView);
//        }

//            UserFaceInfos info = fetchUserFaceInfos(this.context, position);
            holder.registerBtn.setTag(position);
            holder.deleteBtn.setTag(position);
            holder.registerBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callback != null) {
                        CompareResult item = compareResultList.get((Integer) v.getTag());
                        callback.onRegister(item);
                    }
                }
            });

            holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (callback != null) {
                        CompareResult item = compareResultList.get((Integer) v.getTag());
                        callback.onDelete(item);
                    }
                }
            });

//        if (info != null) {
//            holder.nameText.setText(info.getName());
//            holder.sexText.setText(info.getSex());
//            holder.ageText.setText(info.getAge());
//        }else {
//            holder.nameText.setText("未识别");
//            holder.sexText.setText("");
//            holder.ageText.setText("");
//        }
            holder.nameText.setText(item.getName() == null ? "未识别" : item.getName());
            holder.sexText.setText(item.getSex() == null ? "" : item.getSex());
            holder.ageText.setText(item.getAge() == null ? "" : item.getAge());
            holder.timeText.setText(item.getTimeS() > 0 ? fecthTimeS(item.getTimeS()) : "");
        }

    }

    private String fecthTimeS(long c) {
        // 创建一个日期格式化对象，你可以根据需要调整格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        // 使用日期格式化对象将时间戳格式化为字符串
        String formattedDate = sdf.format(new Date(c));
        return formattedDate;
    }

    @Override
    public int getItemCount() {
        return this.compareResultList == null ? 0 : this.compareResultList.size();
    }

    class CompareResultHolder extends RecyclerView.ViewHolder {

        TextView nameText;
        TextView sexText;
        TextView ageText;
        TextView timeText;
        ImageView imageView;
        Button registerBtn;
        Button deleteBtn;

        CompareResultHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface Callback {

        void onRegister(CompareResult item);

        void onDelete(CompareResult item);
    }
}
