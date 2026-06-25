# Plan de Implementación: Épica E (SAF y Persistencia)

Este documento detalla la integración del Storage Access Framework para la gestión de directorios raíz con permisos persistentes.

### Paso 1: Implementación de SAF y Persistencia en Kotlin
* Crear `SafManager` para gestionar `ACTION_OPEN_DOCUMENT_TREE`.
* Implementar `takePersistableUriPermission`.
* Guardar la URI persistente en `SharedPreferences`.
* Añadir logs detallados en cada paso del ciclo de vida del permiso.

### Paso 2: Estructura en NexusLinkModule.kt (Bridge)
* Exponer `pickRootDirectory()` como método Promise.
* Manejar el resultado de la Activity en `MainActivity` y notificar al Bridge.

### Paso 3: Recuperación al Reinicio
* Implementar lógica en `onCreate` del servicio/app para verificar si existe una URI guardada y si sus permisos aún son válidos.

### Paso 4: Integración UI y Errores
* Enlazar botón de configuración en React Native.
* Manejar el estado de "Carpeta no seleccionada" y el ciclo de vida del permiso.