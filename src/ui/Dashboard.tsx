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
import nativeBridge from '../services/nativeBridge';

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
  const statusColor = isRunning ? 'bg-green-500' : 'bg-slate-400';
  const statusPulse = isRunning ? 'opacity-100 animate-pulse' : 'opacity-75';

  /**
   * Test de Humo para verificar la comunicación con el módulo nativo (Kotlin)
   */
  const runSmokeTest = async () => {
    try {
      Alert.alert("Test", "Probando comunicación con Kotlin...");
      // Invocación de prueba
      await nativeBridge.startServer({ 
        port: 9999, 
        rootDirectoryUri: "test", 
        rootDirectoryName: "test", 
        isReadOnly: true, 
        maxConnections: 1 
      });
      Alert.alert("Éxito ✅", "El Puente (Bridge) funciona correctamente.");
    } catch (e) {
      console.error(e);
      Alert.alert("Error ❌", "El puente falló. Revisa Logcat en Android Studio.");
    }
  };

  const handleStartServer = async () => {
    if (!config.rootDirectoryUri) {
      Alert.alert('Carpeta no seleccionada', 'Por favor, selecciona un directorio raíz.');
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

  return (
    <ScrollView 
      className="flex-1 bg-gray-950" 
      contentContainerStyle={{ paddingTop: insets.top, paddingBottom: insets.bottom }}
    >
      {/* Encabezado */}
      <View className="items-center py-6">
        <View className="w-20 h-20 rounded-full bg-blue-600 items-center justify-center mb-4 shadow-lg">
          <Text className="text-2xl">📱</Text>
        </View>
        <Text className="text-3xl font-bold text-white">NexusLink</Text>
        <Text className="text-xs text-slate-400 mt-2">Servidor WebDAV local</Text>
      </View>

      {/* Tarjeta de Instrucciones */}
      <View className="mx-4 mb-6 p-4 rounded-2xl bg-gray-900 border border-gray-800">
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
              <Text className="font-bold">URL:</Text> http://{ipAddress}:{config.port ?? 8080}
            </Text>
            <Text className="text-sm text-green-400 font-bold">
              {activeConnections} conexión{activeConnections !== 1 ? 'es' : ''} activa{activeConnections !== 1 ? 's' : ''}
            </Text>
          </View>
        ) : (
          <Text className="text-sm text-slate-500 mb-4">Esperando configuración...</Text>
        )}

        {selectedFolderName && (
          <View className="mb-4 p-3 bg-gray-900 rounded-xl w-full border border-gray-800">
            <Text className="text-xs text-slate-400 mb-1">Carpeta raíz:</Text>
            <Text className="text-sm text-white font-medium" numberOfLines={1}>{selectedFolderName}</Text>
          </View>
        )}
      </View>

      {/* Botón Seleccionar Carpeta */}
      {!isRunning && (
        <TouchableOpacity
          disabled={isLoading}
          onPress={pickRootDirectory}
          className="mx-4 mb-4 py-3 px-4 rounded-xl bg-gray-900 border border-gray-800"
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
            ${isRunning ? 'bg-gray-800 border-gray-700' : 'bg-blue-600 border-blue-700'}`}
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

      {/* Botón de Test de Humo (DEBUG) */}
      <TouchableOpacity
        onPress={runSmokeTest}
        className="mx-4 mb-8 py-2 rounded-lg bg-orange-900/20 border border-orange-800"
      >
        <Text className="text-center text-orange-400 text-xs font-bold">
          🚀 TEST DE HUMO: Verificar Puente Nativo
        </Text>
      </TouchableOpacity>
    </ScrollView>
  );
};

export default Dashboard;