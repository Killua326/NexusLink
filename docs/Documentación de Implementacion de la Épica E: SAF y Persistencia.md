# Implementación del Sistema de Gestión de Almacenamiento (SAF)

Se ha implementado un sistema robusto de gestión de almacenamiento basado en el Storage Access Framework (SAF), permitiendo a NexusLink mantener el acceso a directorios seleccionados por el usuario incluso después de cierres forzados o reinicios del dispositivo.

## Componentes Clave Desarrollados
### SafManager.kt
Clase central que encapsula toda la lógica de SAF. Gestiona la solicitud de permisos persistentes (`takePersistableUriPermission`), su almacenamiento en `SharedPreferences` y la validación en tiempo real (`isPermissionValid`) para asegurar que el acceso no haya expirado.

### Bridge Nativo (NexusLinkModule.kt)
Expone a React Native la capacidad de lanzar el selector de directorios y, crucialmente, inyecta la URI persistida automáticamente al iniciar el servicio `WebDAVService`, eliminando la necesidad de que el usuario re-seleccione la carpeta manualmente en cada sesión.

### Integración UI y Estado (serverStore.ts)
Se añadió lógica en el store global para que, al arrancar la aplicación, esta consulte al módulo nativo si existe una URI previamente autorizada. Esto permite que el Dashboard UI refleje instantáneamente el nombre y estado de la carpeta raíz cargada.
### Recuperación al Ciclo de Vida (MainActivity.kt)
Se implementó una rutina de chequeo en `onCreate` que asegura la integridad de los permisos desde el inicio del proceso de la aplicación, mejorando la fiabilidad del servicio `WebDAV`.

### Capa de Datos en WebDAV (WebDAVService.kt)
El servidor ahora procesa la URI recibida (ya sea desde el selector en tiempo real o recuperada de la persistencia) para montar el `DocumentFile` raíz, permitiendo operaciones de WebDAV sobre el árbol de documentos seleccionado con plena seguridad de acceso.

Esta arquitectura cumple con los estándares de seguridad de Android (Scoped Storage) y asegura una experiencia de usuario fluida, donde la configuración del almacenamiento es persistente y autogestionada por la aplicación.