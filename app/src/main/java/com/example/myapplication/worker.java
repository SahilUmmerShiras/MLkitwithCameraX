package com.example.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.loader.content.CursorLoader;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.objects.FirebaseVisionObject;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetector;
import com.google.firebase.ml.vision.objects.FirebaseVisionObjectDetectorOptions;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class worker extends Worker{

    private String Image;
    private String mrotationaldesgrees;

    public worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }


    @NonNull
    @Override
    public Result doWork(){


        mrotationaldesgrees = getInputData().getString("degree");
        Image = getInputData().getString("image");



        Uri uri = Uri.parse(Image);

        Log.d("file added", uri.toString());

        mlkit(uri);



        return Result.success();
    }



    private void mlkit(Uri uri) {
        FirebaseVisionImage image;
        try {

            image = FirebaseVisionImage.fromFilePath(getApplicationContext(), uri);


            FirebaseVisionImageLabeler labeler = FirebaseVision.getInstance()
                    .getOnDeviceImageLabeler();


            labeler.processImage(image)
                    .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionImageLabel> labels) {
                            for (FirebaseVisionImageLabel label: labels) {
                                String text = label.getText();
                                float confidence = label.getConfidence();
                                if(confidence>=0.8) {
                                    postToastMessage(text);
                                }
                                Image = getInputData().getString("image");


                                delete(Image);




                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                           Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void delete(String image) {

        Uri uri = Uri.parse(image);

        File fdelete = new File(getFilePath(uri));

        if (fdelete.exists()) {
            if (fdelete.delete()) {
                System.out.println("file Deleted :"+uri.toString() );
                getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fdelete)));
            } else {
                System.out.println("file not Deleted :");
            }
        }
    }

    public void postToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private String getFilePath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};

        Cursor cursor = getApplicationContext().getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(projection[0]);
            String picturePath = cursor.getString(columnIndex); // returns null
            cursor.close();
            return picturePath;
        }
        return null;
    }





}
