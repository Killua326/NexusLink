package com.nexuslink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class WebDAVService : Service() {
    
    private var server: ApplicationEngine? = null
    private val CHANNEL_ID = "NexusLinkServiceChannel"
    private val NOTIFICATION_ID = 1

    // Controladores de estado
    private var activeConnections = AtomicInteger(0)
    private var maxConnections = 10
    private var isReadOnly = false
    private lateinit var rootDir: File

    // Métodos WebDAV Extendidos
    private val PROPFIND = HttpMethod("PROPFIND")
    private val MKCOL = HttpMethod("MKCOL")
    private val MOVE = HttpMethod("MOVE")
    private val COPY = HttpMethod("COPY")
    private val LOCK = HttpMethod("LOCK")
    private val UNLOCK = HttpMethod("UNLOCK")
    private val PROPPATCH = HttpMethod("PROPPATCH")

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // Crear carpeta temporal segura para pruebas antes de la Épica E (SAF)
        rootDir = File(getExternalFilesDir(null), "NexusLink_Drive")
        if (!rootDir.exists()) rootDir.mkdirs()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SERVER" -> {
                val port = intent.getIntExtra("port", 9999)
                maxConnections = intent.getIntExtra("maxConnections", 10)
                isReadOnly = intent.getBooleanExtra("isReadOnly", false)
                
                startForeground(NOTIFICATION_ID, getNotification("Servidor activo en puerto $port"))
                startKtorServer(port)
            }
            "STOP_SERVER" -> stopKtorServer()
        }
        return START_NOT_STICKY
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun startKtorServer(requestedPort: Int) {
        android.util.Log.d("WebDAV", "Intentando iniciar servidor en puerto $requestedPort...")

        serviceScope.launch {
            var currentPort = requestedPort
            var serverStarted = false
            val maxRetries = 5

            while (!serverStarted && currentPort < requestedPort + maxRetries) {
                try {
                    // CAMBIO: wait = false
                    server = embeddedServer(CIO, port = currentPort) {
                        routing {
                            // CAMBIO: Se usa {path...} en lugar de {...}
                            route("{path...}") {
                                handle {
                                    // CAMBIO: Capturamos "path" explícitamente
                                    val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
                                    handleWebDav(call, rootDir, path)
                                }
                            }
                        }
                    }.start(wait = false) 
                    
                    serverStarted = true
                    android.util.Log.d("WebDAV", "Servidor iniciado exitosamente en puerto $currentPort")
                    
                } catch (e: Exception) {
                    // Si el puerto está en uso, intentamos el siguiente
                    android.util.Log.w("WebDAV", "Puerto $currentPort ocupado o error: ${e.message}")
                    currentPort++
                }
            }
        }
    }

    private suspend fun handleWebDav(call: ApplicationCall, baseDir: File, requestPath: String) {
    val targetFile = if (requestPath.isEmpty()) baseDir else File(baseDir, requestPath)
        val method = call.request.httpMethod
        val baseUrl = call.request.path()

        when (method) {
            HttpMethod.Options -> {
                call.response.header("DAV", "1, 2")
                call.response.header("Allow", "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, PROPFIND, MOVE, COPY, LOCK, UNLOCK")
                call.response.header("MS-Author-Via", "DAV")
                call.respond(HttpStatusCode.OK)
            }
            HttpMethod.Head -> {
                if (targetFile.exists()) call.respond(HttpStatusCode.OK)
                else call.respond(HttpStatusCode.NotFound)
            }
            PROPFIND -> {
                if (!targetFile.exists()) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val depthHeader = call.request.header("Depth") ?: "infinity"
                    val depth = if (depthHeader == "0") 0 else 1
                    
                    val xmlResponse = buildPropfindXml(targetFile, baseUrl, depth)
                    call.respondText(xmlResponse, ContentType.Text.Xml, HttpStatusCode.MultiStatus)
                }
            }
            HttpMethod.Get -> {
                if (targetFile.exists() && targetFile.isFile) {
                    call.respondFile(targetFile)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            HttpMethod.Put -> {
                if (isReadOnly) return call.respond(HttpStatusCode.Forbidden)
                
                try {
                    val channel = call.receiveChannel()
                    targetFile.outputStream().use { output ->
                        channel.toInputStream().copyTo(output)
                    }
                    // Notificar a Android de la existencia del nuevo archivo
                    MediaScannerConnection.scanFile(applicationContext, arrayOf(targetFile.absolutePath), null, null)
                    call.respond(HttpStatusCode.Created)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            MKCOL -> {
                if (isReadOnly) return call.respond(HttpStatusCode.Forbidden)
                
                if (targetFile.exists()) {
                    call.respond(HttpStatusCode.MethodNotAllowed)
                } else if (!targetFile.parentFile!!.exists()) {
                    call.respond(HttpStatusCode.Conflict)
                } else {
                    if (targetFile.mkdir()) call.respond(HttpStatusCode.Created)
                    else call.respond(HttpStatusCode.Forbidden)
                }
            }
            HttpMethod.Delete -> {
                if (isReadOnly) return call.respond(HttpStatusCode.Forbidden)
                
                if (targetFile.exists()) {
                    if (targetFile.deleteRecursively()) call.respond(HttpStatusCode.NoContent)
                    else call.respond(HttpStatusCode.Forbidden)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
            MOVE, COPY -> {
                if (isReadOnly) return call.respond(HttpStatusCode.Forbidden)
                
                val destHeader = call.request.header("Destination") ?: return call.respond(HttpStatusCode.BadRequest)
                try {
                    val destUri = URI(destHeader)
                    val relativeDestPath = destUri.path.removePrefix("/")
                    val destFile = File(baseDir, relativeDestPath)

                    if (!targetFile.exists()) return call.respond(HttpStatusCode.NotFound)
                    
                    if (method == MOVE) {
                        if (targetFile.renameTo(destFile)) call.respond(HttpStatusCode.Created)
                        else call.respond(HttpStatusCode.Forbidden)
                    } else {
                        targetFile.copyRecursively(destFile, overwrite = true)
                        call.respond(HttpStatusCode.Created)
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
            LOCK -> {
                // Token falso para satisfacer al Finder de macOS
                val lockToken = "urn:uuid:${UUID.randomUUID()}"
                call.response.header("Lock-Token", "<$lockToken>")
                val lockXml = """<?xml version="1.0" encoding="utf-8" ?>
                |<D:prop xmlns:D="DAV:">
                |  <D:lockdiscovery>
                |    <D:activelock>
                |      <D:locktype><D:write/></D:locktype>
                |      <D:lockscope><D:exclusive/></D:lockscope>
                |      <D:depth>Infinity</D:depth>
                |      <D:locktoken><D:href>$lockToken</D:href></D:locktoken>
                |    </D:activelock>
                |  </D:lockdiscovery>
                |</D:prop>""".trimMargin()
                call.respondText(lockXml, ContentType.Text.Xml, HttpStatusCode.OK)
            }
            UNLOCK, PROPPATCH -> {
                call.respond(HttpStatusCode.NoContent)
            }
            else -> {
                call.respond(HttpStatusCode.MethodNotAllowed)
            }
        }
    }

    // ==========================================
    // UTILIDADES: XML Y BROADCASTS
    // ==========================================

    private fun buildPropfindXml(file: File, baseUrl: String, depth: Int): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n")
        sb.append("<D:multistatus xmlns:D=\"DAV:\">\n")

        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

        fun encodePath(path: String): String {
            return path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        }

        fun appendFileNode(f: File, hrefPath: String) {
            val date = dateFormat.format(Date(f.lastModified()))
            val cleanHref = if (!hrefPath.startsWith("/")) "/$hrefPath" else hrefPath
            val encodedHref = encodePath(cleanHref)

            sb.append("  <D:response>\n")
            sb.append("    <D:href>$encodedHref</D:href>\n")
            sb.append("    <D:propstat>\n")
            sb.append("      <D:prop>\n")
            sb.append("        <D:displayname><![CDATA[${f.name}]]></D:displayname>\n")
            if (f.isDirectory) {
                sb.append("        <D:resourcetype><D:collection/></D:resourcetype>\n")
            } else {
                sb.append("        <D:resourcetype/>\n")
                sb.append("        <D:getcontentlength>${f.length()}</D:getcontentlength>\n")
                sb.append("        <D:getcontenttype>application/octet-stream</D:getcontenttype>\n")
            }
            sb.append("        <D:getlastmodified>$date</D:getlastmodified>\n")
            sb.append("      </D:prop>\n")
            sb.append("      <D:status>HTTP/1.1 200 OK</D:status>\n")
            sb.append("    </D:propstat>\n")
            sb.append("  </D:response>\n")
        }

        appendFileNode(file, baseUrl)

        if (depth > 0 && file.isDirectory) {
            val children = file.listFiles() ?: emptyArray()
            for (child in children) {
                val separator = if (baseUrl.endsWith("/")) "" else "/"
                appendFileNode(child, baseUrl + separator + child.name)
            }
        }

        sb.append("</D:multistatus>")
        return sb.toString()
    }

    private fun broadcastConnectionCount(count: Int) {
        val intent = Intent("com.nexuslink.SERVER_EVENT").apply {
            putExtra("type", "CONNECTION_COUNT")
            putExtra("count", count)
        }
        sendBroadcast(intent)
    }

    // ==========================================
    // CICLO DE VIDA
    // ==========================================

    private fun stopKtorServer() {
        try {
            server?.stop(500L, 1000L) // Cierre rápido
            server = null
            android.util.Log.d("WebDAV", "Servidor detenido correctamente")
        } catch (e: Exception) {
            android.util.Log.e("WebDAV", "Error al detener el servidor", e)
        } finally {
            stopForeground(true)
            stopSelf()
        }
    }

    private fun getNotification(content: String): Notification {
        val launchIntent = packageManager?.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusLink")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "WebDAV Server", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}