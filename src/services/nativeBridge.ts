/**
 * Native Bridge Service - NexusLink
 * Interfaz que expone los métodos disponibles del módulo nativo (Kotlin)
 * y permite suscribirse a eventos del servidor.
 *
 * Referencia: docs/BRIDGE.md
 */

import { NativeModules, NativeEventEmitter, EventSubscription } from 'react-native';
import {
  ServerConfig,
  ServerState,
  DirectoryPickerResult,
  ServerError,
} from '../domain/types';

const { NexusLinkModule } = NativeModules;

/**
 * Servicio que actúa como puente entre RN/TS y el módulo nativo Android
 */
class NativeBridgeService {
  private eventEmitter: NativeEventEmitter;

  constructor() {
    this.eventEmitter = new NativeEventEmitter(NexusLinkModule);
  }

  /**
   * Inicia el servidor WebDAV con la configuración especificada
   * @param config Configuración del servidor (puerto, raíz, permisos, etc.)
   * @returns Promise<boolean> true si fue exitoso, false en caso contrario
   */
  async startServer(config: ServerConfig): Promise<boolean> {
    try {
      // ✅ CORRECCIÓN 1: Pasamos específicamente el 'port' (número), que es lo que pide Kotlin
      const result = await NexusLinkModule.startServer(config.port);
      console.log("Resultado del Bridge:", result);
      // ✅ CORRECCIÓN 2: Verificamos el string que Kotlin realmente devuelve
      return result === "Server started";
    } catch (error) {
      console.error('[NativeBridge] Error starting server:', error);
      throw error;
    }
  }

  /**
   * Detiene el servidor WebDAV y libera recursos
   */
  async stopServer(): Promise<boolean> {
    try {
      const result = await NexusLinkModule.stopServer();
      
      // ✅ CORRECCIÓN: Verificamos el string que Kotlin realmente devuelve
      return result === "Server stopped";
    } catch (error) {
      console.error('[NativeBridge] Error stopping server:', error);
      throw error;
    }
  }

  /**
   * Obtiene el estado actual del servidor para sincronización
   * @returns Promise<ServerState> Estado actual del servidor
   */
  async getServerStatus(): Promise<ServerState> {
    try {
      const state = await NexusLinkModule.getServerStatus();
      return state as ServerState;
    } catch (error) {
      console.error('[NativeBridge] Error getting server status:', error);
      throw error;
    }
  }

  /**
   * Abre el selector SAF para que el usuario seleccione la carpeta raíz
   * @returns Promise<DirectoryPickerResult> URI y nombre de la carpeta seleccionada
   */
  async pickRootDirectory(): Promise<DirectoryPickerResult> {
    try {
      const result = await NexusLinkModule.pickRootDirectory();
      return result as DirectoryPickerResult;
    } catch (error) {
      console.error('[NativeBridge] Error picking directory:', error);
      throw error;
    }
  }

  /**
   * Se suscribe a cambios de estado del servidor
   * @param listener Callback que recibe el nuevo estado
   * @returns Subscription para poder desuscribirse
   */
  onServerStateChanged(listener: (state: ServerState) => void): EventSubscription {
    return this.eventEmitter.addListener('onServerStateChanged', listener);
  }

  /**
   * Se suscribe a actualizaciones del conteo de conexiones activas
   * @param listener Callback que recibe el número de conexiones
   * @returns Subscription para poder desuscribirse
   */
  onConnectionCountUpdated(listener: (count: number) => void): EventSubscription {
    return this.eventEmitter.addListener('onConnectionCountUpdated', listener);
  }

  /**
   * Se suscribe a eventos de error del servidor
   * @param listener Callback que recibe el error
   * @returns Subscription para poder desuscribirse
   */
  onServerError(listener: (error: ServerError) => void): EventSubscription {
    return this.eventEmitter.addListener('onServerError', listener);
  }

  /**
   * Limpia todas las suscripciones de eventos
   */
  destroy(): void {
      this.eventEmitter.removeAllListeners('onServerStateChanged');
  this.eventEmitter.removeAllListeners('onConnectionCountUpdated');
  this.eventEmitter.removeAllListeners('onServerError');
  }
}

export default new NativeBridgeService();
