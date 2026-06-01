# Épica B — UI React Native y Estado Global

**Estado:** ✅ Completa (Semana 2)  
**Rama:** `feature/week1-foundations`

## Entregables Realizados

### 1. Configuración de Estilos: NativeWind + Tailwind

✅ **Dependencias instaladas:**
- `nativewind@^4.0.0` — Framework de estilos para RN
- `tailwindcss@^3.4.0` — Motor de estilos
- `tailwindcss-animate@^1.0.7` — Animaciones

✅ **Archivos creados/actualizados:**
- `tailwind.config.js` — Configuración con paleta personalizada (colores azul/verde/gris del documento rector)
- `babel.config.js` — Plugin NativeWind configurado **primero** en la lista de plugins
- `package.json` — Dependencias actualizadas

✅ **Paleta de colores personalizada:**
```javascript
colors: {
  primary: { 50: '#f9fafb', 900: '#1f2937' },
  accent: { blue: '#3b82f6', green: '#10b981', gray: '#6b7280' }
}
```

---

### 2. Estado Global: Zustand Store

✅ **Archivo:** `src/services/serverStore.ts`

**Características:**
- Store centralizado con Zustand
- Estado extiende `ServerState` con campos adicionales:
  - `isLoading` — flag para operaciones asíncronas
  - `uiError` — mensajes de error para UI
  - `config` — configuración actual del servidor
  - `selectedFolderName` — nombre legible de la carpeta raíz

**Acciones asíncronas:**
- `startServer(config)` — Inicia servidor vía `nativeBridge`
- `stopServer()` — Detiene servidor
- `getServerStatus()` — Sincroniza estado actual
- `pickRootDirectory()` — Abre picker SAF nativo

**Acciones síncronas:**
- `setServerState()`, `setConnectionCount()`, `setError()`, `setConfig()`, etc.
- `setupEventListeners()` — Configura listeners nativos automáticamente

**Sincronización con eventos nativos:**
```typescript
nativeBridge.onServerStateChanged(state => get().setServerState(state))
nativeBridge.onConnectionCountUpdated(count => get().setConnectionCount(count))
nativeBridge.onServerError(error => get().setError(error))
```

---

### 3. Componente Dashboard

✅ **Archivo:** `src/ui/Dashboard.tsx`

**Estructura visual (minimalista, oscuro):**

1. **Encabezado** (Logo + Título)
   - Círculo azul con emoji 📱
   - Título "NexusLink"
   - Subtítulo "Servidor WebDAV local"

2. **Tarjeta de Instrucciones**
   - 3 pasos claros con numeración
   - Fondo gris oscuro, borde sutil

3. **Sección de Estado** (Centro)
   - Indicador pulsante (gris si inactivo, verde esmeralda si activo)
   - Texto de estado dinámico
   - Muestra IP:puerto cuando está activo
   - Contador de conexiones activas
   - Nombre de carpeta raíz seleccionada

4. **Botones de Acción**
   - Botón "📁 Seleccionar Carpeta Raíz" (gris, si servidor inactivo)
   - Botón principal "▶️ INICIAR SERVIDOR" (azul) / "⏹️ DETENER SERVIDOR" (gris)
   - Botón de configuración en footer "⚙️ Configuración"

5. **Debug Info** (solo en `__DEV__`)
   - Muestra puerto, modo solo lectura, max conexiones

**Funcionalidades:**
- ✅ Reactividad total al store de Zustand
- ✅ Manejo de errores con `Alert`
- ✅ Validación: requiere carpeta raíz antes de iniciar
- ✅ Loading states durante operaciones async
- ✅ SafeArea insets incluidos
- ✅ Diseño responsivo con Tailwind

---

### 4. App Principal

✅ **Archivo:** `App.tsx` (simplificado y limpio)

```typescript
import React from 'react';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import Dashboard from './src/ui/Dashboard';

function App() {
  return (
    <SafeAreaProvider>
      <StatusBar barStyle="light-content" backgroundColor="#0f172a" />
      <Dashboard />
    </SafeAreaProvider>
  );
}

export default App;
```

---

### 5. Exportaciones limpias

✅ **`src/ui/index.ts`** — Exporta `Dashboard`
✅ **`src/services/index.ts`** — Exporta `nativeBridge` y `useServerStore`
✅ **`src/domain/index.ts`** — Exporta tipos principales

---

## Estructura Final

```
src/
├── domain/
│   ├── types.ts          # ServerConfig, ServerState, etc.
│   └── index.ts          # Exportaciones
├── services/
│   ├── nativeBridge.ts   # Interfaz con módulo nativo
│   ├── serverStore.ts    # Store global Zustand
│   └── index.ts          # Exportaciones
└── ui/
    ├── Dashboard.tsx     # Componente principal
    └── index.ts          # Exportaciones
```

---

## Validación

```bash
# Compilar TypeScript
pnpm tsc --noEmit

# Ejecutar linter (si está configurado)
pnpm lint

# Instalar dependencias
pnpm install

# Build Android (prueba visual)
pnpm android
```

---

## Características técnicas destacadas

✅ **Clean Architecture**: Separación clara entre dominio, servicios e UI  
✅ **Type Safety**: Totalmente tipado en TypeScript  
✅ **Reactive**: Store Zustand con listeners nativos  
✅ **Error Handling**: Manejo visual de errores  
✅ **Loading States**: Feedback visual en operaciones async  
✅ **Tailwind + NativeWind**: Estilos modernos, animaciones  
✅ **Modular**: Componentes exportables y reutilizables  

---

## Próximos pasos (Épica C - Semana 3+)

- Implementar módulo nativo Kotlin con promesas
- Levantar servidor Ktor embebido
- Implementar métodos WebDAV (OPTIONS, PROPFIND, GET)
- Conectar eventos reales desde Kotlin hacia TS

---

## Referencias

- `docs/plan.md` — Roadmap oficial
- `docs/EPIC_A_SUMMARY.md` — Épica A completada
- Documento Rector — Especificación general
