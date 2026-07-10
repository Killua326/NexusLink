# Reporte de Crasheo en Selección de Carpeta SAF y Solución Propuesta

## 1. Análisis del Error (Crash Log)

Al presionar el botón "Usar esta carpeta" en el selector nativo de SAF (Storage Access Framework) y confirmar el permiso, la aplicación se cierra de forma inesperada lanzando la siguiente excepción fatal en el hilo principal de Android:

```text
java.lang.RuntimeException: Failure delivering result ResultInfo{who=null, request=999, result=-1, data=Intent { dat=content://com.android.externalstorage.documents/... flg=0xc3 }} to activity {com.nexuslink/com.nexuslink.MainActivity}: java.lang.UnsupportedOperationException: ReactInstanceManager.createReactContext is unsupported.
	at android.app.ActivityThread.deliverResults(ActivityThread.java:5388)
    ...
Caused by: java.lang.UnsupportedOperationException: ReactInstanceManager.createReactContext is unsupported.
	at com.facebook.react.ReactInstanceManager.<init>(ReactInstanceManager.java:298)
	at com.facebook.react.ReactInstanceManagerBuilder.build(ReactInstanceManagerBuilder.kt:334)
	at com.facebook.react.ReactNativeHost.createReactInstanceManager(ReactNativeHost.java:97)
	at com.facebook.react.ReactNativeHost.getReactInstanceManager(ReactNativeHost.java:67)
	at com.nexuslink.MainActivity.onActivityResult(MainActivity.kt:48)
```

### ¿Por qué ocurre?
El crasheo se origina en `MainActivity.kt` dentro del método `onActivityResult()`:
```kotlin
val reactContext = reactNativeHost.reactInstanceManager.currentReactContext
val nexusModule = reactContext?.getNativeModule(NexusLinkModule::class.java)
```

En la versión actual de React Native (con la Nueva Arquitectura y Fabric activos en el proyecto), la obtención síncrona del `ReactInstanceManager` a través de `reactNativeHost.reactInstanceManager` en momentos de transición de actividades (como el retorno de `DocumentsUI` a `MainActivity`) dispara un intento forzado de creación de contexto heredado mediante `createReactContext`. 

Debido a que este flujo síncrono está explícitamente deshabilitado y no soportado en el motor moderno de React Native, se lanza un `UnsupportedOperationException`, bloqueando la entrega del resultado y provocando el crash de la app.

---

## 2. Solución Propuesta (Estrategia Limpia de React Native)

Para solucionar esto de manera robusta y definitiva, debemos **eliminar la dependencia manual de `MainActivity` sobre el `ReactInstanceManager`** para buscar el módulo nativo.

En su lugar, utilizaremos el patrón estándar y recomendado por el equipo de React Native: **`ActivityEventListener`**.

### Plan de Acción:
1. **Hacer que `NexusLinkModule` implemente `ActivityEventListener`**:
   Esto le otorga al propio módulo nativo de Kotlin la capacidad de recibir de forma autónoma los eventos de `onActivityResult` del sistema operativo Android.
   
2. **Registrar el listener en `initialize()` y removerlo en `invalidate()` (o constructor)**:
   Al registrar el listener de manera directa en el contexto de React, Android y el puente de React Native distribuirán automáticamente el resultado a nuestro módulo sin intermediación manual.

3. **Simplificar `MainActivity.kt`**:
   `MainActivity` solo llamará a `super.onActivityResult()`. Ya no tendrá que buscar la referencia de `NexusLinkModule` ni interactuar con `reactInstanceManager`, eliminando por completo la línea de código problemática y garantizando la compatibilidad con Fabric.

---

## 3. Beneficios de la Solución
- **Robustez**: Resuelve el crash de inmediato al no invocar APIs no soportadas bajo la Nueva Arquitectura.
- **Desacoplamiento**: `MainActivity` ya no necesita conocer detalles de implementación de `NexusLinkModule`.
- **Elegancia**: Adhiere a las directrices de arquitectura oficiales para módulos nativos de Android en React Native.
