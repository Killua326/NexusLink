package com.nexuslink

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager

class NexusLinkPackage : ReactPackage {
    
    // Aquí es donde registramos tu módulo nativo
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(NexusLinkModule(reactContext))
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}