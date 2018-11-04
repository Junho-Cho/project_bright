package com.example.lgpc.project_bright;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class AdminActivity extends AppCompatActivity implements View.OnClickListener {
    public static Context mAdminContext;

    ImageView imageOpenMenu;
    ImageView backgroundMap;
    ImageView addNodeBt;
    RecyclerView adminRecyclerView;
    RecyclerView.LayoutManager adminLayoutManager;
    EditText inputNodeName;
    TextView outputNodeName;

    private FirebaseAuth firebaseAuth;

    int FloorCnt, currentFloor;

    private DatabaseReference databaseReference;
    final FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageRef;

    int x, y;

    String UID;
    ArrayList<NodeInfo> Nds = new ArrayList<>();
    String sort = "userID";

    HashMap<String, Button> nodesSomeFloor = new HashMap<>();

    // For delete node image
    String nameForDel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_admin);

        mAdminContext = this;

        firebaseAuth = FirebaseAuth.getInstance();

        imageOpenMenu = (ImageView) findViewById(R.id.menu);
        backgroundMap = (ImageView) findViewById(R.id.bg_map);
        addNodeBt = (ImageView) findViewById(R.id.add_node);
        adminRecyclerView = findViewById(R.id.admin_recycler);
        adminRecyclerView.setHasFixedSize(true);
        adminLayoutManager = new LinearLayoutManager(this);
        adminRecyclerView.setLayoutManager(adminLayoutManager);

        ArrayList<MenuInfo> menuInfoArrayList = new ArrayList<>();
        menuInfoArrayList.add(new MenuInfo("메인으로"));

        adminAdapter myAdminAdapter = new adminAdapter(menuInfoArrayList);
        adminRecyclerView.setAdapter(myAdminAdapter);

        FloorCnt = ((BuildingActivity)BuildingActivity.mContext).retFloorCount();

        for (int i = 0; i < FloorCnt; i++) {
            menuInfoArrayList.add(new MenuInfo((i + 1) + "층"));
        }

        adminRecyclerView.setVisibility(adminRecyclerView.GONE);

        imageOpenMenu.setOnClickListener(this);
        addNodeBt.setOnClickListener(this);

        UID = firebaseAuth.getCurrentUser().getUid();

        currentFloor = 1;
        setFirstFloor();
        inputNodeInDB();
    }

    public void setFirstFloor() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("해당 층의 정보를 불러오는 중입니다...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        storageRef = storage.getReferenceFromUrl("gs://bright-project-7fbc2.appspot.com").child(firebaseAuth.getCurrentUser().getUid() + "/" + "1층.png");

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
                        Toast.makeText(AdminActivity.this, "해당 층 표시에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });

        getFirebaseNode();
    }
    public void setOtherFloor(String menuStr) {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("해당 층의 정보를 불러오는 중입니다...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        storageRef = storage.getReferenceFromUrl("gs://bright-project-7fbc2.appspot.com").child(firebaseAuth.getCurrentUser().getUid() + "/" + menuStr + ".png");

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
                        Toast.makeText(AdminActivity.this, "해당 층 표시에 실패했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });

        adminRecyclerView.setVisibility(adminRecyclerView.GONE);

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

    public void postFirebaseNode(boolean add) {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> nodeValues = null;
        if (add) {
            FirebaseNode node = new FirebaseNode(Nds);
            nodeValues = node.toMap();
        }
        childUpdates.put("/node_list/" + UID, nodeValues);
        databaseReference.updateChildren(childUpdates);
    }

    public void getFirebaseNode() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    if (key.equals(firebaseAuth.getCurrentUser().getUid())) {
                        final FirebaseNode g = postSnapshot.getValue(FirebaseNode.class);
                        for (int i = 0; i < g.nodes.size(); i++) {
                            //Nds.add(new NodeInfo(info[0], info[1], info[2], infoName, tmp));

                            if (g.nodes.get(i).floor == currentFloor) {
                                int boolToInt = g.nodes.get(i).state ? 1 : 0;
                                final String infoName = g.nodes.get(i).name;
                                int[] info = {g.nodes.get(i).x, g.nodes.get(i).y, g.nodes.get(i).floor, boolToInt};
                                boolean tmp = (info[3] == 1);

                                RelativeLayout.LayoutParams lp;
                                Button newNode = new Button(mAdminContext);

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
                                        AlertDialog.Builder outputBuilder = new AlertDialog.Builder(AdminActivity.this);
                                        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                                        View view = inflater.inflate(R.layout.output_node_info, null);
                                        outputNodeName = (TextView) view.findViewById(R.id.this_node_name);
                                        outputNodeName.setText(infoName);
                                        nameForDel = infoName;
                                        outputBuilder.setView(view);
                                        outputBuilder.setPositiveButton("확인", null);
                                        outputBuilder.setNegativeButton("노드 제거", deleteButtonClickListener);

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

    public void inputNodeInDB() {
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    if (key.equals(firebaseAuth.getCurrentUser().getUid())) {
                        FirebaseNode g = postSnapshot.getValue(FirebaseNode.class);
                        for (int i = 0; i < g.nodes.size(); i++) {
                            int boolToInt = g.nodes.get(i).state ? 1 : 0;
                            int[] info = {g.nodes.get(i).x, g.nodes.get(i).y, g.nodes.get(i).floor, boolToInt};
                            String infoName = g.nodes.get(i).name;
                            boolean tmp = (info[3] == 1);
                            Nds.add(new NodeInfo(info[0], info[1], info[2], infoName, tmp));
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

    public void deleteNode() {
        for (int i = 0; i < Nds.size(); i++) {
            if (Nds.get(i).name.equals(nameForDel)) {
                Nds.remove(i);
                break;
            }
        }
        Set<String> key = nodesSomeFloor.keySet();
        for (Iterator<String> iterator = key.iterator(); iterator.hasNext();) {
            String stTmp = iterator.next();
            Button btTmp = nodesSomeFloor.get(stTmp);
            if ((stTmp).equals(nameForDel)) {
                ((ViewManager)btTmp.getParent()).removeView(btTmp);
                nodesSomeFloor.remove(stTmp);
                break;
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == imageOpenMenu) {
            if (adminRecyclerView.getVisibility() == adminRecyclerView.GONE) {
                adminRecyclerView.setVisibility(adminRecyclerView.VISIBLE);
            } else {
                adminRecyclerView.setVisibility(adminRecyclerView.GONE);
            }
        }

        if (view == addNodeBt) {
            Toast.makeText(AdminActivity.this, "디바이스의 위치를 선택해주세요.", Toast.LENGTH_SHORT).show();
            backgroundMap.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == event.ACTION_DOWN) {
                        x = (int) event.getX();
                        y = (int) event.getY();

                        String msg = "노드 위치 ( " + x + ", " + y + " )";
                        Toast.makeText(AdminActivity.this, msg, Toast.LENGTH_SHORT).show();

                        AlertDialog.Builder builder = new AlertDialog.Builder(AdminActivity.this);
                        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.input_node_info, null);
                        builder.setView(view);
                        inputNodeName = (EditText) view.findViewById(R.id.node_name);
                        builder.setPositiveButton("등록", yesButtonClickListener);
                        builder.setNegativeButton("취소", noButtonClickListener);

                        builder.create();
                        builder.show();
                    }
                    return false;
                }
            });
        }
    }

    private DialogInterface.OnClickListener yesButtonClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            boolean flag = true;

            for (int i = 0; i < Nds.size(); i++) {
                if ((Nds.get(i).name).equals(inputNodeName.getText().toString())) {
                    Toast.makeText(AdminActivity.this, "이미 존재하는 이름입니다.", Toast.LENGTH_SHORT).show();
                    flag = false;
                    break;
                }
            }
            if (inputNodeName.getText().toString().length() == 0) {
                Toast.makeText(AdminActivity.this, "노드 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            } else if (flag == true) {
                RelativeLayout.LayoutParams lp;
                Button newNode = new Button(mAdminContext);

                lp = new RelativeLayout.LayoutParams(120, 120);
                lp.leftMargin = x - 60;
                lp.topMargin = y - 60;
                newNode.setLayoutParams(lp);
                newNode.setAlpha(0.75f);
                newNode.setBackgroundResource(R.drawable.node_green);

                addContentView(newNode, lp);

                final String thisName = inputNodeName.getText().toString();
                Nds.add(new NodeInfo(x, y, currentFloor, thisName, true));
                nodesSomeFloor.put(thisName, newNode);
                //NdsDel.add(new NodeInfo(x, y, currentFloor, thisName, true));
                postFirebaseNode(true);

                newNode.setOnClickListener(new Button.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder outputBuilder = new AlertDialog.Builder(AdminActivity.this);
                        LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                        View view = inflater.inflate(R.layout.output_node_info, null);
                        outputNodeName = (TextView) view.findViewById(R.id.this_node_name);
                        outputNodeName.setText(thisName);
                        nameForDel = thisName;
                        outputBuilder.setView(view);
                        outputBuilder.setPositiveButton("확인", null);
                        outputBuilder.setNegativeButton("노드 제거", deleteButtonClickListener);

                        outputBuilder.create();
                        outputBuilder.show();
                    }
                });
            }

            backgroundMap.setOnTouchListener(null);
        }
    };
    private DialogInterface.OnClickListener noButtonClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            backgroundMap.setOnTouchListener(null);
        }
    };
    private DialogInterface.OnClickListener deleteButtonClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            deleteNode();
            postFirebaseNode(true);
        }
    };
}
