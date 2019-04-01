package com.intrepiditee.wastedata

import android.Manifest
import android.app.DownloadManager
import android.app.IntentService
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.util.Log
import com.intrepiditee.wastedata.Utils.Companion.showToast
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread


class DownloadService : Service() {
    // Unit is B
    private var amountToWaste: Long = 0
    private var amountWasted = AtomicLong()

    private val downloadBinder = DownloadBinder()
    private lateinit var downloadManager: DownloadManager
    private var downloadingID: Long = 0

    private val downloadURLs = arrayListOf(
        "https://dl.google.com/dl/android/studio/install/3.3.2.0/android-studio-ide-182.5314842-windows.exe",
        "https://upload.wikimedia.org/wikipedia/commons/2/2d/Snake_River_%285mb%29.jpg"
    )

    private val fileToSize = ConcurrentHashMap(downloadURLs.associateWith { 0 })

    private val downloadBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadID = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0)

            // If download is cancelled by the stop button, do not start next download
            if (!isDownloadSuccessful(downloadID)) {
                Log.e("onReceive", "download $downloadID not successful")
                return
            }

            // Get the number of bytes of the finished download and add it to the amount wasted so far
            amountWasted.addAndGet(getAmountDownloaded(downloadID))
            cleanDownload()

            // Begin next download
            val nextFileToDownload = getLargestFile()

            // Start the next download
            enqueueFile(nextFileToDownload)

            Log.i("onReceive", "starting next download $nextFileToDownload")
        }
    }

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService {
            return this@DownloadService
        }
    }

    private fun isDownloadSuccessful(id: Long): Boolean {
        val cursor = getCursor(id)

        if (!cursor.moveToFirst()) {
            Log.e("isDownloadSuccessful", "no such id $id")
            return false
        }

        val status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
        if (status == DownloadManager.STATUS_SUCCESSFUL){
            return true
        }

        return false
    }

    private fun getAmountDownloaded(id: Long): Long {
        val cursor = getCursor(id)

        // moveToFirst returns false if the cursor is empty
        if (!cursor.moveToFirst()) {
            Log.e("getAmountDownloaded", "no such download $id")
            return -1
        }

        return cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
    }

    private fun getQuery(id: Long): DownloadManager.Query {
        val downloadQuery = DownloadManager.Query()
        downloadQuery.setFilterById(id)

        return downloadQuery
    }

    private fun getCursor(id: Long): Cursor {
        return downloadManager.query(getQuery(id))
    }

    override fun onCreate() {

        Log.i("onCreate", "creating")

        // Register broadcast receiver for completed download
        registerReceiver(downloadBroadcastReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        // Update file sizes in hashmap in another thread
        thread {
            for (file in fileToSize) {
                val url = URL(file.key)
                val urlConnection = url.openConnection()

                // Set connect timeout to 500 milliseconds
                urlConnection.connectTimeout = 500

                urlConnection.connect()
                val fileSizeByte = urlConnection.contentLength

                file.setValue(fileSizeByte)
            }
        }

//        thread {
//            while (true) {
//                // Update amount wasted every 1 second in another thread when there is a download
//                Thread.sleep(1000)
//                if (downloadingID != 0L) {
//                    amountWasted.set(getAmountWasted())
//                    Log.i("amountWasted updated", "$amountWasted")
//                }
//            }
//        }

        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder {
        downloadManager = this.getSystemService(DownloadManager::class.java)

        return downloadBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        amountToWaste = intent.getLongExtra("amountToWaste", 0)
        Log.i("onHandleIntent", amountToWaste.toString())

        val largestPossibleFile = getLargestFile()

        enqueueFile(largestPossibleFile)

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        unregisterReceiver(downloadBroadcastReceiver)
        cleanDownload()
        showToast(this, "service destroyed")

        super.onDestroy()
    }

    private fun getLargestFile(): String {

        var file: String = ""
        var size: Int = -1

        for (urlString in downloadURLs) {

            val fileSizeByte = fileToSize[urlString]!!

            // Size of this file has not been or cannot be initialized
            if (fileSizeByte == 0) {
                Log.e("getLargestFile", "$urlString not initialized")
                continue
            }

            if (fileSizeByte == -1) {
                Log.e("getLargestFile", "$urlString unknown size")
                continue
            }

            if (fileSizeByte < amountToWaste - amountWasted.get() && fileSizeByte > size) {
                file = urlString
                size = fileSizeByte
            }
        }

        Log.i("getLargestFile", file)

        return file
    }

    private fun enqueueFile(fileURL: String) {

        // Remainder amount to waste is not enough to download any file
        if (fileURL.isEmpty()) {
            // TODO: maybe update url list from server


            Log.e("enqueueFile", "file url empty")

            // Reset downloading ID to signify no file is currently being downloaded
            return
        }

        // Prepare request to download manager
        val downloadRequest = DownloadManager.Request(Uri.parse(fileURL))

        // Only download in progress visible in notification
        downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverRoaming(false)
            .setVisibleInDownloadsUi(false)
            .setTitle("Wasting data ...")
            .setDestinationInExternalFilesDir(this, "", "/temp")

        downloadingID = downloadManager.enqueue(downloadRequest)

        Log.i("enqueueFile", "enqueued $downloadingID")
    }

    fun cleanDownload() {
        if (downloadingID == 0L) {
            Log.e("cleanDownload", "no file is being downloaded")
            return
        }

        if (downloadManager.remove(downloadingID) == 0) {
            Log.e("cleanDownload", "no such download $downloadingID")
            return
        }

        downloadingID = 0
    }

    fun getAmountWasted(): Long {
        //
        if (downloadingID == 0L) {
            return amountWasted.get()
        }
        return getAmountDownloaded(downloadingID) + amountWasted.get()
    }

}
