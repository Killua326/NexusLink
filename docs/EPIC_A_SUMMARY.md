# Épica A — Fundación del producto Android

**Estado:** ✅ Completa (Semana 1)  
**Rama:** `feature/week1-foundations`

## Entregables Realizados

### 1. Estructura `src/` creada
```
src/
├── domain/           # Tipos e interfaces compartidas
│   ├── types.ts     # ServerConfig, ServerState, etc.
│   └── index.ts     # Exportaciones públicas
├── services/         # Lógica de negocio y estado
│   └── nativeBridge.ts  # Interfaz con módulo nativo
└── ui/              # Componentes React Native (próximos)
```

### 2. Tipos TypeScript definidos (`src/domain/types.ts`)
- `ServerConfig`: Configuración del servidor (puerto, raíz, permisos, conexiones)
- `ServerState`: Estado actual del servidor (running, IP, conexiones, errores)
- `DirectoryPickerResult`: Respuesta del picker SAF
- `ServerError`: Estructura de errores del servidor

Todos los tipos son **JSON serializable** y alineados con el contrato del bridge.

### 3. Native Bridge Service (`src/services/nativeBridge.ts`)
Interfaz singleton que expone:
- **Promesas (TS→Kotlin):**
  - `startServer(config)`: Inicia servidor
  - `stopServer()`: Detiene servidor
  - `getServerStatus()`: Obtiene estado actual
  - `pickRootDirectory()`: Abre picker SAF

- **Eventos (Kotlin→TS):**
  - `onServerStateChanged`: Cambios de estado
  - `onConnectionCountUpdated`: Actualizaciones de conexiones
  - `onServerError`: Errores críticos

### 4. Configuración Android actualizada

#### `AndroidManifest.xml`
- ✅ Permisos agregados: `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`
- ✅ Referencia a `network_security_config.xml`
- ✅ Declaración del `ForegroundService` (`.services.WebDAVService`)

#### `network_security_config.xml`
- ✅ Permite HTTP en red local (127.0.0.1, 192.168.x.x, etc.)
- ✅ Bloquea HTTP en dominios públicos
- ✅ Compatible con Android 9+

## Validación

```bash
# Verificar compilación TS
pnpm tsc --noEmit

# Verificar estructura del proyecto
find src -type f -name "*.ts" | head -20
```

## Próximos pasos (Épica B - Semana 2)
- Crear Dashboard UI en `src/ui/Dashboard.tsx`
- Implementar store global con Zustand
- Conectar UI con `nativeBridge` (eventos simulados)
- Crear modal de configuración

## Referencias
- `docs/plan.md`: Roadmap oficial
- `docs/BRIDGE.md`: Contrato TS↔Kotlin
- `NexusLink_Documento_Rector_v1.md`: Especificación general
