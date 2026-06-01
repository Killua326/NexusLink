/**
 * NexusLink - Servidor WebDAV en Android
 * Aplicación principal
 */

import React from 'react';
import { StatusBar, useColorScheme } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import Dashboard from './src/ui/Dashboard';

function App() {
  const isDarkMode = useColorScheme() === 'dark';

  return (
    <SafeAreaProvider>
      <StatusBar
        barStyle="light-content"
        backgroundColor="#0f172a"
        translucent={false}
      />
      <Dashboard />
    </SafeAreaProvider>
  );
}

export default App;
