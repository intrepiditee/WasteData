package com.intrepiditee.wastedata

import android.os.AsyncTask
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.intrepiditee.wastedata.Utils.Companion.bytesToMegaBytes
import java.lang.ref.WeakReference

class ProgressBarUpdateTask internal constructor(context: MainActivity): AsyncTask<Unit, Long, Unit>() {

    companion object {
        val NOT_STARTED = 0
        val IN_PROGRESS = 1
        val FINISHED = 2
        val CANCELLED = 3;

    }

    private val activityReference = WeakReference(context)

    private fun setProgress(progress: Long) {
        activityReference.get()?.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress.toInt()
        Log.i("setProgress", progress.toString())
    }

    private fun setAmountWasted(amountWasted: Long) {
        activityReference.get()?.findViewById<TextView>(R.id.amountWasted)?.text = amountWasted.toString()
    }

    private fun setAmountToWaste(amountToWaste: Long) {
        activityReference.get()?.findViewById<TextView>(R.id.amountToWaste)?.text = amountToWaste.toString()
    }

    private fun setStatus(status: Int) {
        var text = "Not Started"
        when (status) {
            NOT_STARTED -> text = "Not Started"
            IN_PROGRESS -> text = "In Progress"
            FINISHED -> text = "Finished"
            CANCELLED -> text = "Cancelled"
            else -> Log.e("setStatus", "unknown status code")
        }
        activityReference.get()?.findViewById<TextView>(R.id.status)?.text = text
    }

    override fun onPreExecute() {

        // Clear progress bar.
        setProgress(0)
        setAmountWasted(0)
        setAmountToWaste(0)
        setStatus(NOT_STARTED)
    }

    override fun doInBackground(vararg params: Unit) {

        // Wait for download to start.
        while (activityReference.get() != null && (!isCancelled) && activityReference.get()?.downloadService?.isStarted == false) {Thread.sleep(1000)}

        // Compute and publish progress.
        while (activityReference.get() != null && !isCancelled) {
            val progress: Long = activityReference.get()?.downloadService?.getProgress() ?: 0
            var amountWasted: Long = activityReference.get()?.downloadService?.getAmountWastedMegaBytes() ?: 0
            val amountToWaste: Long = bytesToMegaBytes(activityReference.get()?.downloadService?.amountToWaste ?: 0)
            if (progress == 100L) {
                amountWasted = amountToWaste
            }

            publishProgress(progress, amountWasted, amountToWaste)

            // Wasting completes. Stop updating progress bar. Exit the task.
            if (progress == 100L) {
                return
            }

            Thread.sleep(1000)
        }
    }

    override fun onPostExecute(result: Unit?) {
        setStatus(FINISHED)
    }
    override fun onCancelled(result: Unit?) {
        setStatus(CANCELLED)
    }

    override fun onProgressUpdate(vararg values: Long?) {
        if (!isCancelled) {
            setProgress(values[0]!!)
            setAmountWasted(values[1]!!)
            setAmountToWaste(values[2]!!)
            setStatus(IN_PROGRESS)
        }
    }

}