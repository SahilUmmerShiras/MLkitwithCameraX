package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    var myviewmodel: myviewmodel? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.hide()
        if (allPermissiongranted()) {
            start.setOnClickListener(View.OnClickListener {
                startCamera()

                // Toast(getApplicationContext(),"blah something",Toast.LENGTH_SHORT).show();
            })
            stop.setOnClickListener(View.OnClickListener {
                CameraX.unbindAll()
                flip?.setEnabled(true)
            })
        } else {
            ActivityCompat.requestPermissions(this, REQUIREDPERMISSION, 101)
            finish()
        }
        myviewmodel = ViewModelProvider(this).get(com.example.myapplication.myviewmodel::class.java)
        myviewmodel!!.getText().observe(this, Observer { s -> textView.setText(s) })
    }

    private fun allPermissiongranted(): Boolean {
        for (permission in REQUIREDPERMISSION) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
        CameraX.unbindAll()
        var preview: Preview? = null
        flip!!.isEnabled = false
        val cam = flip!!.text as String
        if (cam == "FRONT") {
            preview = Preview(
                    PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.FRONT).build()
            )
            textureView!!.scaleX = -1f
        } else if (cam == "BACK") {
            preview = Preview(
                    PreviewConfig.Builder().setLensFacing(CameraX.LensFacing.BACK).build()
            )
            textureView!!.scaleX = 1f
        }
        preview!!.setOnPreviewOutputUpdateListener { output ->
            val parent = textureView.getParent() as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView, 0)
            textureView.setSurfaceTexture(output.surfaceTexture)
            updateTransform()
        }
        imageanalysis(preview)
    }

    private fun imageanalysis(preview: Preview?) {
        val executor = Executors.newFixedThreadPool(1)
        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
                .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                .build()
        val imageAnalyzer = ImageAnalysis(imageAnalysisConfig)
        imageAnalyzer.setAnalyzer(executor, ImageAnalysis.Analyzer { image, rotationDegrees ->
            val image1 = image.image
            val yo = toBitmap(image1)
            val uri = getImageUri(applicationContext, yo)
            mlkit(uri)
            Log.d("file added", uri.toString())
            val imager = uri.toString()
            val degree = rotationDegrees.toString()
            val myData = Data.Builder()
                    .put("image", imager)
                    .put("degree", degree)
                    .build()
            val uploadWorkRequest: WorkRequest = OneTimeWorkRequest.Builder(worker::class.java)
                    .setInputData(myData)
                    .build()
            WorkManager.getInstance(this@MainActivity).enqueue(uploadWorkRequest)
        })
        CameraX.bindToLifecycle(this, preview, imageAnalyzer)
    }

    fun mlkit(uri: Uri?) {
        val image: FirebaseVisionImage
        try {
            image = FirebaseVisionImage.fromFilePath(applicationContext, uri!!)
            val labeler = FirebaseVision.getInstance()
                    .onDeviceImageLabeler
            labeler.processImage(image)
                    .addOnSuccessListener { labels ->
                        for (label in labels) {
                            val text = label.text
                            val confidence = label.confidence
                            if (confidence > 0.7) {
                                //postToastMessage(text);
                                myviewmodel!!.changetext(text)
                                Log.d("here is the thing with confidence", text)
                            }
                        }
                    }
                    .addOnFailureListener { e -> Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun getFilePath(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = applicationContext.contentResolver.query(uri, projection, null, null, null)
        if (cursor != null) {
            cursor.moveToFirst()
            val columnIndex = cursor.getColumnIndex(projection[0])
            val picturePath = cursor.getString(columnIndex) // returns null
            cursor.close()
            return picturePath
        }
        return null
    }

    fun postToastMessage(message: String?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
    }

    private fun updateTransform() {
        val mx = Matrix()
        val w = textureView!!.measuredWidth.toFloat()
        val h = textureView!!.measuredHeight.toFloat()
        val cX = w / 2f
        val cY = h / 2f
        val rotationDgr: Int
        val rotation = textureView!!.rotation.toInt()
        rotationDgr = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        mx.postRotate(rotationDgr.toFloat(), cX, cY)
        textureView!!.scaleX = 1f
        textureView!!.setTransform(mx)
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    private fun toBitmap(image: Image?): Bitmap {
        val planes = image!!.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)
        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 75, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && null != data) {
            val projection = arrayOf(
                    MediaStore.Images.ImageColumns._ID,
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.MIME_TYPE
            )
            val cursor = contentResolver
                    .query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null,
                            null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC")
            if (cursor == null || cursor.count < 1) {
                return  // no cursor or no record. DO YOUR ERROR HANDLING
            }
        }
    }

    companion object {
        var REQUIREDPERMISSION = arrayOf("android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE")
    }
}