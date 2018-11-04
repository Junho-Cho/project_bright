package com.example.lgpc.project_bright;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.renderscript.Sampler;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class BuildingActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "BuildingActivity";
    public static Context mContext;
    private String myToken;

    private FirebaseAuth firebaseAuth;

    private TextView textViewUserEmail;
    private ImageView imageUserIcon;
    private ImageView imageAddFloor;
    private EditText currentUserEmail;

    RecyclerView mRecyclerView;
    LinearLayoutManager mLayoutManager;

    ArrayList<floor_info> floorInfoArrayList = new ArrayList<>();
    MyAdapter myAdapter = new MyAdapter(floorInfoArrayList);

    // Firebase Database
    private DatabaseReference databaseReference;
    EditText editBuildingName;
    ImageView confirmBuilding;
    String UID;
    String EM;
    String BN;
    int FN;
    String sort = "userID";
    static ArrayList<String> arrayIndex = new ArrayList<String>();

    // Gallery
    int reqWidth;
    int reqHeight;
    int position;

    int downloadCnt;
    int uploadCnt;

    // GPS
    private final int PERMISSIONS_ACCESS_FINE_LOCATION = 1000;
    private final int PERMISSIONS_ACCESS_COARSE_LOCATION = 1001;
    private boolean isAccessFineLocation = false;
    private boolean isAccessCoarseLocation = false;
    private boolean isPermission = false;
    private GpsInfo gps;
    double gps_x, gps_y, latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_building);

        mContext = this;

        reqWidth = getResources().getDimensionPixelSize(R.dimen.request_image_width);
        reqHeight = getResources().getDimensionPixelSize(R.dimen.request_image_height);

        textViewUserEmail = (TextView) findViewById(R.id.current_email);
        imageUserIcon = (ImageView) findViewById(R.id.user_icon);
        imageAddFloor = (ImageView) findViewById(R.id.add_floor_image);
        confirmBuilding = (ImageView) findViewById(R.id.confirm);
        currentUserEmail = (EditText) findViewById(R.id.current_user_email);

        firebaseAuth = FirebaseAuth.getInstance();

        if (firebaseAuth.getCurrentUser() == null) {
            finish();
            startActivity(new Intent(this, MainActivity.class));
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();

        textViewUserEmail.setText("  " + user.getEmail() + "님, 안녕하세요.  ");
        textViewUserEmail.setVisibility(textViewUserEmail.GONE);
        currentUserEmail.setVisibility(currentUserEmail.GONE);

        imageUserIcon.setOnClickListener(this);
        imageAddFloor.setOnClickListener(this);
        confirmBuilding.setOnClickListener(this);

        // RecyclerView
        mRecyclerView = findViewById(R.id.building_recycler);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Firebase Database
        editBuildingName = (EditText) findViewById(R.id.building_name);

        getFirebasePost();
        mRecyclerView.setAdapter(myAdapter);

        // GPS
        callPermission();

        // FCM Token
        myToken = FirebaseInstanceId.getInstance().getToken();
    }

    public void ClickAddPicBt(int position) {
        this.position = position;
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 20);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 20 && resultCode == RESULT_OK) {
            String[] projection={MediaStore.Images.Media.DATA};
            Cursor cursor=getContentResolver().query(data.getData(), projection, null, null, null);
            cursor.moveToFirst();
            String filePath=cursor.getString(0);
            insertImageView(filePath);
        }
    }

    private void insertImageView(String filePath) {
        if (!filePath.equals("")) {
            File file = new File(filePath);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            try {
                InputStream in = new FileInputStream(filePath);
                BitmapFactory.decodeStream(in, null, options);
                in.close();
                in = null;
            } catch (Exception e) {
                e.printStackTrace();
            }

            final int width = options.outWidth;
            final int height = options.outHeight;
            int inSampleSize = 1;
            if(width>reqWidth || height > reqHeight){
                int widthRatio=Math.round((float)width / (float)reqWidth);
                int heightRatio = Math.round((float)height / (float)reqHeight);
                inSampleSize=heightRatio<widthRatio ? heightRatio : widthRatio;
            }
            BitmapFactory.Options imgOptions=new BitmapFactory.Options();
            imgOptions.inSampleSize=inSampleSize;
            Bitmap bitmap=BitmapFactory.decodeFile(filePath, imgOptions);
            floorInfoArrayList.get(position).setPic(bitmap);
            myAdapter.notifyDataSetChanged();
        }
    }

    private String getFilePathFromUriSegment(Uri uri) {
        String selection=MediaStore.Images.Media._ID+"=?";
        String[] selectionArgs=new String[]{uri.getLastPathSegment()};

        String column="_data";
        String[] projection={column};

        Cursor cursor=getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null);
        String filePath=null;
        if(cursor != null && cursor.moveToFirst()){
            int column_index=cursor.getColumnIndexOrThrow(column);
            filePath=cursor.getString(column_index);
        }
        cursor.close();
        return filePath;
    }

    public void postFirebasePost(boolean add) {
        databaseReference = FirebaseDatabase.getInstance().getReference();
        Map<String, Object> childUpdates = new HashMap<>();
        Map<String, Object> buildingValues = null;
        if (add) {
            if (gps_x == 0 && gps_y == 0) {
                FirebasePost building = new FirebasePost(UID, BN, FN, latitude, longitude, myToken, EM);
                buildingValues = building.toMap();
            } else {
                FirebasePost building = new FirebasePost(UID, BN, FN, gps_x, gps_y, myToken, EM);
                buildingValues = building.toMap();
            }
        }
        childUpdates.put("/user_list/" + UID, buildingValues);
        databaseReference.updateChildren(childUpdates);
    }

    private void uploadFile() {
        if (BN.length() == 0) {
            Toast.makeText(mContext, "빌딩 이름을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (myAdapter.getItemCount() == 0) {
            Toast.makeText(mContext, "ERROR: NO FLOOR", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(this);
        boolean flag = true;
        progressDialog.setTitle("잠시만 기다려주세요.");
        progressDialog.show();

        FirebaseStorage storage = FirebaseStorage.getInstance();

        for (int i = 0; i < myAdapter.getItemCount(); i++) {
            if (floorInfoArrayList.get(i).drawableId == null) {
                progressDialog.dismiss();
                Toast.makeText(BuildingActivity.this, "ERROR: NO PICTURES", Toast.LENGTH_SHORT).show();
                flag = false;
                break;
            }
        }

        if (flag) {
            postFirebasePost(true);
            for (int i = 0; i < myAdapter.getItemCount(); i++) {
                final int index = i;
                final int total = myAdapter.getItemCount();
                uploadCnt = 0;
                String filename = floorInfoArrayList.get(i).floorId + ".png";
                StorageReference storageRef = storage.getReferenceFromUrl("gs://bright-project-7fbc2.appspot.com").child(firebaseAuth.getCurrentUser().getUid() + "/" + filename);
                Bitmap bm = floorInfoArrayList.get(i).drawableId;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.PNG, 100, bytes);
                byte[] data = bytes.toByteArray();

                storageRef.putBytes(data)
                        .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                uploadCnt++;
                                if (uploadCnt == total) {
                                    progressDialog.dismiss();
                                    startActivity(new Intent(BuildingActivity.this, AdminActivity.class));
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                progressDialog.dismiss();
                                Toast.makeText(getApplicationContext(), "업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                                @SuppressWarnings("visibleForTests")
                                double progress = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                                progressDialog.setMessage("Uploaded " + ((int) progress) + "% ...");
                            }
                        });
            }
        }
    }

    public void getFirebasePost() {
        downloadCnt = 0;
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("빌딩 정보를 불러오는 중입니다...");
        progressDialog.show();

        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                int f = 0;
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    final FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef;
                    if (key.equals(firebaseAuth.getCurrentUser().getUid())) {
                        FirebasePost get = postSnapshot.getValue(FirebasePost.class);
                        String[] info = {get.userID, get.buildingName, String.valueOf(get.floorNumber)};
                        double[] GpsInfo = {get.building_x, get.building_y};
                        gps_x = GpsInfo[0]; gps_y = GpsInfo[1];
                        editBuildingName.setText(info[1]);
                        for (int i = 0; i < get.floorNumber; i++) {
                            floorInfoArrayList.add(new floor_info((i + 1) + "층"));
                        }
                        for (int i = 0; i < get.floorNumber; i++) {
                            final int index = i;
                            final int totalFloorNumber = get.floorNumber;
                            storageRef = storage.getReferenceFromUrl("gs://bright-project-7fbc2.appspot.com").child(firebaseAuth.getCurrentUser().getUid() + "/" + Integer.toString(i + 1) + "층.png");

                            storageRef.getBytes(1024 * 1024)
                                    .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                        @Override
                                        public void onSuccess(byte[] bytes) {
                                            downloadCnt++;
                                            if (downloadCnt == totalFloorNumber) {
                                                progressDialog.dismiss();
                                            }
                                            floorInfoArrayList.get(index).setPic(BitmapFactory.decodeByteArray(bytes, 0, bytes.length));
                                            myAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                        }
                                    });
                        }
                        mRecyclerView.setAdapter(myAdapter);
                        f = 1;
                        break;
                    }
                }
                if (f == 0) {
                    AlertDialog.Builder noticeDialogBuilder = new AlertDialog.Builder(mContext);
                    noticeDialogBuilder.setTitle("주의하세요!");
                    noticeDialogBuilder
                            .setMessage("건물의 최초 등록은 반드시 건물 내에서 해주세요.\n" +
                                    "한 번 등록한 건물의 GPS 정보는 바꿀 수 없습니다.")
                            .setCancelable(false)
                            .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    progressDialog.dismiss();
                    noticeDialogBuilder.create();
                    noticeDialogBuilder.show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        };
        Query sortbyAge = FirebaseDatabase.getInstance().getReference().child("user_list").orderByChild(sort);
        sortbyAge.addListenerForSingleValueEvent(postListener);
    }


    public int retFloorCount() {
        return myAdapter.getItemCount();
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

    OnMenuItemClickListener listener = new OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch(item.getItemId()) {
                case R.id.logout:
                    firebaseAuth.signOut();
                    finish();
                    startActivity(new Intent(BuildingActivity.this, MainActivity.class));
                    break;
                case R.id.delete_account:
                    AlertDialog.Builder alert_confirm = new AlertDialog.Builder(BuildingActivity.this);
                    alert_confirm.setMessage("정말 계정을 삭제할까요?").setCancelable(false).setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                            user.delete()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            Toast.makeText(BuildingActivity.this, "계정이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                            finish();
                                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                        }
                                    });
                        }
                    });
                    alert_confirm.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(BuildingActivity.this, "취소", Toast.LENGTH_SHORT).show();
                        }
                    });
                    alert_confirm.show();
                    break;
            }
            return false;
        }
    };

    @Override
    public void onClick(View view) {
        int count;
        count = myAdapter.getItemCount();

        switch (view.getId()) {
            case R.id.user_icon:
                if (textViewUserEmail.getVisibility() == textViewUserEmail.VISIBLE) {
                    textViewUserEmail.setVisibility(textViewUserEmail.GONE);
                } else {
                    textViewUserEmail.setVisibility(textViewUserEmail.VISIBLE);
                    PopupMenu popup = new PopupMenu(this, view);
                    getMenuInflater().inflate(R.menu.accountmenu, popup.getMenu());
                    popup.setOnMenuItemClickListener(listener);
                    popup.show();
                }
                break;

            case R.id.confirm:
                if (!isPermission) {
                    callPermission();
                    return;
                }

                gps = new GpsInfo(BuildingActivity.this);
                if (gps.isGetLocation()) {
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                } else {
                    gps.showSettingAlert();
                }

                gps.stopUsingGPS();

                UID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                EM = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                BN = editBuildingName.getText().toString();
                FN = myAdapter.getItemCount();
                currentUserEmail.setText(EM);
                uploadFile();
                break;
        }

        if (view == imageAddFloor) {
            floorInfoArrayList.add(new floor_info((count + 1) + "층"));
            mRecyclerView.setAdapter(myAdapter);
        }
    }
}
