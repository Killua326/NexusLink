package com.nexuslink

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class NexusLinkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "NexusLinkModule"

    // Receptor de eventos desde el WebDAVService
    private val eventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.nexuslink.SERVER_EVENT") {
                val type = intent.getStringExtra("type")
                if (type == "CONNECTION_COUNT") {
                    val count = intent.getIntExtra("count", 0)
                    reactApplicationContext
                        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                        .emit("onConnectionCountUpdated", count)
                }
            }
        }
    }

    override fun initialize() {
        super.initialize()
        val filter = IntentFilter("com.nexuslink.SERVER_EVENT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reactApplicationContext.registerReceiver(eventReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            reactApplicationContext.registerReceiver(eventReceiver, filter)
        }
    }

    override fun invalidate() {
        super.invalidate()
        try {
            reactApplicationContext.unregisterReceiver(eventReceiver)
        } catch (e: Exception) {
            // Ignorar si no estaba registrado
        }
    }

    @ReactMethod
    fun startServer(port: Int, promise: Promise) {
        try {
            val activity = reactApplicationContext.currentActivity
            val context = activity ?: reactApplicationContext
            val intent = Intent(context, WebDAVService::class.java).apply {
                putExtra("port", port)
                // Opcional: pasaríamos maxConnections y isReadOnly aquí desde React Native
                putExtra("maxConnections", 10) 
                putExtra("isReadOnly", false)
                action = "START_SERVER"
            }
            context.startForegroundService(intent)
            promise.resolve("Server started")
        } catch (e: Exception) {
            promise.reject("START_ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopServer(promise: Promise) {
        try {
            val intent = Intent(reactApplicationContext, WebDAVService::class.java).apply {
                action = "STOP_SERVER"
            }
            reactApplicationContext.startService(intent)
            promise.resolve("Server stopped")
        } catch (e: Exception) {
            promise.reject("STOP_ERROR", e.message)
        }
    }

    @ReactMethod
    fun getServerStatus(promise: Promise) {
        val map = Arguments.createMap()
        map.putBoolean("isRunning", false)
        map.putString("ipAddress", null)
        map.putInt("activeConnections", 0)
        map.putString("errorMessage", null)
        promise.resolve(map)
    }

    @ReactMethod
    fun pickRootDirectory(promise: Promise) {
        val map = Arguments.createMap()
        map.putString("uri", "mock_uri")
        map.putString("name", "Carpeta Interna (Mock)")
        promise.resolve(map)
    }

    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}
}