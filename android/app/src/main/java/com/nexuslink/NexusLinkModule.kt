package com.nexuslink

import com.facebook.react.bridge.*
import android.content.Intent

class NexusLinkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName() = "NexusLinkModule"

    @ReactMethod
    fun startServer(port: Int, promise: Promise) {
        try {
            val intent = Intent(reactApplicationContext, WebDAVService::class.java).apply {
                putExtra("port", port)
                action = "START_SERVER"
            }
            reactApplicationContext.startForegroundService(intent)
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

    // =========================================================================
    // MÉTODOS FALTANTES DEL CONTRATO (Stubs para Épica C)
    // =========================================================================
    
    @ReactMethod
    fun getServerStatus(promise: Promise) {
        // Devolvemos el ServerState inicial para que TS no falle al abrir la app
        val map = Arguments.createMap()
        map.putBoolean("isRunning", false) 
        map.putString("ipAddress", null)
        map.putInt("activeConnections", 0)
        map.putString("errorMessage", null)
        promise.resolve(map)
    }

    @ReactMethod
    fun pickRootDirectory(promise: Promise) {
        // Stub simulado. La implementación real de SAF se hará en la Épica E.
        val map = Arguments.createMap()
        map.putString("uri", "content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload")
        map.putString("name", "Carpeta Simulada (Stub)")
        promise.resolve(map)
    }

    // =========================================================================
    // MÉTODOS OBLIGATORIOS PARA NativeEventEmitter (React Native 0.71+)
    // =========================================================================
    
    @ReactMethod
    fun addListener(eventName: String) {
        // Requerido por el contrato del Bridge. Mantener vacío.
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        // Requerido por el contrato del Bridge. Mantener vacío.
    }
}