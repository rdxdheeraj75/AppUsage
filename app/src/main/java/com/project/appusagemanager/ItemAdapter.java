package com.project.appusagemanager;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ViewHolder> {

    List<AppUsage> list;
    Context context;
    PackageManager packageManager;
    public ItemAdapter(Context context, List<AppUsage> appList) {
        list=new ArrayList<>();
        list=appList;
        this.context=context;
        packageManager=context.getPackageManager();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView iv;
        TextView tvName, tvTime;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName=itemView.findViewById(R.id.tvName);
            tvTime=itemView.findViewById(R.id.tvTime);
            iv=itemView.findViewById(R.id.imageView);
        }
    }

    @NonNull
    @Override
    public ItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_sample,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemAdapter.ViewHolder holder, int position) {

        try {
            holder.tvName.setText(packageManager.getApplicationLabel(packageManager.getApplicationInfo(list.get(position).getName(),PackageManager.GET_META_DATA)));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        holder.tvTime.setText(Integer.parseInt(list.get(position).getTiming())/60000+" Min");
        try {
            holder.iv.setImageDrawable(context.getPackageManager().getApplicationIcon(list.get(position).getName()));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
