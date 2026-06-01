# Épica D: Fundación del MVP (Infraestructura Base)

## 1. Objetivo
Establecer la infraestructura base necesaria para permitir la comunicación entre la capa de React Native (TypeScript) y la capa nativa (Android/Kotlin), habilitando la ejecución de un servidor WebDAV embebido.

## 2. Entregables Técnicos
- **Integración de Ktor:** Configuración exitosa de `ktor-server-core`, `ktor-server-netty` y corrutinas para ejecución no bloqueante.
- **WebDAVService:** Implementación de un `Foreground Service` (tipo `dataSync`) que permite mantener el servidor corriendo mientras la app está en segundo plano.
- **NexusLinkModule:** Bridge nativo configurado para iniciar (`startServer`) y detener (`stopServer`) el servidor desde React Native.
- **Configuración Gradle:** Ajustes de empaquetado para evitar colisiones de dependencias de Netty y habilitación del soporte para Android.

## 3. Decisiones de Arquitectura
- **Servidor:** Se optó por **Ktor (Netty)** por ser ligero y nativo de Kotlin, ideal para ejecutarse dentro de un servicio de Android sin sobrecarga significativa.
- **Comunicación:** Patrón *Bridge* estándar de React Native utilizando `Promises` para llamadas directas y `Event Emitter` para eventos de estado (en desarrollo).
- **Hilo de ejecución:** Se utiliza `Dispatchers.IO` para todas las operaciones de servidor para asegurar que el Hilo Principal (UI Thread) nunca sea bloqueado.

## 4. Estado actual
- [x] Configuración de dependencias `build.gradle` (v2.3.12).
- [x] Resolución de conflictos de compilación en `WebDAVService`.
- [x] Registro de `NexusLinkPackage` en `MainApplication`.
- [x] `MainActivity` configurada.

## 5. Próximos pasos
- Iniciar implementación de rutas WebDAV (`OPTIONS`, `PROPFIND`).
- Implementar el almacenamiento persistente con Storage Access Framework (SAF).