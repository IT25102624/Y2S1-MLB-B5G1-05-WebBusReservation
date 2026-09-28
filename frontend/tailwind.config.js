/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eefdf4',
          100: '#d6f9e2',
          200: '#aef1c4',
          300: '#78e39d',
          400: '#42cf76',
          500: '#1fb85a',
          600: '#149548',
          700: '#12773c',
          800: '#125e33',
          900: '#104d2c'
        },
        ink: {
          950: '#0a0f0c',
          900: '#0f1712',
          800: '#16211a',
          700: '#1d2e24'
        },
        sand: {
          50: '#fbfaf6',
          100: '#f5f2ea'
        },
        ops: {
          950: '#07090f',
          900: '#0b0e17',
          800: '#121729',
          700: '#1a2038'
        },
        signal: {
          cyan: '#5eead4',
          rose: '#fb7185',
          amber: '#ffb84d'
        }
      },
      fontFamily: {
        display: ['"Bricolage Grotesque"', 'sans-serif'],
        body: ['"Manrope"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace']
      },
      boxShadow: {
        card: '0 20px 45px -20px rgba(16, 77, 44, 0.35)',
        'card-lg': '0 30px 70px -25px rgba(16, 77, 44, 0.45)',
        glow: '0 0 40px -8px rgba(94, 234, 212, 0.4)'
      },
      backgroundImage: {
        grain: "url(\"data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='120' height='120'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='2' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.035'/%3E%3C/svg%3E\")"
      }
    }
  },
  plugins: []
}
