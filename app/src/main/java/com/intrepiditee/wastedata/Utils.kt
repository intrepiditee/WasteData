package com.intrepiditee.wastedata

import android.content.Context
import android.widget.Toast

class Utils {
    companion object {
        fun showToast(context: Context, stringToShow: String) {
            Toast.makeText(context, stringToShow, Toast.LENGTH_SHORT).show()
        }
    }
}