package com.nexuslink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class WebDAVService : Service() {
    
    private var server: ApplicationEngine? = null
    private val CHANNEL_ID = "NexusLinkServiceChannel"
    private val NOTIFICATION_ID = 1 // ID constante para poder actualizar o quitar la misma

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SERVER" -> {
                // Iniciar el servicio en primer plano inmediatamente
                startForeground(NOTIFICATION_ID, getNotification("Servidor activo"))
                startKtorServer(intent.getIntExtra("port", 8080))
            }
            "STOP_SERVER" -> {
                stopKtorServer()
            }
        }
        return START_NOT_STICKY // Cambiado a NOT_STICKY para evitar reinicios automáticos indeseados
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "WebDAV Server", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(content: String): Notification {
        val launchIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusLink")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pendingIntent)
            // Si quieres que el usuario PUEDA quitar la notificación al detener el servidor,
            // setOngoing debe ser false, pero al ser un Foreground Service, 
            // el sistema exige que sea visible. 
            // La clave es que al llamar stopForeground, el sistema la quite.
            .setOngoing(true) 
            .build()
    }

    private fun startKtorServer(port: Int) {
        // 1. Limpieza de seguridad: Destruir el "fantasma" si el puerto quedó trabado por un crasheo previo
        server?.stop(1000L, 2000L)

        // 2. Iniciar el servidor
        try {
            server = embeddedServer(CIO, port = port) {
                routing {
                    get("/") {
                        call.respondText("NexusLink WebDAV Server Running")
                    }
                }
            }.start(wait = false)
        } catch (e: Exception) {
            android.util.Log.e("WebDAV", "Error iniciando Ktor: ${e.message}")
        }
    }

    private fun stopKtorServer() {
        android.util.Log.d("WebDAV", "Deteniendo servidor...")
        server?.stop(1000L, 2000L)
        server = null
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
        android.util.Log.d("WebDAV", "Servicio detenido.")
    }

    override fun onBind(intent: Intent?): IBinder? = null

}