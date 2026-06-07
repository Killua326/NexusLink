const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
const { withNativeWind } = require('nativewind/metro');

const config = {
  resolver: {
    // 1. Esto fuerza a Metro a leer el archivo libre de import.meta
    alias: {
      'zustand': require.resolve('zustand'),
    },
    // 2. Mantenemos esto por compatibilidad con otras dependencias
    unstable_conditionNames: ['browser', 'require', 'react-native'],
  },
};

module.exports = withNativeWind(mergeConfig(getDefaultConfig(__dirname), config), { input: './global.css' });
