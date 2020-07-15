package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.io.File

class worker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {
    private var Image: String? = null
    private var mrotationaldesgrees: String? = null

    @SuppressLint("RestrictedApi")
    override fun doWork(): Result {
        mrotationaldesgrees = inputData.getString("degree")
        Image = inputData.getString("image")
        val uri = Uri.parse(Image)
        delete(Image)
        return Result.success()
    }

    private fun delete(image: String?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post {
            val uri = Uri.parse(image)
            val fdelete = File(getFilePath(uri))
            if (fdelete.exists()) {
                if (fdelete.delete()) {
                    println("file Deleted :$uri")
                    applicationContext.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(fdelete)))
                } else {
                    println("file not Deleted :")
                }
            }
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
}