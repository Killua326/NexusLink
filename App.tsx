// src/App.tsx
import React from 'react';
import { StatusBar, useColorScheme } from 'react-native'; // Usado
import { SafeAreaProvider } from 'react-native-safe-area-context';
import Dashboard from './src/ui/Dashboard';
import "./global.css";

function App() {
  const colorScheme = useColorScheme(); // Ahora sí está en uso
  const isDarkMode = colorScheme === 'dark';

  return (
    <SafeAreaProvider>
      <StatusBar
        barStyle={isDarkMode ? "light-content" : "dark-content"}
        backgroundColor="#0f172a"
        translucent={false}
      />
      <Dashboard />
    </SafeAreaProvider>
  );
}

export default App;