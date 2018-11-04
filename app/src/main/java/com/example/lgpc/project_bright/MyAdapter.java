package com.example.lgpc.project_bright;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created by LGPC on 2018-10-18.
 */

public class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView ivPicture;
        TextView tvFloor;
        ImageView imageDeleteFloor = itemView.findViewById(R.id.delete_floor);
        Button imageAddBt;

        MyViewHolder(View view) {
            super(view);
            ivPicture = view.findViewById(R.id.floor_image);
            tvFloor = view.findViewById(R.id.floor_info);
            imageAddBt = view.findViewById(R.id.picture_update);

            imageDeleteFloor.setOnClickListener(this);
            imageAddBt.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (view == imageDeleteFloor) {
                delete(getAdapterPosition());
            }

            if (view == imageAddBt) {
                ((BuildingActivity)BuildingActivity.mContext).ClickAddPicBt(getAdapterPosition());
            }
        }

        void delete(int position) {
            floorInfoArrayList.remove(position);
            notifyItemRemoved(position);

            for (int i = 0; i < getItemCount(); i++) {
                floorInfoArrayList.get(i).setIndex(i);
            }
            notifyDataSetChanged();
        }
    }

    private ArrayList<floor_info> floorInfoArrayList;
    MyAdapter(ArrayList<floor_info> floorInfoArrayList) {
        this.floorInfoArrayList = floorInfoArrayList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.building_info, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        MyViewHolder myViewHolder = (MyViewHolder) holder;
        myViewHolder.ivPicture.setImageBitmap(floorInfoArrayList.get(position).drawableId);
        myViewHolder.tvFloor.setText(floorInfoArrayList.get(position).floorId);
    }

    @Override
    public int getItemCount() {
        return floorInfoArrayList.size();
    }
}
