package com.example.lgpc.project_bright;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.Image;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class UserActivity extends AppCompatActivity implements View.OnClickListener {
    public static Context mUserContext;

    ImageView imageOpenMenu;
    ImageView backgroundMap;
    ImageView thisBuildingInfo;
    ImageView reportIcon;
    RecyclerView userRecyclerView;
    RecyclerView.LayoutManager userLayoutManager;
    TextView outputNodeName;
    TextView thisBuildingName;
    TextView thisBuildingDist;

    int currentFloor;

    private DatabaseReference databaseReference;
    final FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef;

    ArrayList<floor_info> floorInfoArrayList = new ArrayList<>();
    MyAdapter mUserAdapter = new MyAdapter(floorInfoArrayList);

    String closeBuildingAdmin, closeBuildingName;
    int closeBuildingFloor;

    double minDist = 1000000.0;
    int minOrd;
    String sort = "userID";

    HashMap<String, Button> nodesSomeFloor = new HashMap<>();

    // GPS
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    private GpsInfo gps;
    double latitude, longitude;
    List<Address> list = null;
    String address;

    ArrayList<MenuInfo> menuInfoArrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_user);

        mUserContext = this;

        imageOpenMenu = (ImageView) findViewById(R.id.menu);
        backgroundMap = (ImageView) findViewById(R.id.bg_map);
        thisBuildingInfo = (ImageView) findViewById(R.id.building_info);
        reportIcon = (ImageView) findViewById(R.id.report);
        userRecyclerView = findViewById(R.id.user_recycler);
        userRecyclerView.setHasFixedSize(true);
        userLayoutManager = new LinearLayoutManager(this);
        userRecyclerView.setLayoutManager(userLayoutManager);

        imageOpenMenu.setOnClickListener(this);
        thisBuildingInfo.setOnClickListener(this);
        reportIcon.setOnClickListener(this);

        menuInfoArrayList.add(new MenuInfo("메인으로"));

        UserAdapter myUserAdapter = new UserAdapter(menuInfoArrayList);
        userRecyclerView.setAdapter(myUserAdapter);

        currentFloor = 1;

        setUserGps();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_ACCESS_FINE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAccessFineLocation = true;
        } else if (requestCode == PERMISSIONS_ACCESS_COARSE_LOCATION && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            isAccessCoarseLocation = true;
        }
        if (isAccessFineLocation && isAccessCoarseLocation) {
            isPermission = true;
        }
    }

    private void callPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_ACCESS_FINE_LOCATION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_ACCESS_COARSE_LOCATION);
        } else {
            isPermission = true;
        }
    }

    public void setUserGps() {
        gps = new GpsInfo(UserActivity.this);
        if (gps.isGetLocation()) {
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
        } else {
            gps.showSettingAlert();
        }

        Toast.makeText(mUserContext, "가까운 건물을 탐색 중입니다...", Toast.LENGTH_SHORT).show();

        final Geocoder geocoder = new Geocoder(this);
        try {
            list = geocoder.getFromLocation(latitude, longitude, 1);
            if (list != null && list.size() > 0) {
                address = list.get(0).getAddressLine(0).toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int ord = 0;
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    FirebasePost get = postSnapshot.getValue(FirebasePost.class);

                    String[] info = {get.userID, get.buildingName, String.valueOf(get.floorNumber)};
                    double[] GpsInfo = {get.building_x, get.building_y};

                    Location locationA = new Location("UserPoint");
                    locationA.setLatitude(latitude);
                    locationA.setLongitude(longitude);
                    Location locationB = new Location("BuildingPoint");
                    locationB.setLatitude(GpsInfo[0]);
                    locationB.setLongitude(GpsInfo[1]);

                    if (locationA.distanceTo(locationB) < minDist) {
                        minDist = locationA.distanceTo(locationB);
                        minOrd = ord;
                        closeBuildingAdmin = info[0]; closeBuildingName = info[1]; closeBuildingFloor = Integer.parseInt(info[2]);
                    }
                    ord++;
                }

                for (int i = 0; i < closeBuildingFloor; i++) {
                    menuInfoArrayList.add(new MenuInfo((i + 1) + "층"));
                }

                userRecyclerView.setVisibility(userRecyclerView.GONE);

                setFirstFloor();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        Query sortbyAge = FirebaseDatabase.getInstance().getReference().child("user_list").orderByChild(sort);
        sortbyAge.addListenerForSingleValueEvent(postListener);
    }

    public void setFirstFloor() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("해당 층의 정보를 불러오는 중입니다...");
        progressDialog.show();

        storageRef = storage.getReferenceFromUrl("gs://bright-project-7fbc2.appspot.com").child(closeBuildingAdmin + "/" + "1층.png");

        storageRef.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        backgroundMap.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(UserActivity.this, "해당 층 표시에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });

        getFirebaseNode();
    }

    public void setOtherFloor(String menuStr) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("해당 층의 정보를 불러오는 중입니다...");
        progressDialog.show();

        storageRef = storage.getReferenceFromUrl("gs://bright-project-7fbc2.appspot.com").child(closeBuildingAdmin + "/" + menuStr + ".png");

        storageRef.getBytes(1024 * 1024)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        backgroundMap.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                        progressDialog.dismiss();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(UserActivity.this, "해당 층 표시에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });

        userRecyclerView.setVisibility(userRecyclerView.GONE);

        getFirebaseNode();
    }

    public void menuSelect(String menuStr) {
        if (menuStr.equals("메인으로")) {
            finish();
        } else {
            String tmp = menuStr.substring(0, menuStr.length() - 1);
            currentFloor = Integer.parseInt(tmp);
            setOtherFloor(menuStr);

            Set<String> k = nodesSomeFloor.keySet();
            for (Iterator<String> it = k.iterator(); it.hasNext();) {
                String st = it.next();
                Button bt = nodesSomeFloor.get(st);
                ((ViewManager)bt.getParent()).removeView(bt);
            }
            nodesSomeFloor.clear();
        }
    }

    public void getFirebaseNode() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    if (key.equals(closeBuildingAdmin)) {
                        final FirebaseNode g = postSnapshot.getValue(FirebaseNode.class);
                        for (int i = 0; i < g.nodes.size(); i++) {
                            if (g.nodes.get(i).floor == currentFloor) {
                                int boolToInt = g.nodes.get(i).state ? 1 : 0;
                                final String infoName = g.nodes.get(i).name;
                                int[] info = {g.nodes.get(i).x, g.nodes.get(i).y, g.nodes.get(i).floor, boolToInt};
                                boolean tmp = (info[3] == 1);

                                RelativeLayout.LayoutParams lp;
                                Button newNode = new Button(mUserContext);

                                lp = new RelativeLayout.LayoutParams(120, 120);
                                lp.leftMargin = info[0] - 60;
                                lp.topMargin = info[1] - 60;
                                newNode.setLayoutParams(lp);
                                newNode.setAlpha(0.75f);
                                newNode.setBackgroundResource(R.drawable.node_green);

                                addContentView(newNode, lp);
                                nodesSomeFloor.put(infoName, newNode);

                                newNode.setOnClickListener(new Button.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        AlertDialog.Builder outputBuilder = new AlertDialog.Builder(UserActivity.this);
                                        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                                        View view = inflater.inflate(R.layout.output_node_info, null);
                                        outputNodeName = (TextView) view.findViewById(R.id.this_node_name);
                                        outputNodeName.setText(infoName);
                                        outputBuilder.setView(view);
                                        outputBuilder.setPositiveButton("확인", null);

                                        outputBuilder.create();
                                        outputBuilder.show();
                                    }
                                });
                            }
                        }

                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        Query sortbyAge = FirebaseDatabase.getInstance().getReference().child("node_list").orderByChild(sort);
        sortbyAge.addListenerForSingleValueEvent(postListener);
    }

    @Override
    public void onClick(View view) {
        if (view == imageOpenMenu) {
            if (userRecyclerView.getVisibility() == userRecyclerView.GONE) {
                userRecyclerView.setVisibility(userRecyclerView.VISIBLE);
            } else {
                userRecyclerView.setVisibility(userRecyclerView.GONE);
            }
        }
        if (view == thisBuildingInfo) {
            AlertDialog.Builder infoBuilder = new AlertDialog.Builder(UserActivity.this);
            LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
            View v = inflater.inflate(R.layout.information, null);
            thisBuildingName = (TextView) v.findViewById(R.id.this_building_name);
            thisBuildingDist = (TextView) v.findViewById(R.id.this_building_dist);
            thisBuildingName.setText(closeBuildingName);
            thisBuildingDist.setText(String.valueOf(Math.round(minDist)));
            infoBuilder.setView(v);
            infoBuilder.setPositiveButton("확인", null);

            infoBuilder.create();
            infoBuilder.show();
        }
        if (view == reportIcon) {
            Intent intent = new Intent(getApplicationContext(), ReportActivity.class);
            intent.putExtra("address", address);
            startActivity(intent);
        }
    }
}
