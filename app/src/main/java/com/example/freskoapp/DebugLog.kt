package com.example.freskoapp

import android.util.Log

// Debug builds only: release never logs.
fun debugLog(message: String) {
    if (BuildConfig.DEBUG) Log.d("Fresko", message)
}
