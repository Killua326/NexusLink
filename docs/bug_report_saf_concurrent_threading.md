# Reporte de Diagnóstico: React Native Bridgeless / Concurrent Threading Race Condition

## 1. Identificación del Error (Causa Raíz)

Analizando detalladamente el archivo `crash_log_20260625_233631.txt`, observamos que:
1. No se registra ninguna excepción `FATAL EXCEPTION` en el `AndroidRuntime` del logcat filtrado.
2. La UI de SurfaceFlinger se mantiene actualizando constantemente a 60 fps (`fps=60.95 dur=1000.80`).
3. La aplicación de React Native se queda congelada visualmente con el indicador de carga (*"Cargando..."*).

### ¿Por qué ocurre?
Bajo la **Nueva Arquitectura de React Native (Bridgeless / Fabric con React 18)**, el renderizado de la interfaz de usuario se procesa en hilos concurrentes diferentes. El módulo nativo `NexusLinkModule` resuelve la promesa usando:

```kotlin
promise?.resolve(map)
```

Sin embargo, las llamadas al puente de React Native bajo el modo Bridgeless se encolan de manera asíncrona. Si la llamada de resolución (`resolve`) ocurre en un hilo diferente al hilo principal de JS (o antes de que el motor de JS esté listo para recibirlo por transiciones de la actividad), la respuesta de la promesa se "pierde" silenciosamente o no dispara el callback en TypeScript debido a un **Thread Sync Issue** (Problema de Sincronización de Hilos).

En la Nueva Arquitectura de React Native, todas las interacciones con el puente de UI o la resolución de promesas que afecten directamente el estado de componentes visuales deben ser despachadas en el **JS Thread** o el **UI Thread** de React Native de forma explícita para evitar bloqueos del planificador concurrente.

---

## 2. Solución Propuesta

Para garantizar que la promesa nativa se resuelva de manera confiable e instantánea y que la UI de React Native salga inmediatamente del estado "Cargando", debemos despachar la resolución de la promesa (`promise?.resolve`) de vuelta al hilo de ejecución correcto de React Native.

Podemos forzar la ejecución en el contexto seguro de React usando:

```kotlin
reactApplicationContext.runOnJSQueueThread {
    promise?.resolve(map)
}
```

O en su defecto, en el hilo principal de UI de Android (que es donde React Native sincroniza su bucle de eventos nativos):

```kotlin
reactApplicationContext.runOnUiQueueThread {
    promise?.resolve(map)
}
```

La estrategia más segura y recomendada por Facebook en la Nueva Arquitectura de React Native es despachar la resolución de promesas asíncronas de actividades dentro del **JS Queue Thread** para asegurar que el motor de JavaScript procese la microtarea inmediatamente, eliminando el cuelgue visual.

---

## 3. Implementación de la Solución

Modificaremos `handleSafResult` en `NexusLinkModule.kt` para envolver las resoluciones y rechazos de la promesa en `reactApplicationContext.runOnJSQueueThread { ... }`.
