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

  useEffect(() => {
    getServerStatus();
  }, [getServerStatus]);

  useEffect(() => {
    if (uiError) Alert.alert('Error', uiError);
  }, [uiError]);

  // Variables de estado dinámico
  const statusColor = isRunning ? 'bg-success' : 'bg-slate-400';
  const statusPulse = isRunning ? 'opacity-100 animate-pulse' : 'opacity-75';

  const handleStartServer = async () => {
    if (!config.rootDirectoryUri) {
      Alert.alert('Carpeta no seleccionada', 'Por favor, selecciona un directorio raíz.');
      return;
    }

    // Dando uso a la interfaz ServerConfig aquí:
    const serverConfig: ServerConfig = {
      port: config.port ?? 8080,
      rootDirectoryUri: config.rootDirectoryUri,
      rootDirectoryName: config.rootDirectoryName ?? 'Raíz',
      isReadOnly: config.isReadOnly ?? false,
      maxConnections: config.maxConnections ?? 10,
    };

    await startServer(serverConfig);
  };

  return (
    <ScrollView 
      className="flex-1 bg-background" 
      contentContainerStyle={{ paddingTop: insets.top, paddingBottom: insets.bottom }}
    >
      {/* Encabezado */}
      <View className="items-center py-6">
        <View className="w-20 h-20 rounded-full bg-primary items-center justify-center mb-4 shadow-lg">
          <Text className="text-2xl">📱</Text>
        </View>
        <Text className="text-3xl font-bold text-white">NexusLink</Text>
        <Text className="text-xs text-muted mt-2">Servidor WebDAV local</Text>
      </View>

      {/* Tarjeta de Instrucciones */}
      <View className="mx-4 mb-6 p-4 rounded-2xl bg-card border border-border">
        <Text className="text-sm text-slate-200 leading-5">
          <Text className="font-bold text-blue-400">1.</Text> Conecta este teléfono y tu PC a la misma red Wi-Fi.{'\n'}
          <Text className="font-bold text-blue-400">2.</Text> Selecciona una carpeta raíz e inicia el servidor.{'\n'}
          <Text className="font-bold text-blue-400">3.</Text> En tu PC, agrega una ubicación de red usando la URL mostrada.
        </Text>
      </View>

      {/* Sección de Estado */}
      <View className="mx-4 mb-8 items-center">
        <View className={`w-16 h-16 rounded-full ${statusColor} ${statusPulse} shadow-lg mb-4`} />

        <Text className="text-xl font-semibold text-white mb-2">
          {isRunning ? 'Servidor Activo' : 'Servidor Inactivo'}
        </Text>

        {isRunning && ipAddress ? (
          <View className="items-center mb-4">
            <Text className="text-sm text-slate-200 mb-2">
              <Text className="font-bold">IP:</Text> {ipAddress}:{config.port ?? 8080}
            </Text>
            <Text className="text-sm text-success font-bold">
              {activeConnections} conexión{activeConnections !== 1 ? 'es' : ''} activa{activeConnections !== 1 ? 's' : ''}
            </Text>
          </View>
        ) : (
          <Text className="text-sm text-muted mb-4">Esperando configuración...</Text>
        )}

        {selectedFolderName && (
          <View className="mb-4 p-3 bg-border rounded-xl w-full">
            <Text className="text-xs text-muted mb-1">Carpeta raíz:</Text>
            <Text className="text-sm text-white font-medium" numberOfLines={1}>{selectedFolderName}</Text>
          </View>
        )}
      </View>

      {/* Botón Seleccionar */}
      {!isRunning && (
        <TouchableOpacity
          disabled={isLoading}
          onPress={pickRootDirectory}
          className="mx-4 mb-4 py-3 px-4 rounded-xl bg-card border border-border"
        >
          <Text className="text-center text-slate-200 font-semibold text-base">
            {isLoading ? '⏳ Cargando...' : '📁 Seleccionar Carpeta Raíz'}
          </Text>
        </TouchableOpacity>
      )}

      {/* Botón Principal */}
      <View className="mx-4 mb-6">
        <TouchableOpacity
          disabled={isLoading}
          onPress={isRunning ? stopServer : handleStartServer}
          className={`py-4 px-6 rounded-2xl flex-row items-center justify-center shadow-lg border-2 
            ${isRunning ? 'bg-card border-border' : 'bg-primary border-blue-700'}`}
        >
          {isLoading ? (
            <ActivityIndicator color="#ffffff" size="small" />
          ) : (
            <Text className={`text-lg font-bold ${isRunning ? 'text-slate-200' : 'text-white'}`}>
              {isRunning ? '⏹️ DETENER SERVIDOR' : '▶️ INICIAR SERVIDOR'}
            </Text>
          )}
        </TouchableOpacity>
      </View>

      {/* Footer */}
      <View className="mx-4 mb-8">
        <TouchableOpacity className="py-3 px-4 rounded-xl bg-card border border-border">
          <Text className="text-center text-slate-400 font-medium text-sm">
            ⚙️ Configuración del Servidor
          </Text>
        </TouchableOpacity>
      </View>
    </ScrollView>
  );
};

export default Dashboard;