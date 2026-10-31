package com.example.freskoapp

import android.util.Log

// Logcat in debug builds, nothing in release. Replaceable because android.util.Log throws in JVM unit tests.
var debugLogSink: (String) -> Unit = if (BuildConfig.DEBUG) { message -> Log.d("Fresko", message) } else { _ -> }

fun debugLog(message: String) = debugLogSink(message)
