/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ["./src/**/*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      colors: {
        background: '#0f172a',
        card: '#1e293b',
        border: '#334155',
        primary: '#3b82f6',
        success: '#10b981',
        muted: '#94a3b8',
      },
    },
  },
  plugins: [],
}