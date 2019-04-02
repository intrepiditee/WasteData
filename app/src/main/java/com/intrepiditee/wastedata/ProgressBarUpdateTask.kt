package com.intrepiditee.wastedata

import android.os.AsyncTask
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import com.intrepiditee.wastedata.Utils.Companion.bytesToMegaBytes
import java.lang.ref.WeakReference

class ProgressBarUpdateTask internal constructor(context: MainActivity): AsyncTask<Unit, Long, Unit>() {
    private val activityReference = WeakReference(context)

    private fun setProgress(progress: Long) {
        activityReference.get()?.findViewById<ProgressBar>(R.id.progressBar)?.progress = progress.toInt()
        Log.i("setProgress", progress.toString())
    }

    private fun setAmountWasted(amountWasted: Long) {

        // If amountWasted is less than 0, set text to empty string.
        val text = if (amountWasted < 0L) "" else amountWasted.toString()
        activityReference.get()?.findViewById<TextView>(R.id.amountWasted)?.text = text
    }

    private fun setAmountToWaste(amountToWaste: Long) {
        val text = if (amountToWaste < 0L) "" else amountToWaste.toString()
        activityReference.get()?.findViewById<TextView>(R.id.amountToWaste)?.text = text
    }

    private fun setSeparator(isToSeparator: Boolean) {
        val text = if (isToSeparator) "/" else "Not Started"
        activityReference.get()?.findViewById<TextView>(R.id.separator)?.text = text
    }

    override fun onPreExecute() {

        // Clear progress bar.
        setProgress(0)
        setAmountWasted(0)
        setSeparator(true)
        setAmountToWaste(0)
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

    override fun onCancelled(result: Unit?) {
        setProgress(-1)
        setAmountWasted(-1)
        setSeparator(false)
        setAmountToWaste(-1)
    }

    override fun onProgressUpdate(vararg values: Long?) {
        if (!isCancelled) {
            setProgress(values[0]!!)
            setAmountWasted(values[1]!!)
            setAmountToWaste(values[2]!!)
        }
    }

}