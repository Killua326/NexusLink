package com.nexuslink

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

  companion object {
    const val REQUEST_CODE_SAF = 999
  }

  override fun getMainComponentName(): String = "NexusLink"

  // CORRECCIÓN ESTÁNDAR: El tercer parámetro activa Fabric y delega la inicialización de la vista
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

  // OBLIGATORIO para evitar que Android intente recrear fragmentos rotos en cache
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(null)
    checkAndRestoreSafPermissions()
  }

  private fun checkAndRestoreSafPermissions() {
    val safManager = SafManager(this)
    val persistedUri = safManager.getPersistedUri()
    
    if (persistedUri != null) {
        Log.d("MainActivity", "URI persistida encontrada: $persistedUri")
    } else {
        Log.d("MainActivity", "No se encontró URI persistida o el permiso ha expirado.")
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
  }
}
