# Plan: NexusLink — Android + Ktor (Versión Oficial)

**Documento de referencia oficial para la implementación del MVP de NexusLink.**  
*Última actualización: 31 de mayo de 2026*

---

## Resumen Ejecutivo

NexusLink es una aplicación Android-first que permite acceder al almacenamiento seleccionado de un dispositivo Android desde cualquier ordenador en la misma red Wi‑Fi a través de WebDAV (sin cliente instalado, sin consumo de datos móviles).

- **Frontend:** React Native + TypeScript (Dashboard único).
- **Backend:** Kotlin + Ktor (servidor WebDAV embebido).
- **Storage:** Storage Access Framework (SAF) + permisos persistentes.
- **Protocolo:** HTTP/WebDAV — RFC 4918 compatible.
- **Ejecución:** Android Foreground Service.
- **Alcance:** Android only; iOS dormido.
- **Duración estimada:** 6 semanas (MVP).

---

## 1. Objetivo y Scope

### Objetivo Principal
Entregar un MVP Android que permita a un ordenador en la misma red Wi‑Fi acceder (leer/escribir/crear/borrar) al almacenamiento seleccionado del dispositivo Android vía WebDAV, ejecutando el servidor como un Foreground Service robusto, y exponiendo un bridge TS↔Kotlin para control total desde la UI.

### Alcance del MVP
- **Plataforma:** Android only (iOS sin cambios operativos).
- **Backend:** Ktor embebido en Kotlin.
- **Frontend:** React Native + TypeScript (Dashboard único minimalista).
- **Storage:** SAF con permisos persistentes tras reinicio.
- **Protocolos HTTP:**
  - Semana 3: `OPTIONS`, `PROPFIND`, `GET` (prioridad).
  - Semana 6: `PUT`, `DELETE`, `MKCOL`, límites de concurrencia.
- **Seguridad:** MVP sin autenticación; Basic Auth opcional en roadmap.
- **Red:** HTTP local (cleartext), sin TLS en MVP.

### Out of Scope (Roadmap futuro)
- Autenticación Basic Auth.
- HTTPS/TLS.
- iOS (mantener como referencia).
- Streaming de archivos grandes > 500 MB (validar en sprint de rendimiento).
- Multi-usuario / compartición de sesiones.

---

## 2. Épicas

### Épica A — Fundación del producto Android
Crear la estructura base del repositorio, decisiones arquitectónicas críticas y configuraciones Android para soportar el servidor WebDAV embebido.

**Entregables:**
- Estructura `src/` con capas de presentación, dominio e infraestructura.
- Scripts `pnpm` para build, lint y test.
- `AndroidManifest.xml` con permisos necesarios.
- `build.gradle` con dependencias Ktor, SAF y bridge nativo.
- Documento de decisiones técnicas (Ktor confirmado, estrategia del bridge).

---

### Épica B — UI React Native y estado global
Construir el Dashboard UI del MVP y centralizar el estado de la aplicación.

**Entregables:**
- Modelos TypeScript: `ServerConfig`, `ServerState`.
- Dashboard (`src/ui/Dashboard.tsx`) con indicador de estado, IP activa, botón iniciar/detener.
- Modal de configuración (puerto, directorio raíz, modo lectura/escritura).
- Store global (Zustand o Redux) con acciones asíncronas.
- Manejo visual de errores y estados intermedios.

---

### Épica C — Native Bridge TS↔Kotlin
Definir e implementar el puente de comunicación entre TypeScript y Kotlin.

**Entregables:**
- Contrato de promesas: `startServer(config)`, `stopServer()`, `getServerStatus()`, `pickRootDirectory()`.
- Contrato de eventos: `onServerStateChanged`, `onConnectionCountUpdated`, `onServerError`.
- Módulo nativo Kotlin que implemente promesas y emita eventos.
- Serialización estable de `Uri`, estados, errores.
- Documentación interna del contrato.

---

### Épica D — Servidor WebDAV con Ktor
Implementar el servidor HTTP/WebDAV embebido con soporte inicial para métodos clave y compatibilidad con clientes de escritorio.

**Entregables:**
- Ktor embebido arrancando en el puerto configurado.
- Métodos `OPTIONS`, `PROPFIND`, `GET` (Semana 3).
- Métodos `PUT`, `DELETE`, `MKCOL` (Semana 6).
- Respuestas `207 Multi-Status` con XML correcto.
- Cabeceras `DAV`, `Allow`, manejo de recursos.
- Control de concurrencia y códigos `503` en exceso.
- Manejo de errores `403` para solo lectura.

---

### Épica E — SAF y capa de almacenamiento
Integrar el Storage Access Framework para seleccionar y gestionar el directorio raíz.

**Entregables:**
- Activity Result picker para seleccionar carpeta raíz.
- Persistencia de permisos con `takePersistableUriPermission()`.
- Almacenamiento seguro en `SharedPreferences` o `DataStore`.
- Mapeo de rutas WebDAV a `DocumentFile`/`ContentResolver`.
- Respeto del modo `isReadOnly` a nivel de capa de datos.
- Recovery de permisos al reiniciar la app.

---

### Épica F — Foreground Service y ciclo de vida
Crear un servicio persistente que garantice ejecución del servidor sin interrupciones.

**Entregables:**
- `ForegroundService` que aloje el servidor Ktor.
- Declaración en `AndroidManifest.xml` con `foregroundServiceType` correcto.
- Notificación persistente con acciones (Abrir app / Detener servidor).
- Sincronización de estado entre servicio y RN.
- Manejo de ciclo de vida: arranque, pausa, reinicio, detención.
- Limpieza de recursos al detener.

---

### Épica G — Validación y compatibilidad
Ejecutar pruebas exhaustivas contra clientes reales de escritorio y asegurar RFC 4918.

**Entregables:**
- Smoke tests: conectar desde PC, listar raíz, crear/leer/escribir archivo.
- Validación con Windows Explorer.
- Validación con macOS Finder.
- Validación con cliente Linux (Nautilus / Caja).
- Ajustes de XML/headers conforme a comportamientos observados.
- Build `assembleDebug` estable y reproducible.
- Base sólida para auth futura.

---

## 3. Historias Técnicas (por épica)

### Épica A — Fundación del producto Android

**A.1** Crear estructura `src/` con capas (presentación, dominio, infraestructura).
- Reemplazar template por shell de NexusLink.
- Definir entrypoints y módulos principales.

**A.2** Configurar scripts `pnpm` para build, lint, test y validación Android.
- Script para compilar APK.
- Script para linters (ESLint, TypeScript).

**A.3** Ajustar `android/app/src/main/AndroidManifest.xml`.
- Permisos: `INTERNET`, `ACCESS_NETWORK_STATE`, `RECORD_AUDIO` (si aplica), `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`.
- Soporte para cleartext traffic (HTTP en red local).
- Placeholder para `ForegroundService`.

**A.4** Preparar `android/app/build.gradle`.
- Dependencias Ktor (`io.ktor:ktor-server-core`, `io.ktor:ktor-server-android`).
- Kotlin stdlib y coroutines.
- SAF, AndroidX.

**A.5** Documentar decisiones técnicas (Ktor, bridge nativo, auth scope).

---

### Épica B — UI React Native y estado global

**B.1** Modelar `ServerConfig` y `ServerState` en TypeScript.
- Interfaz `ServerConfig`: puerto, URI raíz, nombre raíz, modo lectura/escritura, máx conexiones.
- Interfaz `ServerState`: running, IP, conexiones activas, error.

**B.2** Implementar Dashboard (`src/ui/Dashboard.tsx`).
- Logo/branding centrado.
- Tarjeta de instrucciones.
- Indicador de estado (círculo gris/verde pulsante).
- Texto de estado dinámico.
- Botón "INICIAR SERVIDOR" (azul) / "DETENER SERVIDOR" (blanco).
- Botón ⚙ "Configuración del Servidor".

**B.3** Implementar modal de configuración.
- Selector de puerto.
- Selector de raíz (SAF picker).
- Toggle modo lectura/escritura.
- Guardar y aplicar.

**B.4** Crear store global (Zustand o Redux).
- Estado centralizado de servidor.
- Acciones: `startServer`, `stopServer`, `getStatus`, `pickFolder`.
- Sincronización con eventos nativos.

**B.5** Manejar errores visibles y transiciones de estado.

---

### Épica C — Native Bridge TS↔Kotlin

**C.1** Definir API de promesas en TypeScript.
```typescript
startServer(config: ServerConfig): Promise<boolean>
stopServer(): Promise<boolean>
getServerStatus(): Promise<ServerState>
pickRootDirectory(): Promise<{ uri: string; name: string }>
```

**C.2** Definir API de eventos en TypeScript.
```typescript
onServerStateChanged: (listener: (state: ServerState) => void) => void
onConnectionCountUpdated: (listener: (count: number) => void) => void
onServerError: (listener: (error: { code: string; message: string }) => void) => void
```

**C.3** Crear módulo nativo Kotlin con promesas stub.
- Exportar métodos accesibles desde JS.
- Emitir eventos hacia JS.

**C.4** Unificar serialización de datos entre capas.
- JSON serializable para todos los payloads.

**C.5** Documentar contrato del bridge (wiki o `BRIDGE.md`).

---

### Épica D — Servidor WebDAV con Ktor

**D.1** Integrar Ktor en módulo nativo; inicializar servidor.
- Puerto configurable.
- Escucha en `0.0.0.0:puerto`.

**D.2** Implementar `OPTIONS` y `PROPFIND` (Semana 3).
- Respuestas RFC 4918 compatibles con Windows Explorer.
- XML `207 Multi-Status`.
- Cabeceras `DAV: 1, 2`.

**D.3** Implementar `GET` (Semana 3).
- Lectura de archivos desde raíz.
- Headers correctos (`Content-Type`, `Content-Length`).

**D.4** Implementar `PUT`, `DELETE`, `MKCOL` (Semana 6).
- Creación de directorios.
- Escritura de archivos.
- Borrado (respetando `isReadOnly`).

**D.5** Aplicar control de concurrencia.
- Limitar conexiones simultáneas (configurable `maxConnections`).
- Rechazar con `503` si se excede.

**D.6** Manejo de errores y códigos HTTP.
- `403` para operaciones prohibidas (solo lectura).
- `404` para recursos no encontrados.
- `409` para conflictos (crear en raíz inexistente).

---

### Épica E — SAF y capa de almacenamiento

**E.1** Implementar picker SAF via Activity Result.
- Abrir selector nativo de carpetas.
- Devolver `{ uri: string; name: string }`.

**E.2** Persistir permisos con `takePersistableUriPermission()`.
- Almacenar URI en `SharedPreferences` o `DataStore`.
- Recuperar al arrancar la app.

**E.3** Traducir rutas WebDAV a `DocumentFile`/`ContentResolver`.
- Abstracción para listar archivos.
- Abstracción para leer/escribir.
- Validación de rutas (evitar path traversal).

**E.4** Aplicar modo `isReadOnly` transversalmente.
- Bloquear `PUT`, `DELETE`, `MKCOL` si `isReadOnly=true`.
- Permitir `GET`, `OPTIONS`, `PROPFIND`.

**E.5** Recuperación graceful de permisos perdidos.
- Instruir al usuario si permisos caducaron.
- Ofrecer opción de re-seleccionar carpeta.

---

### Épica F — Foreground Service y ciclo de vida

**F.1** Crear `ForegroundService` que aloje el servidor Ktor.
- Declarar en `AndroidManifest.xml`.
- Usar `foregroundServiceType="dataSync"` o `connectedDevice`.

**F.2** Implementar notificación persistente.
- Mostrar mientras servidor esté activo.
- Acciones: "Abrir NexusLink", "Detener servidor".

**F.3** Sincronizar estado servicio ↔ RN.
- Emitir eventos `onServerStateChanged` al cambiar estado.
- Actualizar conteo de conexiones en tiempo real.

**F.4** Manejar ciclo de vida robusto.
- `onCreate`: restaurar estado anterior si aplica.
- `onStartCommand`: arrancar servidor.
- `onDestroy`: limpiar recursos.
- Proteger contra killbacks del sistema (use `START_STICKY`).

**F.5** Gestión de recursos.
- Liberar puertos y sockets al detener.
- Cerrar `DocumentFile` handles.

---

### Épica G — Validación y compatibilidad

**G.1** Crear suite de smoke tests básicos.
- Conectar desde PC a la IP local.
- Listar raíz con `PROPFIND`.
- Leer archivo con `GET`.

**G.2** Validar con Windows Explorer.
- Mapear unidad de red.
- Crear/editar/borrar archivos.
- Ajustar XML/headers según comportamiento.

**G.3** Validar con macOS Finder.
- Conectar servidor WebDAV.
- Operaciones básicas.

**G.4** Validar con cliente Linux.
- Nautilus / Caja / Dolphin.

**G.5** Ajustes de compatibilidad.
- Refinar `PROPFIND` response.
- Revisar cabeceras y códigos de estado.
- Tester con archivos grandes (100+ MB).

**G.6** Build y release readiness.
- `assembleDebug` reproducible.
- Linters pasando (ESLint, Ktlint).
- Pruebas unitarias core (bridge, SAF, Ktor routing).

---

## 4. Roadmap por semanas

### Semana 1 — Fundaciones y decisiones
**Objetivos:**
- Establecer estructura base del proyecto.
- Confirmar decisiones técnicas (Ktor, bridge).
- Preparar configuración Android.

**Entregables:**
- ✅ Estructura `src/` creada.
- ✅ Scripts `pnpm` funcionales.
- ✅ `AndroidManifest.xml` con permisos base.
- ✅ `build.gradle` con dependencias Ktor.
- ✅ Documento de decisiones técnicas.

**Validación:**
- `pnpm start` y `pnpm run android` sin errores.

---

### Semana 2 — UI y bridge inicial
**Objetivos:**
- Dashboard operativo con store global.
- Módulo nativo esqueleto con promesas stub.

**Entregables:**
- ✅ Dashboard UI completo (mockup visual funcional).
- ✅ Store global (Zustand/Redux).
- ✅ Modelos TypeScript (`ServerConfig`, `ServerState`).
- ✅ Módulo nativo con promesas simuladas.
- ✅ Eventos nativos wired a store.

**Validación:**
- UI muestra estado, botón iniciar/detener funciona (eventos simulados).
- Logs de promesas en JS y respuestas Kotlin.

---

### Semana 3 — Servidor Ktor mínimo
**Objetivos:**
- Ktor encendido y respondiendo métodos clave.
- Compatibilidad inicial con Windows Explorer.

**Entregables:**
- ✅ Ktor embebido arrancando.
- ✅ `OPTIONS` respondiendo correctamente.
- ✅ `PROPFIND` con XML `207`.
- ✅ `GET` leyendo archivos raíz.
- ✅ IP local expuesta en UI.

**Validación:**
- PC puede hacer `OPTIONS /` y recibir cabeceras DAV.
- PC puede hacer `PROPFIND /` y ver listado en XML.
- PC puede descargar archivo con `GET`.

---

### Semana 4 — SAF y almacenamiento
**Objetivos:**
- Selector de carpeta raíz funcional.
- Operaciones de lectura mapeadas a SAF.

**Entregables:**
- ✅ Activity Result picker para SAF.
- ✅ Persistencia de permisos.
- ✅ `DocumentFile` abstractions.
- ✅ Listado de archivos raíz vía `PROPFIND`.
- ✅ Lectura de archivos vía `GET`.

**Validación:**
- Usuario selecciona carpeta en app.
- Tras reiniciar, permisos se recuperan.
- `PROPFIND` lista archivos de la raíz seleccionada.

---

### Semana 5 — Foreground Service y eventos reales
**Objetivos:**
- Servicio persistente operativo.
- Eventos reales sincronizados con UI.

**Entregables:**
- ✅ `ForegroundService` creado y declarado.
- ✅ Notificación persistente visible.
- ✅ Eventos reales `onServerStateChanged`, `onConnectionCountUpdated`.
- ✅ UI refleja conteo de conexiones en tiempo real.
- ✅ App en background, servidor sigue activo.

**Validación:**
- Servicio visible en configuración del sistema.
- Notificación persiste al minimizar app.
- Eventos llevan a UI correctamente.
- Servidor sigue respondiendo si app está minimizada.

---

### Semana 6 — WebDAV completo y validación final
**Objetivos:**
- Métodos `PUT`, `DELETE`, `MKCOL` operativos.
- Validación exhaustiva con clientes reales.
- Build estable.

**Entregables:**
- ✅ `PUT` escribiendo archivos (respetando `isReadOnly`).
- ✅ `DELETE` borrando archivos/carpetas.
- ✅ `MKCOL` creando directorios.
- ✅ Límites de concurrencia y códigos `503`.
- ✅ Validación Windows Explorer.
- ✅ Validación macOS Finder.
- ✅ Validación Linux client.
- ✅ Build `assembleDebug` reproducible.

**Validación:**
- PC crea/edita/borra archivos en modo lectura/escritura.
- PC recibe `403` en modo solo lectura.
- Conectar múltiples clients y observar `503` al exceder `maxConnections`.
- Smoke tests pasando.

---

## 5. Decisiones críticas y spikes

### Prioridad 1 — Spike WebDAV Compatibility
**Qué:** Validar exactamente qué XML, cabeceras y códigos de estado exigen Windows Explorer y macOS Finder.  
**Cuándo:** Semana 1-2 (antes de cerrar `PROPFIND`).  
**Dueño:** Lead Backend.  
**Resultado esperado:** Documento con formato XML exacto, cabeceras requeridas, edge cases.

### Prioridad 2 — Estrategia del Bridge Nativo
**Opciones:**
- Módulo puente clásico (recomendado para MVP).
- TurboModule / New Architecture (más complejo, para futuro).

**Decisión:** Módulo clásico en MVP para entrega rápida.  
**Documento:** `android/app/src/main/java/com/nexuslink/NexusLinkModule.kt`.

### Prioridad 3 — SAF Persistence Strategy
**Qué:** Confirmar cómo almacenar y recuperar URIs persistentes tras reinicio.  
**Decisión:** `SharedPreferences` (simple) o `DataStore` (recomendado, más futuro-proof).  
**Test:** Reiniciar app y verificar acceso a carpeta sin nuevo picker.

### Prioridad 4 — Auth Scope
**MVP:** Sin autenticación.  
**Futuro:** Basic Auth opcional (roadmap Q3 2026).  
**Nota:** Windows XP bloquea Basic Auth sobre HTTP; documentar limitación.

### Prioridad 5 — Cleartext HTTP Allowlist
**Qué:** Permitir HTTP en red local (127.0.0.1, 192.168.x.x, etc.).  
**Archivo:** `android/app/src/main/res/xml/network_security_config.xml`.  
**Versión Android:** Aplicable Android 9+.

---

## 6. Primeros archivos a modificar (puntos de entrada)

Prioridad de toque en orden de ejecución:

1. `android/app/src/main/AndroidManifest.xml` — permisos y servicio.
2. `android/app/build.gradle` — dependencias Ktor, Kotlin.
3. `android/app/src/main/res/xml/network_security_config.xml` — HTTP local.
4. `android/app/src/main/java/com/nexuslink/MainActivity.kt` — entry point.
5. `android/app/src/main/java/com/nexuslink/MainApplication.kt` — registro módulos nativos.
6. `App.tsx` → `src/ui/Dashboard.tsx` — UI minimalista.
7. `src/domain/types.ts` — modelos TypeScript.
8. `src/services/serverStore.ts` — estado global (Zustand).
9. `package.json` — scripts y dependencias RN.
10. `android/app/src/main/java/com/nexuslink/NexusLinkModule.kt` — bridge.

---

## 7. Criterios de aceptación del MVP

- [ ] **UI:** Dashboard muestra estado servidor, IP local, botón iniciar/detener, modal configuración.
- [ ] **Selección de raíz:** Usuario elige carpeta via SAF; permisos persisten tras reinicio.
- [ ] **Servidor:** Ktor arranca/detiene desde UI; responde en red local en IP + puerto.
- [ ] **WebDAV métodos:** `OPTIONS`, `PROPFIND`, `GET` operativos y RFC 4918 compatibles.
- [ ] **SAF storage:** Listado de archivos raíz vía `PROPFIND`; lectura via `GET`.
- [ ] **Foreground Service:** Servicio visible, notificación persistente, servidor activo en background.
- [ ] **Validación clientes:** Windows Explorer, macOS Finder y Linux client pueden conectar.
- [ ] **Modo lectura/escritura:** `isReadOnly=true` bloquea `PUT`/`DELETE`, devuelve `403`.
- [ ] **Build:** `assembleDebug` reproducible, linters pasando.

---

## 8. Riesgos y mitigaciones

| Riesgo | Probabilidad | Impacto | Mitigación |
|--------|--------------|--------|-----------|
| Compatibilidad WebDAV Windows | Media | Alto | Spike Semana 1; prototipar `PROPFIND` XML temprano. |
| Permisos SAF caducados | Media | Medio | Verificar y restaurar en `onCreate`; UX clara si re-aceptación. |
| Foreground Service detenido por SO | Media | Alto | `foregroundServiceType` correcto, notificación visible, test en Android 11/12/13. |
| Concurrencia y memory leaks | Baja | Medio | Coroutines Kotlin, pruebas de carga, monitoring. |
| Cleartext bloqueado por políticas red | Baja | Medio | HTTPS roadmap Q3 2026; documentar limitación MVP. |
| Compatibilidad RN con módulo nativo | Media | Alto | Validar bridge con test simple Semana 2. |

---

## 9. Próximos pasos operativos

1. **Revisión y aprobación** de este documento oficial.
2. **Crear rama** `feature/week1/foundations` en repositorio.
3. **Iniciar Semana 1:**
   - Aplicar cambios estructurales a `AndroidManifest.xml` y `build.gradle`.
   - Confirmar decisiones (Ktor, bridge).
   - Crear documento `docs/BRIDGE.md` con contrato TS↔Kotlin.
4. **Spike WebDAV:** Prototipo rápido de `OPTIONS` y `PROPFIND` XML.
5. **Reviews regulares:** Revisión de entregables fin de cada semana; ajustar si es necesario.

---

## 10. Referencias y recursos

- **RFC 4918:** WebDAV Specification ([https://tools.ietf.org/html/rfc4918](https://tools.ietf.org/html/rfc4918))
- **Ktor Docs:** [https://ktor.io/](https://ktor.io/)
- **Android SAF:** [https://developer.android.com/guide/topics/providers/document-provider](https://developer.android.com/guide/topics/providers/document-provider)
- **Android Foreground Service:** [https://developer.android.com/guide/components/foreground-services](https://developer.android.com/guide/components/foreground-services)
- **React Native Bridge (Kotlin):** [https://reactnative.dev/docs/native-modules-android](https://reactnative.dev/docs/native-modules-android)
- **Documento Rector NexusLink:** `NexusLink_Documento_Rector_v1.md`

---

**Versión:** 1.0  
**Estado:** Oficial - Listo para implementación  
**Aprobado por:** Equipo de Desarrollo  
**Fecha:** 31 de mayo de 2026
