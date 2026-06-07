package com.nexuslink

import android.os.Bundle
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

  override fun getMainComponentName(): String = "NexusLink"

  // CORRECCIÓN ESTÁNDAR: El tercer parámetro activa Fabric y delega la inicialización de la vista
  override fun createReactActivityDelegate(): ReactActivityDelegate =
      DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)

  // OBLIGATORIO para evitar que Android intente recrear fragmentos rotos en cache
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(null)
  }
}
