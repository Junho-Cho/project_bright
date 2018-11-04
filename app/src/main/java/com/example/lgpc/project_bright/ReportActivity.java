package com.example.lgpc.project_bright;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class ReportActivity extends AppCompatActivity implements View.OnClickListener {

    ImageView btConfirm;
    ImageView resultImageView;
    ImageView btPickPicture;
    ImageView btTakePicture;
    EditText inputAddress;
    EditText inputBody;

    File filePath;
    int reqWidth;
    int reqHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_report);

        btConfirm = (ImageView) findViewById(R.id.confirm);
        resultImageView = (ImageView) findViewById(R.id.result);
        btPickPicture = (ImageView) findViewById(R.id.pick_picture);
        btTakePicture = (ImageView) findViewById(R.id.take_picture);
        inputAddress = (EditText) findViewById(R.id.input_address);
        inputBody = (EditText) findViewById(R.id.input_body);

        btConfirm.setOnClickListener(this);
        btPickPicture.setOnClickListener(this);
        btTakePicture.setOnClickListener(this);

        reqWidth = getResources().getDimensionPixelSize(R.dimen.request_image_width);
        reqHeight = getResources().getDimensionPixelSize(R.dimen.request_image_height);

        Intent intent = getIntent();
        String address = intent.getStringExtra("address");
        inputAddress.setText(address);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 20 && resultCode == RESULT_OK) {
            String[] projection={MediaStore.Images.Media.DATA};
            Cursor cursor=getContentResolver().query(data.getData(), projection, null, null, null);
            cursor.moveToFirst();
            String filePath=cursor.getString(0);
            insertImageView(filePath);
        } else if (requestCode == 40 && resultCode == RESULT_OK) {
            if(filePath != null){
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inJustDecodeBounds=true;
                try{
                    InputStream in=new FileInputStream(filePath);
                    BitmapFactory.decodeStream(in, null, options);
                    in.close();
                    in=null;
                }catch (Exception e){
                    e.printStackTrace();
                }
                final int height=options.outHeight;
                final int width=options.outWidth;
                int inSampleSize=1;
                if(height>reqHeight || width>reqWidth){
                    final int heightRatio=Math.round((float)height/(float)reqHeight);
                    final int widthtRatio=Math.round((float)width/(float)reqWidth);


                    inSampleSize=heightRatio<widthtRatio ? heightRatio : widthtRatio;
                }

                BitmapFactory.Options imgOptions=new BitmapFactory.Options();
                imgOptions.inSampleSize=inSampleSize;
                Bitmap bitmap=BitmapFactory.decodeFile(filePath.getAbsolutePath(), imgOptions);
                resultImageView.setImageBitmap(bitmap);
            }
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
            int inSampleSize = 1;

            if(width>reqWidth){
                int widthRatio=Math.round((float)width / (float)reqWidth);
                inSampleSize=widthRatio;
            }

            BitmapFactory.Options imgOptions=new BitmapFactory.Options();
            imgOptions.inSampleSize=inSampleSize;
            Bitmap bitmap=BitmapFactory.decodeFile(filePath, imgOptions);
            resultImageView.setImageBitmap(bitmap);
        }
    }

    private String getFilePathFromUriSegment(Uri uri){
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

    @Override
    public void onClick(View view) {
        if (view == btPickPicture) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            intent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, 20);
        }
        if (view == btTakePicture) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
                try{
                    String dirPath= Environment.getExternalStorageDirectory().getAbsolutePath()+"/myApp";
                    File dir=new File(dirPath);
                    if(!dir.exists())
                        dir.mkdir();
                    filePath=File.createTempFile("IMG",".jpg", dir);
                    if(!filePath.exists())
                        filePath.createNewFile();

                    Uri photoURI= FileProvider.getUriForFile(ReportActivity.this, BuildConfig.APPLICATION_ID+".provider", filePath);
                    Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(intent, 40);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
        if (view == btConfirm) {
            if (inputAddress.getText().toString().length() == 0 || inputBody.getText().toString().length() == 0) {
                Toast.makeText(this, "빠진 양식이 없는지 확인해 주십시오.", Toast.LENGTH_SHORT).show();
            } else if (resultImageView.getDrawable() == null) {
                Toast.makeText(this, "사진은 필수 양식입니다.", Toast.LENGTH_SHORT).show();
            } else {
                finish();
                Toast.makeText(this, "감사합니다. 신고가 접수되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
