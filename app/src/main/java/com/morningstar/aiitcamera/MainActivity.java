/*
 * Created by Sujoy Datta. Copyright (c) 2018. All rights reserved.
 *
 * To the person who is reading this..
 * When you finally understand how this works, please do explain it to me too at sujoydatta26@gmail.com
 * P.S.: In case you are planning to use this without mentioning me, you will be met with mean judgemental looks and sarcastic comments.
 */

package com.morningstar.aiitcamera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {


    private static final String IMAGE_DIRECTORY_NAME = "AIIT_Cam";
    private CircleImageView camera_image_button;
    private EditText number_input_field;
    private ImageView display_image;
    private CircleImageView button_redo, button_sync;

    private static int REQUEST_IMAGE_CAPTURE = 69;
    private static int PERMISSION_WRITE_REQUEST = 0;

    private String aadhar_number;
    private String image_name;

    private File dir;
    private File dir_name;
    private Bitmap bitmap;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        display_image = findViewById(R.id.imageview_image);
        camera_image_button = findViewById(R.id.imageview_camera);
        number_input_field = findViewById(R.id.input_field);
        button_redo = findViewById(R.id.button_redo);
        button_sync = findViewById(R.id.sync);
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_WRITE_REQUEST);
            }
            else{
                dir = new File(Environment.getExternalStorageDirectory(), IMAGE_DIRECTORY_NAME);
            }
        } else {
            createFolder();
        }


        camera_image_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aadhar_number = number_input_field.getText().toString();
                if (validateInputField(aadhar_number)) {
                    takePicture();
                } else {
                    Toast.makeText(MainActivity.this, "Fill in with the correct Aadhar Number", Toast.LENGTH_SHORT).show();
                }
            }
        });

        button_redo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retake();
            }
        });
        button_sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadPicture();
            }
        });
    }

    private void uploadPicture() {
        bitmap = BitmapFactory.decodeFile(dir_name.getAbsolutePath());
        if (bitmap!=null) {
            StorageReference child_ref = storageReference.child("images/" + image_name);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            byte[] compressed_data = byteArrayOutputStream.toByteArray();

            UploadTask uploadTask = (UploadTask) child_ref.putBytes(compressed_data).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(MainActivity.this, "Image Uploaded successfully", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, "Image could not be uploaded!", Toast.LENGTH_SHORT).show();
                }
            }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(MainActivity.this, "Please wait while the image is being uploaded!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        else{
            Toast.makeText(this, "The bitmap received was empty", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                createFolder();
            } else {
                Toast.makeText(this, "Permission could not be granted!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void createFolder() {
        dir = new File(Environment.getExternalStorageDirectory(), IMAGE_DIRECTORY_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
            Toast.makeText(this, "Directory created at: " + dir, Toast.LENGTH_SHORT).show();
        }
    }

    private void retake() {
        display_image.setVisibility(View.INVISIBLE);
        camera_image_button.setVisibility(View.VISIBLE);
    }

    private boolean validateInputField(String aadhar_number) {
        if (!TextUtils.isEmpty(aadhar_number) && (aadhar_number.length() == 12)) {
            return true;
        } else
            return false;
    }

    private void takePicture() {
        image_name = number_input_field.getText().toString();
        String file_name = image_name + ".jpg";
        dir_name = new File(dir, file_name);
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(dir_name));
        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Picture taken successfully!", Toast.LENGTH_SHORT).show();
            display_image.setVisibility(View.VISIBLE);
            camera_image_button.setVisibility(View.INVISIBLE);
            try {
                bitmap = BitmapFactory.decodeFile(dir_name.getAbsolutePath());
                display_image.setImageBitmap(bitmap);
            } catch (Exception e) {
                Toast.makeText(this, "Image saved but could not be set", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Picture could not be taken!", Toast.LENGTH_SHORT).show();
        }
    }
}
