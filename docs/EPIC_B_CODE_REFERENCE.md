# Épica B — Código Generado (Referencia)

Este archivo contiene un resumen de todos los archivos clave creados/modificados en la Épica B.

---

## 1. `tailwind.config.js`

```javascript
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './App.tsx',
    './src/**/*.{js,jsx,ts,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#f9fafb',
          100: '#f3f4f6',
          900: '#1f2937',
        },
        accent: {
          blue: '#3b82f6',
          green: '#10b981',
          gray: '#6b7280',
        },
      },
      spacing: {
        safe: 'var(--safe-area-inset)',
      },
      animation: {
        pulse: 'pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      },
    },
  },
  plugins: [
    require('tailwindcss-animate'),
  ],
};
```

---

## 2. `babel.config.js` (actualizado)

```javascript
module.exports = {
  presets: ['module:@react-native/babel-preset'],
  plugins: [
    // NativeWind debe ser el primer plugin
    ['nativewind/babel'],
  ],
};
```

---

## 3. `src/services/serverStore.ts` (Zustand Store)

### Características principales:
- **Estado centralizado** con configuración y estado del servidor
- **Acciones asíncronas** que llaman al `nativeBridge`
- **Listeners automáticos** que sincronizan eventos nativos
- **Error handling** visual

### Tipos clave:
```typescript
export interface ExtendedServerState extends ServerState {
  isLoading: boolean;
  uiError: string | null;
  config: Partial<ServerConfig>;
  selectedFolderName: string | null;
}
```

### Acciones:
- `startServer(config)` — Inicia servidor
- `stopServer()` — Detiene servidor
- `getServerStatus()` — Sincroniza estado
- `pickRootDirectory()` — Abre picker SAF

---

## 4. `src/ui/Dashboard.tsx` (Componente Principal)

### Secciones:
1. **Encabezado** — Logo + título
2. **Instrucciones** — Card con pasos
3. **Estado** — Indicador pulsante + IP + conexiones
4. **Botones** — Seleccionar carpeta, iniciar/detener
5. **Footer** — Configuración

### Estilos Tailwind aplicados:
- Fondo oscuro: `bg-slate-900`
- Tarjetas: `bg-slate-800` con bordes sutiles
- Indicador pulsante: `animate-pulse`
- Botones: `rounded-2xl`, `shadow-xl`, `active:scale-95`
- Colores: Azul principal, verde para estado activo, grises para inactivo

### Funcionalidades:
- ✅ Reactividad Zustand
- ✅ Validación de carpeta raíz
- ✅ Loading states
- ✅ Error handling con Alerts
- ✅ SafeArea insets

---

## 5. `App.tsx` (Simplificado)

```typescript
import React from 'react';
import { StatusBar } from 'react-native';
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

## 6. `package.json` (Dependencias agregadas)

### Dependencies:
```json
"nativewind": "^4.0.0",
"zustand": "^4.5.0",
"tailwindcss-animate": "^1.0.7"
```

### DevDependencies:
```json
"tailwindcss": "^3.4.0",
"@babel/preset-typescript": "^7.25.2",
"@types/react-native": "^0.85.3"
```

---

## Flujo de datos

```
Dashboard.tsx (componente)
    ↓
useServerStore (Zustand)
    ↓
    ├─→ startServer() → nativeBridge.startServer()
    ├─→ stopServer() → nativeBridge.stopServer()
    ├─→ pickRootDirectory() → nativeBridge.pickRootDirectory()
    └─→ Event Listeners (sincronización automática)
        ├─→ onServerStateChanged
        ├─→ onConnectionCountUpdated
        └─→ onServerError
    ↓
nativeBridge (módulo nativo Kotlin)
    ↓
NexusLinkModule.kt (por implementar en Épica C)
```

---

## Próximas épicas

**Épica C — Native Bridge TS↔Kotlin:**
- Implementar módulo nativo Kotlin con promesas stub
- Exportar métodos a JavaScript
- Emitir eventos hacia JavaScript

**Épica D — Servidor WebDAV con Ktor:**
- Inicializar Ktor embebido
- Implementar métodos HTTP (OPTIONS, PROPFIND, GET, etc.)
- Respuestas RFC 4918 compatibles

---

## Comandos útiles

```bash
# Instalar dependencias
pnpm install

# Compilar TypeScript (sin emitir)
pnpm tsc --noEmit

# Ejecutar linter
pnpm lint

# Ejecutar en Android
pnpm android

# Iniciar servidor Metro
pnpm start
```

---

**Estado:** Épica B Completa ✅  
**Próximo:** Épica C (Semana 3)
