module.exports = {
  presets: ['module:@react-native/babel-preset'],
  plugins: [
    // NativeWind debe ser el primer plugin
    ['nativewind/babel'],
  ],
};
