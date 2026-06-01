package com.nexuslink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*

class WebDAVService : Service() {
    private var server: NettyApplicationEngine? = null
    private val CHANNEL_ID = "NexusLinkServiceChannel"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_SERVER") {
            // Requerimiento obligatorio: Mostrar notificación al iniciar
            val notification = getNotification("Servidor WebDAV iniciado")
            startForeground(1, notification)
            
            startKtorServer(intent.getIntExtra("port", 8080))
        } else if (intent?.action == "STOP_SERVER") {
            stopKtorServer()
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "NexusLink Server",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    private fun getNotification(content: String): Notification {
        // Usamos un icono estándar de Android para evitar errores
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusLink WebDAV")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    private fun startKtorServer(port: Int) {
        server = embeddedServer(Netty, port = port) {
            routing {
                get("/") {
                    call.respondText("NexusLink WebDAV Server Running")
                }
            }
        }.start(wait = false)
    }

    private fun stopKtorServer() {
        server?.stop(1000, 2000)
        stopForeground(true)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}