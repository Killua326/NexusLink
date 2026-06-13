/**
 * Server Store - NexusLink
 * Estado global de la aplicación usando Zustand
 * Maneja el ciclo de vida del servidor WebDAV, configuración y sincronización de eventos.
 */

import { create } from 'zustand';
import { ServerConfig, ServerState, ServerError } from '../domain/types';
import nativeBridge from './nativeBridge';

/**
 * Estado extendido del servidor (incluye loading y error handling)
 */
export interface ExtendedServerState extends ServerState {
  /** true mientras se procesa una acción asíncrona */
  isLoading: boolean;

  /** Mensaje de error visual para mostrar en UI */
  uiError: string | null;

  /** Configuración actual (puede variar del estado del servidor) */
  config: Partial<ServerConfig>;

  /** Nombre de la carpeta raíz seleccionada (legible) */
  selectedFolderName: string | null;
}

/**
 * Acciones y métodos del store
 */
interface ServerStoreActions {
  // Acciones asíncronas
  startServer: (config: ServerConfig) => Promise<void>;
  stopServer: () => Promise<void>;
  getServerStatus: () => Promise<void>;
  pickRootDirectory: () => Promise<void>;

  // Acciones síncronas (sincronización con eventos nativos)
  setServerState: (state: ServerState) => void;
  setConnectionCount: (count: number) => void;
  setError: (error: ServerError | null) => void;
  setUIError: (message: string | null) => void;
  setConfig: (config: Partial<ServerConfig>) => void;
  setSelectedFolderName: (name: string | null) => void;
  setIsLoading: (loading: boolean) => void;

  // Limpieza
  resetStore: () => void;
  setupEventListeners: () => void;
}

/**
 * Estado inicial
 */
const initialState: ExtendedServerState = {
  isRunning: false,
  ipAddress: null,
  activeConnections: 0,
  errorMessage: null,
  isLoading: false,
  uiError: null,
  config: {
    port: 8080,
    maxConnections: 10,
    isReadOnly: false,
  },
  selectedFolderName: null,
};

/**
 * Store global del servidor
 */
export const useServerStore = create<ExtendedServerState & ServerStoreActions>((set, get) => {
  // Configurar listeners de eventos nativos (se ejecuta una sola vez)
  const setupListeners = () => {
    // Listener: cambio de estado del servidor
    nativeBridge.onServerStateChanged((state: ServerState) => {
      get().setServerState(state);
    });

    // Listener: actualización de conexiones activas
    nativeBridge.onConnectionCountUpdated((count: number) => {
      get().setConnectionCount(count);
    });

    // Listener: error del servidor
    nativeBridge.onServerError((error: ServerError) => {
      get().setError(error);
      get().setUIError(error.message);
    });
  };

  return {
    // Estado inicial
    ...initialState,

    // Acciones síncronas
    setServerState: (state: ServerState) => {
      set({ ...state, errorMessage: state.errorMessage });
    },

    setConnectionCount: (count: number) => {
      set({ activeConnections: count });
    },

    setError: (error: ServerError | null) => {
      if (error) {
        set({ errorMessage: error.message });
      } else {
        set({ errorMessage: null });
      }
    },

    setUIError: (message: string | null) => {
      set({ uiError: message });
    },

    setConfig: (config: Partial<ServerConfig>) => {
      set(state => ({
        config: { ...state.config, ...config },
      }));
    },

    setSelectedFolderName: (name: string | null) => {
      set({ selectedFolderName: name });
    },

    setIsLoading: (loading: boolean) => {
      set({ isLoading: loading });
    },

    // Acciones asíncronas
    startServer: async (config: ServerConfig) => {
      try {
        set({ isLoading: true, uiError: null });
        const result = await nativeBridge.startServer(config);
        if (!result) {
          set({ uiError: 'Error al iniciar el servidor' });
        } else {
          set({ 
            isRunning: true, 
            config: config, 
            isLoading: false 
          });
        }
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Error desconocido';
        set({ uiError: message });
        console.error('[ServerStore] Error starting server:', error);
      } finally {
        set({ isLoading: false });
      }
    },

    stopServer: async () => {
      try {
        set({ isLoading: true, uiError: null, isRunning: false  });
        const result = await nativeBridge.stopServer();
        if (!result) {
          set({ uiError: 'Error al detener el servidor' });
        }
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Error desconocido';
        set({ uiError: message });
        console.error('[ServerStore] Error stopping server:', error);
      } finally {
        set({ isLoading: false });
      }
    },

    getServerStatus: async () => {
      try {
        set({ isLoading: true });
        const state = await nativeBridge.getServerStatus();
        set(state);
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Error al obtener estado';
        set({ uiError: message });
        console.error('[ServerStore] Error getting server status:', error);
      } finally {
        set({ isLoading: false });
      }
    },

    pickRootDirectory: async () => {
      try {
        set({ isLoading: true, uiError: null });
        const result = await nativeBridge.pickRootDirectory();
        set({
          config: {
            ...get().config,
            rootDirectoryUri: result.uri,
            rootDirectoryName: result.name,
          },
          selectedFolderName: result.name,
        });
      } catch (error) {
        const message = error instanceof Error ? error.message : 'Cancellado';
        // No mostrar error si es cancelación natural
        if (!message.toLowerCase().includes('cancel')) {
          set({ uiError: message });
        }
        console.error('[ServerStore] Error picking directory:', error);
      } finally {
        set({ isLoading: false });
      }
    },

    resetStore: () => {
      set(initialState);
    },

    setupEventListeners: () => {
      setupListeners();
    },
  };
});

// Inicializar listeners al cargar el store
useServerStore.getState().setupEventListeners();
