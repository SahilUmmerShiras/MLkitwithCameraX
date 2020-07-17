package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;


import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;

import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {


    static String[] REQUIREDPERMISSION = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;
    Button start;
    Button stop;
    ToggleButton flip;
    TextView textView;
    private Rectangle rectangle;

    private static final double FOCUS_WIDTH_PERCENTAGE = 0.18;



    myviewmodel myviewmodel;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textureView = findViewById(R.id.textureView);
        start = findViewById(R.id.button);
        stop = findViewById(R.id.button2);
        flip = findViewById(R.id.flip);
        textView = findViewById(R.id.textView);

        getSupportActionBar().hide();
        rectangle = new Rectangle(this);

        if(allPermissiongranted()) {

            start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startCamera();
                    // Toast(getApplicationContext(),"blah something",Toast.LENGTH_SHORT).show();

                }
            });

            stop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CameraX.unbindAll();
                    flip.setEnabled(true);
                }
            });

            textureView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("working at" , " 1");
                    if(event.getAction() == MotionEvent.ACTION_DOWN){
                        focusTappedArea(
                                event.getX(),
                                event.getY(),
                                true
                        );
                    }
                    return true;
                }
            });




        }
        else
        {
            ActivityCompat.requestPermissions(this,REQUIREDPERMISSION, 101);
            finish();
        }


        myviewmodel = ViewModelProviders.of(this).get(myviewmodel.class);
        myviewmodel.getText().observe(this, new Observer<String>() {
            @Override
            public void onChanged(String s) {
                textView.setText(s);
            }
        });

    }

    private void focusTappedArea(float x, float y, boolean drawRectangle ) {

        try {

            if (drawRectangle)
            {
                Log.d("working at" , " 2");
                drawFocusRectangle((int) x, (int) y);
            }

        }
        catch(Exception e)
        {
            Toast.makeText(getApplicationContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    private class Rectangle extends View {
        private final Paint paint = new Paint();
        private int xCoordinate=0, yCoordinate=0;
        private Handler mHandler = new Handler();
        private boolean shouldDraw = true;
        private Rect rect = new Rect(xCoordinate,yCoordinate,0,0);

        public Rectangle(Context context) {
            super(context);
        }

        @Override
        protected void onDraw(final Canvas canvas) {

            paint.setColor(Color.YELLOW);
            paint.setStyle(Paint.Style.STROKE);
            paint.setTextSize(24);

            if(shouldDraw)
            {canvas.drawRect(rect,paint);}

        }

        public void setRect(Rect rect) {this.rect = rect;}

        public void setxCoordinate(int xCoordinate) {
            this.xCoordinate = xCoordinate;
        }

        public void setyCoordinate(int yCoordinate) {
            this.yCoordinate = yCoordinate;
        }

        public void drawFocusRectangle(){
            Log.d("working at" , " 4");
            toggleRectangle(true);

            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    shouldDraw =false;
                    invalidate();
                }
            },600L);

        }
        private void toggleRectangle(boolean visible){
            Log.d("working at" , " 5");
            shouldDraw = visible;
            invalidate();
        }

    }


    private void drawFocusRectangle(int x, int y) {
        rectangle.setxCoordinate(x);
        rectangle.setyCoordinate(y);
        Log.d("working at" , " 3");
        rectangle.setRect(new Rect(
                        x,
                        y,
                        (int)(x+textureView.getWidth()*FOCUS_WIDTH_PERCENTAGE),
                        (int)(y+textureView.getHeight()*FOCUS_WIDTH_PERCENTAGE)
                )
        );

        rectangle.drawFocusRectangle();

    }

    private boolean allPermissiongranted() {
        for(String permission : REQUIREDPERMISSION)
        {
            if(ContextCompat.checkSelfPermission(this,permission)!= PackageManager.PERMISSION_GRANTED)
            {
                return false;
            }

        }

        return true;

    }

    @SuppressLint("RestrictedApi")
    private void startCamera(){

        CameraX.unbindAll();
        Preview preview = null;
        flip.setEnabled(false);
        String cam = (String) flip.getText();
        if(cam.equals("FRONT"))
        {
            preview = new Preview(
                    new PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.FRONT).build()
            );
            textureView.setScaleX(-1);

        }
        
        else if (cam.equals("BACK"))
        {
            preview = new Preview(
                    new PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.BACK).build()
            );
            textureView.setScaleX(1);
        }
        

        preview.setOnPreviewOutputUpdateListener(new Preview.OnPreviewOutputUpdateListener() {
            @Override
            public void onUpdated(@NonNull Preview.PreviewOutput output) {
                textureView = findViewById(R.id.textureView);
                ViewGroup parent = (ViewGroup) textureView.getParent();
                parent.removeView(textureView);
                parent.addView(textureView, 0);
                textureView.setSurfaceTexture(output.getSurfaceTexture());

                updateTransform();

            }
        });


        imageanalysis(preview);


    }

    private void imageanalysis(Preview preview) {
        ExecutorService executor = Executors.newFixedThreadPool(1);

        ImageAnalysisConfig imageAnalysisConfig = new ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build();
        ImageAnalysis imageAnalyzer = new ImageAnalysis(imageAnalysisConfig);
        imageAnalyzer.setAnalyzer(executor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy image, int rotationDegrees) {

                Image image1 = image.getImage();

                Bitmap yo = toBitmap(image1);

                Uri uri = getImageUri(getApplicationContext(),yo);

                mlkit(uri);

                Log.d("file added", uri.toString());

                String imager = uri.toString();
                String degree = String.valueOf(rotationDegrees);

                Data myData = new Data.Builder()
                        .put("image",imager)
                        .put("degree",degree)
                        .build();

                WorkRequest uploadWorkRequest =
                        new OneTimeWorkRequest.Builder(worker.class)
                                .setInputData(myData)
                                .build();

                WorkManager.getInstance(MainActivity.this).enqueue(uploadWorkRequest);

            }
        });
        CameraX.bindToLifecycle(this, preview, imageAnalyzer);
    }


    public void mlkit(Uri uri)
    {
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

                                if(confidence>0.7)
                                {
                                    //postToastMessage(text);
                                    myviewmodel.changetext(text);
                                    Log.d("here is the thing with confidence",text);
                                }

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

    public void postToastMessage(final String message) {
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateTransform(){
        Matrix mx = new Matrix();
        float w = textureView.getMeasuredWidth();
        float h = textureView.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;

        int rotationDgr;
        int rotation = (int)textureView.getRotation();

        switch(rotation){
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        mx.postRotate((float)rotationDgr, cX, cY);
        textureView.setScaleX(1);
        textureView.setTransform(mx);
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private Bitmap toBitmap(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


}
