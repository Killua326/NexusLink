module.exports = {
  root: true,
  extends: '@react-native',
  overrides: [
    {
      // Aplica estas reglas SOLO a archivos de prueba
      files: ['**/*.test.[jt]s', '**/*.spec.[jt]s', '**/__tests__/**/*'],
      plugins: ['jest'],
      extends: ['plugin:jest/recommended'],
      rules: {
        'jest/no-disabled-tests': 'warn',
        'jest/no-focused-tests': 'error',
        'jest/no-identical-title': 'error',
        'jest/prefer-to-have-length': 'warn',
        'jest/valid-expect': 'error',
      },
    },
  ],
};
