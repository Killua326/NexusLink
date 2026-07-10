# Reporte de Diagnóstico de Error: Estado "Cargando" Infinito tras SAF

## 1. Identificación del Error (Causa Raíz)

Tras corregir el *crash* anterior, la aplicación ahora entra en un estado de **"Cargando" (Loading) infinito** tras seleccionar un directorio en el selector nativo SAF.

Analizando el comportamiento del código en `NexusLinkModule.kt`:

```kotlin
    fun handleSafResult(uri: Uri?) {
        val promise = pendingSafPromise
        pendingSafPromise = null
        // ...
        promise?.resolve(map)
        // ...
    }
```

### ¿Por qué ocurre?
El problema no es un crash, sino una **omisión lógica de UI en React Native**. 

1. `handleSafResult` es llamado correctamente desde `MainActivity`.
2. La `promise` se resuelve con éxito.
3. El store de Zustand (`src/services/serverStore.ts`) recibe el resultado en `pickRootDirectory`.
4. El estado `isLoading` se vuelve `false` en `serverStore.ts` (bloque `finally`).

Sin embargo, el Dashboard de React Native (`src/ui/Dashboard.tsx`) **no está escuchando o sincronizando el estado `config.rootDirectoryUri` con el estado `isRunning`** de manera automática para refrescar la UI una vez que el `serverStore` es actualizado.

Más importante aún, cuando el servidor no está corriendo, la resolución de la promesa SAF está actualizando el estado `config` y `selectedFolderName` en el store, pero no está disparando una actualización *completa* de la UI en la vista del Dashboard.

Pero el problema principal detectado en el comportamiento es: **El Dashboard no dispara `getServerStatus` o una actualización de estado de vuelta desde nativo tras el éxito de SAF**. La UI queda esperando una confirmación que el store recibió pero no propagó visualmente.

---

## 2. Solución Propuesta

1. **Forzar actualización en `serverStore.ts`**: Asegurarnos de que cuando `pickRootDirectory` se resuelve, también se dispare una lógica de refresco o se asegure que el estado UI (`isLoading`) esté en `false` consistentemente.
2. **Mejorar el Bridge de comunicación**: Actualmente, el Dashboard depende de `selectedFolderName`. Si este campo se actualiza, el componente debería re-renderizar, pero si el Dashboard está en un estado *stale*, no detecta el cambio.
3. **Debug de Logcat**: Al no haber excepciones en `AndroidRuntime`, el sistema está funcionando pero la UI de React Native no está recibiendo la señal de fin de carga. Vamos a añadir un `console.log` en el store para verificar que `isLoading` llega a `false`.

---

## 3. Implementación de la Solución

Vamos a modificar `src/services/serverStore.ts` para asegurar que el `isLoading` se maneje correctamente y añadir logs de depuración para rastrear por qué la UI no reacciona al cambio de estado tras el `pickRootDirectory`.
