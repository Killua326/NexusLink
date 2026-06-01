package com.nexuslink

import android.app.Service
import android.content.Intent
import android.os.IBinder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.event.Level
import java.io.File

class WebDAVService : Service() {
    private var server: NettyApplicationEngine? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_SERVER") {
            startKtorServer(intent.getIntExtra("port", 8080))
        } else if (intent?.action == "STOP_SERVER") {
            stopKtorServer()
        }
        return START_STICKY
    }

    private fun startKtorServer(port: Int) {
        server = embeddedServer(Netty, port = port) {
            
            intercept(ApplicationCallPipeline.Monitoring) {
                android.util.Log.d("WebDAV", "${call.request.httpMethod.value} ${call.request.path()}")
            }

            routing {
                get("/") {
                    call.respondText("NexusLink WebDAV Server Running")
                }
            }
        }.start(wait = false)
    }

    private fun stopKtorServer() {
        server?.stop(1000, 2000)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}