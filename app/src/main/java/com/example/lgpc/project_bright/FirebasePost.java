package com.example.lgpc.project_bright;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import org.w3c.dom.Node;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LGPC on 2018-10-19.
 */

@IgnoreExtraProperties
public class FirebasePost {
    public String userID;
    public String buildingName;
    public int floorNumber;
    public double building_x;
    public double building_y;
    public String token;
    public String email;

    public FirebasePost() {

    }

    public FirebasePost(String userID, String buildingName, int floorNumber, double building_x, double building_y, String token, String email) {
        this.userID = userID;
        this.buildingName = buildingName;
        this.floorNumber = floorNumber;
        this.building_x = building_x;
        this.building_y = building_y;
        this.token = token;
        this.email = email;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("userID", userID);
        result.put("buildingName", buildingName);
        result.put("floorNumber", floorNumber);
        result.put("building_x", building_x);
        result.put("building_y", building_y);
        result.put("token", token);
        result.put("email", email);

        return result;
    }

    public String getToken() {
        return token;
    }
}
