module.exports = function (api) {
  api.cache(true);
  return {
    presets: ['module:@react-native/babel-preset', 'nativewind/babel'],
    plugins: [
      // 👈 Este plugin SÍ transforma/elimina el import.meta de la compilación
      'babel-plugin-transform-import-meta' 
    ],
  };
};
