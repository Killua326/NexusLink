/**
 * Dashboard - NexusLink
 * Pantalla principal minimalista y oscura
 * Muestra estado del servidor, IP, conexiones y controles de inicio/parada
 */

import React, { useEffect } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useServerStore } from '../services/serverStore';
import { ServerConfig } from '../domain/types';

const Dashboard: React.FC = () => {
  const insets = useSafeAreaInsets();

  // Estado del store
  const {
    isRunning,
    ipAddress,
    activeConnections,
    isLoading,
    uiError,
    config,
    selectedFolderName,
    startServer,
    stopServer,
    pickRootDirectory,
    getServerStatus,
  } = useServerStore();

  // Al montar el componente, sincronizar estado con servidor
  useEffect(() => {
    getServerStatus();
  }, [getServerStatus]);

  // Mostrar errores visualmente
  useEffect(() => {
    if (uiError) {
      Alert.alert('Error', uiError);
    }
  }, [uiError]);

  /**
   * Manejador del botón iniciar servidor
   */
  const handleStartServer = async () => {
    // Validar que se haya seleccionado una carpeta
    if (!config.rootDirectoryUri) {
      Alert.alert(
        'Carpeta no seleccionada',
        'Por favor, selecciona un directorio raíz antes de iniciar el servidor.'
      );
      return;
    }

    const serverConfig: ServerConfig = {
      port: config.port ?? 8080,
      rootDirectoryUri: config.rootDirectoryUri,
      rootDirectoryName: config.rootDirectoryName ?? 'Raíz',
      isReadOnly: config.isReadOnly ?? false,
      maxConnections: config.maxConnections ?? 10,
    };

    await startServer(serverConfig);
  };

  /**
   * Manejador del botón detener servidor
   */
  const handleStopServer = async () => {
    await stopServer();
  };

  /**
   * Manejador para seleccionar carpeta raíz
   */
  const handlePickFolder = async () => {
    await pickRootDirectory();
  };

  /**
   * Color del indicador de estado basado en isRunning
   */
  const statusColor = isRunning ? 'bg-emerald-500' : 'bg-slate-400';
  const statusPulse = isRunning ? 'opacity-100 animate-pulse' : 'opacity-75';

  return (
    <ScrollView
      style={{ flex: 1, backgroundColor: '#0f172a' }}
      contentContainerStyle={{ paddingTop: insets.top, paddingBottom: insets.bottom }}
    >
      {/* Encabezado con Logo y Título */}
      <View style={{ alignItems: 'center', paddingTop: 24, paddingBottom: 24 }}>
        <View style={{ width: 80, height: 80, borderRadius: 9999, backgroundColor: '#3b82f6', alignItems: 'center', justifyContent: 'center', marginBottom: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 4.65, elevation: 8 }}>
          <Text style={{ fontSize: 24 }}>📱</Text>
        </View>
        <Text style={{ fontSize: 36, fontWeight: 'bold', color: '#fff' }}>NexusLink</Text>
        <Text style={{ fontSize: 12, color: '#94a3b8', marginTop: 8 }}>Servidor WebDAV local</Text>
      </View>

      {/* Tarjeta de Instrucciones */}
      <View style={{ marginHorizontal: 16, marginBottom: 24, padding: 16, borderRadius: 16, backgroundColor: '#1e293b', borderColor: '#334155', borderWidth: 1 }}>
        <Text style={{ fontSize: 14, color: '#e2e8f0', lineHeight: 20 }}>
          <Text style={{ fontWeight: 'bold', color: '#60a5fa' }}>1.</Text> Conecta este teléfono y tu PC a la misma red Wi-Fi.{'\n'}
          <Text style={{ fontWeight: 'bold', color: '#60a5fa' }}>2.</Text> Selecciona una carpeta raíz e inicia el servidor.{'\n'}
          <Text style={{ fontWeight: 'bold', color: '#60a5fa' }}>3.</Text> En tu PC, agrega una ubicación de red usando la dirección URL mostrada.
        </Text>
      </View>

      {/* Sección de Estado - Centro de Pantalla */}
      <View style={{ marginHorizontal: 16, marginBottom: 32, alignItems: 'center' }}>
        {/* Indicador visual de estado (círculo pulsante) */}
        <View style={{ width: 64, height: 64, borderRadius: 9999, backgroundColor: isRunning ? '#10b981' : '#64748b', opacity: isRunning ? 1 : 0.75, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 4.65, elevation: 8, marginBottom: 16 }} />

        {/* Texto de estado dinámico */}
        <Text style={{ fontSize: 20, fontWeight: '600', color: '#fff', marginBottom: 8 }}>
          {isRunning ? 'Servidor Activo' : 'Servidor Inactivo'}
        </Text>

        {/* IP y conexiones */}
        {isRunning && ipAddress ? (
          <View style={{ alignItems: 'center', marginBottom: 16 }}>
            <Text style={{ fontSize: 14, color: '#cbd5e1', marginBottom: 8 }}>
              <Text style={{ fontWeight: 'bold' }}>IP:</Text> {ipAddress}:
              {config.port ?? 8080}
            </Text>
            <Text style={{ fontSize: 14, color: '#34d399' }}>
              <Text style={{ fontWeight: 'bold' }}>{activeConnections}</Text> conexión
              {activeConnections !== 1 ? 'es' : ''} activa{activeConnections !== 1 ? 's' : ''}
            </Text>
          </View>
        ) : (
          <Text style={{ fontSize: 14, color: '#94a3b8', marginBottom: 16 }}>Esperando configuración...</Text>
        )}

        {/* Carpeta seleccionada */}
        {selectedFolderName && (
          <View style={{ marginBottom: 16, padding: 12, backgroundColor: '#334155', borderRadius: 12, width: '100%' }}>
            <Text style={{ fontSize: 12, color: '#94a3b8', marginBottom: 8 }}>Carpeta raíz:</Text>
            <Text style={{ fontSize: 14, color: '#fff', fontWeight: '500' }} numberOfLines={1}>{selectedFolderName}</Text>
          </View>
        )}
      </View>

      {/* Botón seleccionar carpeta (si no está corriendo) */}
      {!isRunning && (
        <TouchableOpacity
          disabled={isLoading}
          onPress={handlePickFolder}
          style={{ marginHorizontal: 16, marginBottom: 16, paddingVertical: 12, paddingHorizontal: 16, borderRadius: 12, backgroundColor: '#334155', borderColor: '#475569', borderWidth: 1 }}
        >
          <Text style={{ textAlign: 'center', color: '#e2e8f0', fontWeight: '600', fontSize: 16 }}>
            {isLoading ? '⏳ Cargando...' : '📁 Seleccionar Carpeta Raíz'}
          </Text>
        </TouchableOpacity>
      )}

      {/* Botón principal: Iniciar/Detener Servidor */}
      <View style={{ marginHorizontal: 16, marginBottom: 24 }}>
        <TouchableOpacity
          disabled={isLoading}
          onPress={isRunning ? handleStopServer : handleStartServer}
          style={{ paddingVertical: 16, paddingHorizontal: 24, borderRadius: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.3, shadowRadius: 4.65, elevation: 8, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', backgroundColor: isRunning ? '#334155' : '#3b82f6', borderColor: isRunning ? '#475569' : '#1d4ed8', borderWidth: 2 }}
        >
          {isLoading ? (
            <ActivityIndicator color={isRunning ? '#cbd5e1' : '#ffffff'} size="small" />
          ) : (
            <Text style={{ fontSize: 18, fontWeight: 'bold', marginLeft: 8, color: isRunning ? '#e2e8f0' : '#fff' }}>
              {isRunning ? '⏹️ DETENER SERVIDOR' : '▶️ INICIAR SERVIDOR'}
            </Text>
          )}
        </TouchableOpacity>
      </View>

      {/* Botón de configuración (footer) */}
      <View style={{ marginHorizontal: 16, marginBottom: 32 }}>
        <TouchableOpacity style={{ paddingVertical: 12, paddingHorizontal: 16, borderRadius: 12, backgroundColor: '#1e293b', borderColor: '#334155', borderWidth: 1 }}>
          <Text style={{ textAlign: 'center', color: '#d1d5db', fontWeight: '500', fontSize: 14 }}>
            ⚙️ Configuración del Servidor
          </Text>
        </TouchableOpacity>
      </View>

      {/* Debug: Mostrar configuración actual (solo en desarrollo) */}
      {__DEV__ && (
        <View style={{ marginHorizontal: 16, marginBottom: 32, padding: 12, backgroundColor: '#1e293b', borderRadius: 12, borderColor: '#334155', borderWidth: 1 }}>
          <Text style={{ fontSize: 12, color: '#94a3b8', marginBottom: 8 }}>
            <Text style={{ fontWeight: 'bold' }}>Debug - Config:</Text>
          </Text>
          <Text style={{ fontSize: 12, color: '#cbd5e1' }}>
            Puerto: {config.port ?? 8080}
          </Text>
          <Text style={{ fontSize: 12, color: '#cbd5e1' }}>
            Solo lectura: {config.isReadOnly ? 'Sí' : 'No'}
          </Text>
          <Text style={{ fontSize: 12, color: '#cbd5e1' }}>
            Max conexiones: {config.maxConnections ?? 10}
          </Text>
        </View>
      )}
    </ScrollView>
  );
};

export default Dashboard;