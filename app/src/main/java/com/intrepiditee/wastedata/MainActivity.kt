package com.intrepiditee.wastedata

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.icu.util.UniversalTimeScale.toLong
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import com.intrepiditee.wastedata.Utils.Companion.megaBytesToBytes
import com.intrepiditee.wastedata.Utils.Companion.showToast
import java.lang.Thread.sleep
import java.lang.ref.WeakReference


class MainActivity : AppCompatActivity() {

    lateinit var downloadService: DownloadService
    private var isBound: Boolean = false
    private lateinit var progressBarUpdateTask: ProgressBarUpdateTask

    private val downloaServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val downloadBinder = service as DownloadService.DownloadBinder
            downloadService = downloadBinder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Create and bind to the download service, but not start it
        bindDownloadService()
    }

    private fun bindDownloadService() {
        val downloadIntent = Intent(this, DownloadService::class.java)
        bindService(downloadIntent, downloaServiceConnection,
            BIND_NOT_FOREGROUND or BIND_AUTO_CREATE)
    }

    fun startWasting(view: View) {
        if (downloadService.isStarted) {
            showToast(this, "Error: already started.")
            return
        }

        val inputText = findViewById<EditText>(R.id.inputField).text

        // Check if input is empty string
        if (inputText.isBlank()) {
            showToast(this, "Error: please specify number of MB to waste.")
            return
        }

        // Convert input text to long
        val inputAmount = Integer.parseInt(inputText.trim().toString()).toLong()

        if (inputAmount < 0) {
            showToast(this, "Error: please enter a positive number")
            return
        }

        // Initialize download intent
        val downloadIntent = Intent(this, DownloadService::class.java)

        // Convert number of MB to number of B
        downloadIntent.putExtra("amountToWaste", megaBytesToBytes(inputAmount))

        // Start the download service after connection is successful
        if (isBound) {
            startService(downloadIntent)

            // Get a new task every time.
            progressBarUpdateTask = ProgressBarUpdateTask(this)
            progressBarUpdateTask.execute()
            showToast(this, "Starting: scheduled to waste $inputAmount MB.")

        } else {
            bindDownloadService()
            showToast(this, "Service rebond: please try again")
        }

    }

    override fun onRestart() {
        // TODO: check if service still running
        // TODO: fetch newest amount wasted
        Log.i("onRestart", "restarting")
        super.onRestart()
    }

    override fun onDestroy() {
        Log.i("onDestory", "destroying")
        stopWasting()
        unbindService(downloaServiceConnection)
        super.onDestroy()
    }

    override fun onStop() {
        Log.i("onStop", "stopping")
        super.onStop()
    }

    override fun onBackPressed() {
        moveTaskToBack(false)
    }

    fun stopWastingOnClick(view: View) {
        stopWasting()
    }

    // Stop progress bar thread and call stopWasting on the service
    private fun stopWasting() {
        // TODO: Fetch amount wasted from service

        if (!downloadService.isStarted) {
            showToast(this, "Error: not started yet.")
            return
        }

        progressBarUpdateTask.cancel(true)
        val tempAmountWasted = downloadService.getAmountWastedMegaBytes()
        showToast(this, "Stopped: wasted $tempAmountWasted MB.")
        downloadService.stopWasting()
    }
}


