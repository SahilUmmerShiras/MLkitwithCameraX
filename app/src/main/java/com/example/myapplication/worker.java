package com.example.myapplication;

import android.annotation.SuppressLint;
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
import android.service.carrier.CarrierMessagingService;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.content.CursorLoader;
import androidx.work.Data;
import androidx.work.ListenableWorker;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class worker extends Worker {

    private String Image;
    private String mrotationaldesgrees;


    public worker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }





    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public Result doWork(){

        mrotationaldesgrees = getInputData().getString("degree");
        Image = getInputData().getString("image");



        Uri uri = Uri.parse(Image);

        delete(Image);


        return Result.success();

    }


    private void delete(String image) {
        Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
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
