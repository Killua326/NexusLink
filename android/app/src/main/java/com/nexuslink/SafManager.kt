package com.nexuslink

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log

/**
 * Gestiona el Storage Access Framework (SAF) y la persistencia de permisos de directorio.
 */
class SafManager(private val context: Context) {
    private val TAG = "SAF_DEBUG"
    private val PREFS_NAME = "NexusLinkPrefs"
    private val KEY_ROOT_URI = "root_uri"

    /**
     * Persiste el permiso de acceso a una URI obtenida via ACTION_OPEN_DOCUMENT_TREE.
     */
    fun persistPermission(uri: Uri) {
        try {
            val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            
            // Persistir permiso en el ContentResolver
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            
            // Guardar URI en SharedPreferences para recuperación posterior
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_ROOT_URI, uri.toString()).apply()
            
            Log.d(TAG, "Permiso persistido exitosamente para la URI: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Error al persistir permiso SAF para la URI: $uri", e)
        }
    }

    /**
     * Recupera la URI persistida si los permisos siguen siendo válidos.
     */
    fun getPersistedUri(): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriString = prefs.getString(KEY_ROOT_URI, null) ?: return null
        
        val uri = Uri.parse(uriString)
        
        return if (isPermissionValid(uri)) {
            Log.d(TAG, "URI persistida recuperada y válida: $uri")
            uri
        } else {
            Log.w(TAG, "URI persistida encontrada pero los permisos ya no son válidos: $uri")
            // Limpiar si ya no es válido
            clearPersistedUri()
            null
        }
    }

    /**
     * Verifica si la URI proporcionada tiene permisos persistentes válidos.
     */
    fun isPermissionValid(uri: Uri): Boolean {
        val persistedPermissions = context.contentResolver.persistedUriPermissions
        return persistedPermissions.any { it.uri == uri && it.isReadPermission && it.isWritePermission }
    }

    /**
     * Elimina la URI guardada en preferencias.
     */
    fun clearPersistedUri() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ROOT_URI).apply()
        Log.d(TAG, "URI persistida eliminada de las preferencias.")
    }
}
