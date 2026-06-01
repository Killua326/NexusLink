# Épica C: Infraestructura Nativa (Bridge & Service) - FINALIZADA

## Objetivo
Establecer la comunicación bidireccional entre la capa de JavaScript y el entorno nativo (Kotlin) y garantizar que el servicio WebDAV sea persistente.

## Estado Final
- [x] **Comunicación:** Bridge (`NexusLinkModule`) implementado con Promises.
- [x] **Servicio:** `WebDAVService` implementado como `Foreground Service`.
- [x] **Android 14 Compatibility:** Declaración de `foregroundServiceType="dataSync"` y permisos asociados.
- [x] **Persistencia:** Notificación persistente implementada (`startForeground`) para evitar cierre por parte del OS.
- [x] **Build:** Packaging configurado para resolver conflictos de Netty (`INDEX.LIST`).

## Notas de Implementación Críticas
- **Notificación:** El servicio utiliza un canal de baja importancia (`IMPORTANCE_LOW`) para no interrumpir al usuario, pero es obligatorio para mantener el proceso vivo.
- **Ruta de Servicio:** Se corrigió la referencia en el `AndroidManifest` para apuntar a `com.nexuslink.WebDAVService` directamente.