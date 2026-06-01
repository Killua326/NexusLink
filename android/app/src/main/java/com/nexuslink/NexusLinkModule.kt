package com.nexuslink

import com.facebook.react.bridge.*
import android.content.Intent

class NexusLinkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "NexusLinkModule"

    @ReactMethod
    fun startServer(port: Int, promise: Promise) {
        val intent = Intent(reactApplicationContext, WebDAVService::class.java).apply {
            putExtra("port", port)
            action = "START_SERVER"
        }
        reactApplicationContext.startForegroundService(intent)
        promise.resolve("Server started")
    }

    @ReactMethod
    fun stopServer(promise: Promise) {
        val intent = Intent(reactApplicationContext, WebDAVService::class.java).apply {
            action = "STOP_SERVER"
        }
        reactApplicationContext.startService(intent)
        promise.resolve("Server stopped")
    }
}