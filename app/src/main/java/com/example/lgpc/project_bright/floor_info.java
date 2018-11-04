package com.example.lgpc.project_bright;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;

/**
 * Created by LGPC on 2018-10-18.
 */

public class floor_info {
    public String floorId;
    public Bitmap drawableId = null;

    public floor_info (String floorId) {
        this.floorId = floorId;
    }

    public void setIndex (int index) {
        this.floorId = Integer.toString(index + 1) + "층";
    }
    public void setPic (Bitmap bitmap) { this.drawableId = bitmap; }
}
