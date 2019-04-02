package com.intrepiditee.wastedata

import android.content.Context
import android.widget.Toast

class Utils {
    companion object {
        fun showToast(context: Context, stringToShow: String) {
            Toast.makeText(context, stringToShow, Toast.LENGTH_SHORT).show()
        }

        fun bytesToMegaBytes(bytes: Long): Long {
            return bytes / 1000000
        }

        fun megaBytesToBytes(megaBytes: Long): Long {
            return megaBytes * 1000000
        }
    }
}