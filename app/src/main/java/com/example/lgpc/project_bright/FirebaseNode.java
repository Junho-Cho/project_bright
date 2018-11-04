package com.example.lgpc.project_bright;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by LGPC on 2018-10-22.
 */

@IgnoreExtraProperties
public class FirebaseNode {
    //public String userID;
    public ArrayList<NodeInfo> nodes;

    public FirebaseNode() {

    }

    public FirebaseNode(ArrayList<NodeInfo> nodes) {
        //this.userID = userID;
        this.nodes = nodes;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        //result.put("userID", userID);
        result.put("nodes", nodes);

        return result;
    }
}
