/**
 * Domain Types - NexusLink
 * Contrato de comunicación entre RN (TypeScript) y Android (Kotlin)
 * Referencia: docs/BRIDGE.md
 */

/**
 * Configuración del servidor WebDAV
 * Se envía desde RN hacia el módulo nativo (Kotlin)
 */
export interface ServerConfig {
  /** Puerto TCP en el que escuchará el servidor */
  port: number;

  /** URI de la carpeta raíz seleccionada (e.g., content://com.android.externalstorage/documents/primary:Downloads) */
  rootDirectoryUri: string;

  /** Nombre legible de la carpeta raíz (e.g., "Descargas") */
  rootDirectoryName: string;

  /** Si true, bloquea operaciones de escritura (PUT, DELETE, MKCOL) */
  isReadOnly: boolean;

  /** Máximo número de conexiones simultáneas permitidas */
  maxConnections: number;
}

/**
 * Estado actual del servidor WebDAV
 * Se emite desde Kotlin hacia RN en tiempo real
 */
export interface ServerState {
  /** true si el servidor está activo y escuchando */
  isRunning: boolean;

  /** IP local en la que el servidor es accesible (e.g., "192.168.1.100"), null si no disponible */
  ipAddress: string | null;

  /** Número actual de clientes conectados */
  activeConnections: number;

  /** Mensaje de error si existe alguno, null si sin errores */
  errorMessage: string | null;
}

/**
 * Respuesta del picker de directorio SAF (Storage Access Framework)
 * Se devuelve tras seleccionar una carpeta desde el selector nativo
 */
export interface DirectoryPickerResult {
  /** URI de la carpeta seleccionada (persistible) */
  uri: string;

  /** Nombre amigable de la carpeta */
  name: string;
}

/**
 * Estructura de error emitida por eventos del servidor
 * Se envía hacia RN cuando ocurren errores críticos
 */
export interface ServerError {
  /** Código de error identificador (e.g., "SOCKET_ERROR", "SAF_PERMISSION_DENIED") */
  code: string;

  /** Mensaje descriptivo del error */
  message: string;
}
