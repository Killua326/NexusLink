# Reporte de Diagnóstico: "Congelamiento" en el Selector SAF

## 1. Identificación del Error (Causa Raíz)

Analizando los logs proporcionados y el flujo de ejecución, el "cargando infinito" no es un bug de la UI, sino un **problema de ciclo de vida del `Promise` en el bridge nativo**.

### El Problema: "Promesa Perdida"
1. `pickRootDirectory` guarda la `promise` en `pendingSafPromise`.
2. El usuario selecciona la carpeta y Android devuelve el resultado.
3. Se invoca `handleSafResult(uri)`.
4. **Error:** Si por cualquier motivo (re-creación de actividad, pausa de ciclo de vida, o race condition en el hilo de React) la instancia de `NexusLinkModule` que recibió la llamada `handleSafResult` **NO** es la misma que guardó la promesa en `pendingSafPromise` (o si la actividad se recreó), la `promise` original se pierde o queda "colgando" (nunca resuelta).

El log muestra un flujo constante de `SurfaceFlinger` y `BufferQueueProducer`, lo que indica que **la UI de React Native está viva y renderizando** (60 fps), pero la *acción* `pickRootDirectory` nunca recibió el `resolve` desde el módulo nativo.

---

## 2. Solución Propuesta

Para resolver esto de forma robusta, no podemos depender de una variable de instancia (`pendingSafPromise`) en un módulo nativo que puede ser recreado o cuya asociación con la actividad puede volverse inestable.

**La Solución:** 
Usar un mecanismo de **Activity Lifecycle Listener** para re-asociar la actividad correctamente y, sobre todo, **garantizar la persistencia de la promesa** incluso si la actividad cambia. 

Una forma más segura es mover la lógica de SAF a un `ReactActivityLifecycleEventListener` registrado correctamente en el módulo nativo. Sin embargo, para no complicar en exceso, la solución inmediata y efectiva es:

1. **Asegurar que la promesa sea persistente**: Actualmente, si `NexusLinkModule` se recrea (por ejemplo, al volver de la actividad de selección de documentos), `pendingSafPromise` se inicializa como `null`.
2. **Uso de un Singleton para el estado de la promesa**: La promesa pendiente debe sobrevivir a la recreación de la instancia del módulo nativo.

---

## 3. Implementación de la Solución

Vamos a convertir el almacenamiento de la `pendingSafPromise` en un **objeto estático (Companion Object)** para que persista durante todo el tiempo de vida del proceso de la aplicación, independientemente de que la instancia del módulo nativo sea recreada por el puente.

### Paso a seguir: Modificar `NexusLinkModule.kt`

```kotlin
class NexusLinkModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    companion object {
        // Objeto estático para asegurar que la promesa sobrevive a la recreación del módulo
        var pendingSafPromise: Promise? = null
    }
    // ... el resto del código
}
```
Y en `handleSafResult`, referenciar a `NexusLinkModule.pendingSafPromise`.
