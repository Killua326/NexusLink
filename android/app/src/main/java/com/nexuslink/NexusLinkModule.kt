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

class NexusLinkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var pendingSafPromise: Promise? = null
    private val safManager = SafManager(reactContext)

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
                
                // Pasar la URI persistida de SAF si existe
                val persistedUri = safManager.getPersistedUri()
                if (persistedUri != null) {
                    putExtra("rootUri", persistedUri.toString())
                }
                
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

        pendingSafPromise = promise

        try {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                // Opcional: sugerir una ubicación inicial
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            currentActivity.startActivityForResult(intent, MainActivity.REQUEST_CODE_SAF)
        } catch (e: Exception) {
            pendingSafPromise = null
            promise.reject("SAF_LAUNCH_ERROR", e.message)
        }
    }

    /**
     * Llamado desde MainActivity para procesar el resultado de la selección de carpeta
     */
    fun handleSafResult(uri: Uri?) {
        val promise = pendingSafPromise
        pendingSafPromise = null

        if (uri != null) {
            try {
                // 1. Persistir el permiso usando el SafManager
                safManager.persistPermission(uri)

                // 2. Obtener el nombre de la carpeta (opcional, para feedback al usuario)
                var folderName = "Carpeta seleccionada"
                try {
                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
                    reactApplicationContext.contentResolver.query(documentUri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            folderName = cursor.getString(0) ?: "Carpeta seleccionada"
                        }
                    }
                } catch (e: Exception) {
                    // Ignorar error al obtener nombre y usar fallback
                }
                
                // 3. Responder a React Native
                val map = Arguments.createMap()
                map.putString("uri", uri.toString())
                map.putString("name", folderName)
                promise?.resolve(map)
            } catch (e: Exception) {
                promise?.reject("PERSIST_ERROR", "Error al persistir el permiso: ${e.message}")
            }
        } else {
            promise?.reject("CANCELED", "El usuario canceló la selección")
        }
    }

    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}
}