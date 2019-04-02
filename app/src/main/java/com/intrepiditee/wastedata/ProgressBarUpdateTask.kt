package com.intrepiditee.wastedata

import android.os.AsyncTask
import android.util.Log
import android.widget.ProgressBar
import java.lang.ref.WeakReference

class ProgressBarUpdateTask internal constructor(context: MainActivity): AsyncTask<Unit, Int, Unit>() {
    private val activityReference = WeakReference(context)

    private fun setProgress(progress: Int?) {
        activityReference.get()?.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress ?: 0
        Log.i("setProgress", progress.toString())
    }

    override fun onPreExecute() {

        // Clear progress bar.
        setProgress(0)
    }

    override fun doInBackground(vararg params: Unit) {

        // Wait for download to start.
        while (activityReference.get() != null && (!isCancelled) && activityReference.get()?.downloadService?.isStarted == false) {Thread.sleep(1000)}

        // Compute and publish progress.
        while (activityReference.get() != null && !isCancelled) {
            val progress: Int? = activityReference.get()?.downloadService?.getProgress()
            publishProgress(progress)

            // Wasting completes. Stop updating progress bar.
            if (progress == 100) {
                return
            }

            Thread.sleep(1000)
        }
    }

    override fun onCancelled(result: Unit?) {
       setProgress(0)
    }

    override fun onProgressUpdate(vararg values: Int?) {
        if (!isCancelled) {
            setProgress(values[0])
        }
    }

}