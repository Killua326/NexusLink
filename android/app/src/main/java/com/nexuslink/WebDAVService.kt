package com.nexuslink

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class WebDAVService : Service() {

    private var server: ApplicationEngine? = null
    private val CHANNEL_ID     = "NexusLinkServiceChannel"
    private val NOTIFICATION_ID = 1
    private val TAG            = "WebDAV"

    private var maxConnections = 10
    private var isReadOnly     = false
    private lateinit var rootDocumentFile: DocumentFile

    private val PROPFIND  = HttpMethod("PROPFIND")
    private val MKCOL     = HttpMethod("MKCOL")
    private val MOVE      = HttpMethod("MOVE")
    private val COPY      = HttpMethod("COPY")
    private val LOCK      = HttpMethod("LOCK")
    private val UNLOCK    = HttpMethod("UNLOCK")
    private val PROPPATCH = HttpMethod("PROPPATCH")

    // =========================================================
    // CICLO DE VIDA
    // =========================================================

    override fun onCreate() { super.onCreate(); createNotificationChannel() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_SERVER" -> {
                val port      = intent.getIntExtra("port", 9999)
                maxConnections = intent.getIntExtra("maxConnections", 10)
                isReadOnly    = intent.getBooleanExtra("isReadOnly", false)
                val uriString = intent.getStringExtra("rootUri")

                rootDocumentFile = if (uriString != null) {
                    DocumentFile.fromTreeUri(this, Uri.parse(uriString))
                        ?: throw IllegalArgumentException("URI SAF inválida")
                } else {
                    val f = java.io.File(getExternalFilesDir(null), "NexusLink_Drive")
                    if (!f.exists()) f.mkdirs()
                    DocumentFile.fromFile(f)
                }

                startForeground(NOTIFICATION_ID, getNotification("Servidor activo en puerto $port"))
                startKtorServer(port)
            }
            "STOP_SERVER" -> stopKtorServer()
        }
        return START_NOT_STICKY
    }

    // =========================================================
    // KTOR
    // =========================================================

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun startKtorServer(requestedPort: Int) {
        serviceScope.launch {
            var port = requestedPort; var started = false
            while (!started && port < requestedPort + 5) {
                try {
                    server = embeddedServer(CIO, port = port) {
                        routing {
                            route("{path...}") {
                                handle {
                                    val path = call.parameters.getAll("path")?.joinToString("/") ?: ""
                                    handleWebDav(call, rootDocumentFile, path)
                                }
                            }
                        }
                    }.start(wait = false)
                    started = true
                } catch (e: Exception) { port++ }
            }
        }
    }

    private fun stopKtorServer() {
        server?.stop(1000L, 2000L); server = null
        stopForeground(true); stopSelf()
    }

    // =========================================================
    // UTILIDADES SAF
    // =========================================================

    private fun getDocumentFileFromPath(root: DocumentFile, path: String): DocumentFile? {
        if (path.isEmpty() || path == "/") return root
        var current = root
        for (part in path.trim('/').split("/").filter { it.isNotEmpty() }) {
            current = current.findFile(part) ?: return null
        }
        return current
    }

    private fun getMimeType(fileName: String): String {
        val ext = fileName.substringAfterLast('.', "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
            ?: "application/octet-stream"
    }

    private fun createSafFileExact(parent: DocumentFile, fileName: String): DocumentFile? {
        val created = parent.createFile(getMimeType(fileName), fileName) ?: return null
        if (created.name != fileName) created.renameTo(fileName)
        return created
    }

    // =========================================================
    // MOVE CROSS-DIRECTORY (fix central del bug)
    // =========================================================

    /**
     * Mueve [src] al nombre [destName] dentro de [destParent].
     *
     * SAF solo permite renameTo() dentro del MISMO directorio padre.
     * Cuando origen y destino tienen padres distintos hay que hacer
     * copia binaria + borrado del origen.
     *
     * Retorna true si la operación tuvo éxito.
     */
    private suspend fun safMove(
        src: DocumentFile,
        destParent: DocumentFile,
        destName: String
    ): Boolean {
        // ¿Mismo padre? Podemos intentar rename directo (más rápido, atómico).
        val srcParentUri = src.uri.toString()
            .substringBeforeLast("%2F")  // URI SAF usa %2F como separador
            .substringBeforeLast("/")

        val destParentUri = destParent.uri.toString()

        val sameParent = srcParentUri == destParentUri ||
                         src.uri.toString().substringBeforeLast("/") == destParent.uri.toString()

        if (sameParent) {
            val renamed = src.renameTo(destName)
            android.util.Log.d(TAG, "safMove sameParent renameTo=$renamed")
            if (renamed) return true
            // Si falla el rename incluso en mismo directorio, caemos a copia
        }

        // Copia + borrado (cross-directory o rename fallido)
        android.util.Log.d(TAG, "safMove via copy+delete: ${src.uri} → ${destParent.uri}/$destName")
        val destFile = createSafFileExact(destParent, destName) ?: return false
        var ok = false
        withContext(Dispatchers.IO) {
            applicationContext.contentResolver.openInputStream(src.uri)?.use { inp ->
                applicationContext.contentResolver.openOutputStream(destFile.uri, "wt")?.use { out ->
                    inp.copyTo(out)
                    out.flush()
                    ok = true
                }
            }
        }
        return if (ok) {
            src.delete()
            true
        } else {
            destFile.delete()  // limpiamos el destino a medias
            false
        }
    }

    // =========================================================
    // CUOTA
    // =========================================================

    private fun getQuotaInfo(): Pair<Long, Long> = try {
        val path  = getExternalFilesDir(null)?.path ?: Environment.getExternalStorageDirectory().path
        val stat  = StatFs(path)
        val block = stat.blockSizeLong
        Pair(stat.availableBlocksLong * block, (stat.blockCountLong - stat.availableBlocksLong) * block)
    } catch (e: Exception) {
        Pair(50L * 1024 * 1024 * 1024, 0L)
    }

    // =========================================================
    // MANEJADOR WEBDAV
    // =========================================================

    private suspend fun handleWebDav(call: ApplicationCall, root: DocumentFile, requestPath: String) {
        val method     = call.request.httpMethod
        val targetFile = getDocumentFileFromPath(root, requestPath)
        val baseUrl    = call.request.path()
        android.util.Log.d(TAG, "${method.value} /$requestPath")

        try {
            when (method) {

                HttpMethod.Options -> {
                    call.response.header("DAV", "1, 2")
                    call.response.header("Allow",
                        "OPTIONS, GET, HEAD, PUT, DELETE, MKCOL, PROPFIND, MOVE, COPY, LOCK, UNLOCK")
                    call.response.header("MS-Author-Via", "DAV")
                    call.respond(HttpStatusCode.OK)
                }

                HttpMethod.Head -> {
                    if (targetFile != null && targetFile.exists()) call.respond(HttpStatusCode.OK)
                    else call.respond(HttpStatusCode.NotFound)
                }

                PROPFIND -> {
                    if (targetFile == null || !targetFile.exists()) {
                        call.respond(HttpStatusCode.NotFound); return
                    }
                    val depth = if ((call.request.header("Depth") ?: "1") == "0") 0 else 1
                    val xml   = buildPropfindXml(targetFile, baseUrl, depth)
                    call.respondText(xml, ContentType.Text.Xml, HttpStatusCode.MultiStatus)
                }

                HttpMethod.Get -> {
                    if (targetFile != null && targetFile.isFile) {
                        val mime = getMimeType(targetFile.name ?: "")
                        call.respondOutputStream(ContentType.parse(mime), HttpStatusCode.OK) {
                            applicationContext.contentResolver
                                .openInputStream(targetFile.uri)?.use { it.copyTo(this) }
                        }
                    } else call.respond(HttpStatusCode.NotFound)
                }

                // ── PUT ──────────────────────────────────────────────────────
                HttpMethod.Put -> {
                    if (isReadOnly) { call.respond(HttpStatusCode.Forbidden); return }

                    val parts      = requestPath.trim('/').split("/").filter { it.isNotEmpty() }
                    if (parts.isEmpty()) { call.respond(HttpStatusCode.MethodNotAllowed); return }

                    val fileName   = parts.last()
                    val parentPath = parts.dropLast(1).joinToString("/")
                    val parentFile = getDocumentFileFromPath(root, parentPath)
                        ?: run { call.respond(HttpStatusCode.Conflict); return }

                    val existing    = parentFile.findFile(fileName)
                    val isOverwrite = existing != null

                    val tempName = ".${fileName}.tmp-${System.currentTimeMillis()}"
                    val tempFile = createSafFileExact(parentFile, tempName)
                        ?: run { call.respond(HttpStatusCode.InternalServerError); return }

                    var writeSuccess = false
                    try {
                        val channel = call.receiveChannel()
                        withContext(Dispatchers.IO) {
                            applicationContext.contentResolver
                                .openOutputStream(tempFile.uri, "wt")?.use { out ->
                                    channel.toInputStream().copyTo(out)
                                    out.flush()
                                    writeSuccess = true
                                }
                        }
                        if (writeSuccess) delay(80)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Error en PUT", e)
                    }

                    if (writeSuccess) {
                        existing?.delete()
                        val renamed = tempFile.renameTo(fileName)
                        if (renamed) {
                            call.respond(if (isOverwrite) HttpStatusCode.NoContent else HttpStatusCode.Created)
                        } else {
                            // rename en mismo directorio falló: copia de emergencia
                            android.util.Log.w(TAG, "PUT renameTo falló, copia de emergencia")
                            val rescue = createSafFileExact(parentFile, fileName)
                            var rescued = false
                            if (rescue != null) {
                                withContext(Dispatchers.IO) {
                                    applicationContext.contentResolver
                                        .openInputStream(tempFile.uri)?.use { inp ->
                                            applicationContext.contentResolver
                                                .openOutputStream(rescue.uri, "wt")?.use { out ->
                                                    inp.copyTo(out); out.flush(); rescued = true
                                                }
                                        }
                                }
                            }
                            tempFile.delete()
                            call.respond(
                                if (rescued) { if (isOverwrite) HttpStatusCode.NoContent else HttpStatusCode.Created }
                                else HttpStatusCode.InternalServerError
                            )
                        }
                    } else {
                        tempFile.delete()
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }

                // ── MKCOL ────────────────────────────────────────────────────
                MKCOL -> {
                    if (isReadOnly) { call.respond(HttpStatusCode.Forbidden); return }
                    if (targetFile != null) { call.respond(HttpStatusCode.MethodNotAllowed); return }

                    val parts      = requestPath.trim('/').split("/").filter { it.isNotEmpty() }
                    val folderName = parts.last()
                    val parentPath = parts.dropLast(1).joinToString("/")
                    val parentFile = getDocumentFileFromPath(root, parentPath)
                        ?: run { call.respond(HttpStatusCode.Conflict); return }

                    val newDir = parentFile.createDirectory(folderName)
                    if (newDir != null) {
                        if (newDir.name != folderName) newDir.renameTo(folderName)
                        call.respond(HttpStatusCode.Created)
                    } else call.respond(HttpStatusCode.Forbidden)
                }

                // ── DELETE ───────────────────────────────────────────────────
                HttpMethod.Delete -> {
                    if (isReadOnly) { call.respond(HttpStatusCode.Forbidden); return }
                    if (targetFile != null) {
                        if (targetFile.delete()) call.respond(HttpStatusCode.NoContent)
                        else call.respond(HttpStatusCode.Forbidden)
                    } else call.respond(HttpStatusCode.NotFound)
                }

                // ── MOVE / COPY ──────────────────────────────────────────────
                MOVE, COPY -> {
                    if (isReadOnly) { call.respond(HttpStatusCode.Forbidden); return }
                    if (targetFile == null) { call.respond(HttpStatusCode.NotFound); return }

                    val destHeader      = call.request.header("Destination")
                        ?: run { call.respond(HttpStatusCode.BadRequest); return }
                    val overwriteHeader = call.request.header("Overwrite") ?: "T"

                    val destUri     = URI(destHeader.replace(" ", "%20"))
                    val relDestPath = destUri.path.removePrefix("/")
                    val destParts   = relDestPath.trim('/').split("/").filter { it.isNotEmpty() }
                    if (destParts.isEmpty()) { call.respond(HttpStatusCode.BadRequest); return }

                    val destName   = destParts.last()
                    val destParent = getDocumentFileFromPath(root, destParts.dropLast(1).joinToString("/"))
                        ?: run { call.respond(HttpStatusCode.Conflict); return }

                    val existingDest = destParent.findFile(destName)
                    val isOverwrite  = existingDest != null

                    if (isOverwrite && overwriteHeader.equals("F", ignoreCase = true)) {
                        call.respond(HttpStatusCode.PreconditionFailed); return
                    }

                    if (method == MOVE) {
                        if (isOverwrite) existingDest?.delete()
                        // Usamos safMove que maneja tanto mismo-directorio como cross-directory
                        val ok = safMove(targetFile, destParent, destName)
                        call.respond(
                            if (ok) { if (isOverwrite) HttpStatusCode.NoContent else HttpStatusCode.Created }
                            else HttpStatusCode.InternalServerError
                        )
                    } else { // COPY
                        if (isOverwrite) existingDest?.delete()
                        val newFile = createSafFileExact(destParent, destName)
                            ?: run { call.respond(HttpStatusCode.InternalServerError); return }
                        var ok = false
                        withContext(Dispatchers.IO) {
                            applicationContext.contentResolver
                                .openInputStream(targetFile.uri)?.use { inp ->
                                    applicationContext.contentResolver
                                        .openOutputStream(newFile.uri, "wt")?.use { out ->
                                            inp.copyTo(out); out.flush(); ok = true
                                        }
                                }
                        }
                        if (ok) call.respond(if (isOverwrite) HttpStatusCode.NoContent else HttpStatusCode.Created)
                        else { newFile.delete(); call.respond(HttpStatusCode.InternalServerError) }
                    }
                }

                // ── LOCK ─────────────────────────────────────────────────────
                LOCK -> {
                    val lockToken = "urn:uuid:${UUID.randomUUID()}"
                    call.response.header("Lock-Token", "<$lockToken>")
                    call.respondText("""<?xml version="1.0" encoding="utf-8" ?>
<D:prop xmlns:D="DAV:">
  <D:lockdiscovery>
    <D:activelock>
      <D:locktype><D:write/></D:locktype>
      <D:lockscope><D:exclusive/></D:lockscope>
      <D:depth>Infinity</D:depth>
      <D:locktoken><D:href>$lockToken</D:href></D:locktoken>
    </D:activelock>
  </D:lockdiscovery>
</D:prop>""", ContentType.Text.Xml, HttpStatusCode.OK)
                }

                UNLOCK   -> call.respond(HttpStatusCode.NoContent)
                PROPPATCH -> {
                    call.receiveText() // consumir el body
                    call.respondText("""<?xml version="1.0" encoding="utf-8" ?>
<D:multistatus xmlns:D="DAV:">
  <D:response>
    <D:href>${call.request.path()}</D:href>
    <D:propstat><D:prop/><D:status>HTTP/1.1 200 OK</D:status></D:propstat>
  </D:response>
</D:multistatus>""", ContentType.Text.Xml, HttpStatusCode.MultiStatus)
                }

                else -> call.respond(HttpStatusCode.MethodNotAllowed)
            }

        } catch (e: SecurityException) {
            android.util.Log.e(TAG, "SecurityException", e)
            call.respond(HttpStatusCode.Forbidden)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error interno", e)
            call.respond(HttpStatusCode.InternalServerError)
        }
    }

    // =========================================================
    // PROPFIND XML
    // =========================================================

    private fun buildPropfindXml(file: DocumentFile, baseUrl: String, depth: Int): String {
        val (avail, used) = getQuotaInfo()
        val dateFmt = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }

        fun encodeHref(p: String): String =
            p.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }

        fun escapeXml(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

        fun node(sb: StringBuilder, f: DocumentFile, href: String) {
            val date    = dateFmt.format(Date(f.lastModified()))
            val encoded = encodeHref(href.removePrefix("/"))
            val finalH  = if (encoded.startsWith("/")) encoded else "/$encoded"
            sb.append("  <D:response>\n    <D:href>$finalH</D:href>\n")
            sb.append("    <D:propstat>\n      <D:prop>\n")
            sb.append("        <D:displayname>${escapeXml(f.name ?: "")}</D:displayname>\n")
            if (f.isDirectory) {
                sb.append("        <D:resourcetype><D:collection/></D:resourcetype>\n")
            } else {
                sb.append("        <D:resourcetype/>\n")
                sb.append("        <D:getcontentlength>${f.length()}</D:getcontentlength>\n")
                sb.append("        <D:getcontenttype>${getMimeType(f.name ?: "")}</D:getcontenttype>\n")
            }
            sb.append("        <D:getlastmodified>$date</D:getlastmodified>\n")
            sb.append("        <D:creationdate>$date</D:creationdate>\n")
            sb.append("      </D:prop>\n      <D:status>HTTP/1.1 200 OK</D:status>\n    </D:propstat>\n")
            // Cuota en propstat separado (RFC 4331) — solo en colecciones
            if (f.isDirectory) {
                sb.append("    <D:propstat>\n      <D:prop>\n")
                sb.append("        <D:quota-available-bytes>$avail</D:quota-available-bytes>\n")
                sb.append("        <D:quota-used-bytes>$used</D:quota-used-bytes>\n")
                sb.append("      </D:prop>\n      <D:status>HTTP/1.1 200 OK</D:status>\n    </D:propstat>\n")
            }
            sb.append("  </D:response>\n")
        }

        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n<D:multistatus xmlns:D=\"DAV:\">\n")
        node(sb, file, baseUrl)
        if (depth > 0 && file.isDirectory) {
            for (child in file.listFiles()) {
                val sep = if (baseUrl.endsWith("/")) "" else "/"
                node(sb, child, "$baseUrl$sep${child.name ?: ""}")
            }
        }
        sb.append("</D:multistatus>")
        return sb.toString()
    }

    // =========================================================
    // NOTIFICACIÓN
    // =========================================================

    private fun getNotification(content: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, packageManager?.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NexusLink WebDAV").setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_save)
            .setContentIntent(pi).setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "WebDAV Server", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}