package com.nexuslink

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import android.util.Log

class NexusLinkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        var pendingSafPromise: Promise? = null
    }

    private val safManager = SafManager(reactContext)

    // FIX: Las firmas de ActivityEventListener no llevan '?' en la versión actual de RN.
    // activity, data e intent son non-nullable según la interfaz actual.
    private val activityEventListener = object : ActivityEventListener {
        override fun onActivityResult(activity: Activity, requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode == MainActivity.REQUEST_CODE_SAF) {
                val uri: Uri? = if (resultCode == Activity.RESULT_OK) data?.data else null
                handleSafResult(uri)
            }
        }

        override fun onNewIntent(intent: Intent) {
            // No necesario para este módulo
        }
    }

    override fun getName() = "NexusLinkModule"

    init {
        reactApplicationContext.addActivityEventListener(activityEventListener)
    }

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
        try { reactApplicationContext.unregisterReceiver(eventReceiver) } catch (e: Exception) {}
        try { reactApplicationContext.removeActivityEventListener(activityEventListener) } catch (e: Exception) {}
    }

    @ReactMethod
    fun startServer(port: Int, promise: Promise) {
        try {
            val activity = reactApplicationContext.currentActivity
            val context = activity ?: reactApplicationContext
            val intent = Intent(context, WebDAVService::class.java).apply {
                putExtra("port", port)
                putExtra("maxConnections", 10)
                putExtra("isReadOnly", false)
                val persistedUri = safManager.getPersistedUri()
                if (persistedUri != null) putExtra("rootUri", persistedUri.toString())
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
        val persistedUri = safManager.getPersistedUri()
        map.putBoolean("isRunning", false)
        map.putString("ipAddress", null)
        map.putInt("activeConnections", 0)
        map.putString("errorMessage", null)
        if (persistedUri != null) {
            map.putString("persistedUri", persistedUri.toString())
            map.putString("persistedName", "Carpeta seleccionada")
        } else {
            map.putString("persistedUri", null)
            map.putString("persistedName", null)
        }
        promise.resolve(map)
    }

    @ReactMethod
    fun pickRootDirectory(promise: Promise) {
        val currentActivity = reactApplicationContext.currentActivity
        if (currentActivity == null) {
            promise.reject("ACTIVITY_NOT_FOUND", "No se encontró una actividad activa")
            return
        }
        NexusLinkModule.pendingSafPromise = promise
        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            currentActivity.startActivityForResult(intent, MainActivity.REQUEST_CODE_SAF)
        } catch (e: Exception) {
            NexusLinkModule.pendingSafPromise = null
            promise.reject("SAF_LAUNCH_ERROR", e.message)
        }
    }

    private fun handleSafResult(uri: Uri?) {
        val promise = NexusLinkModule.pendingSafPromise
        NexusLinkModule.pendingSafPromise = null

        if (promise == null) {
            Log.e("NexusLinkModule", "No hay una promesa pendiente para resolver el resultado SAF")
            return
        }

        if (uri != null) {
            try {
                safManager.persistPermission(uri)
                var folderName = "Carpeta seleccionada"
                try {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                        uri, DocumentsContract.getTreeDocumentId(uri)
                    )
                    reactApplicationContext.contentResolver.query(
                        documentUri,
                        arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                        null, null, null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) folderName = cursor.getString(0) ?: "Carpeta seleccionada"
                    }
                } catch (e: Exception) { /* usar fallback */ }

                val map = Arguments.createMap()
                map.putString("uri", uri.toString())
                map.putString("name", folderName)
                reactApplicationContext.runOnJSQueueThread { promise.resolve(map) }
            } catch (e: Exception) {
                reactApplicationContext.runOnJSQueueThread {
                    promise.reject("PERSIST_ERROR", "Error al persistir el permiso: ${e.message}")
                }
            }
        } else {
            reactApplicationContext.runOnJSQueueThread {
                promise.reject("CANCELED", "El usuario canceló la selección")
            }
        }
    }

    @ReactMethod fun addListener(eventName: String) {}
    @ReactMethod fun removeListeners(count: Int) {}
}