/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts,scss}"
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        // Basalt brand palette — mirrors Gemini's dark slate
        basalt: {
          bg:        '#0f0f11',   // deep background
          surface:   '#1e1f25',   // card / bubble background
          surface2:  '#2a2b33',   // slightly lighter surface
          border:    '#3a3b45',   // subtle borders
          accent:    '#8ab4f8',   // blue accent (Gemini-like)
          accentHov: '#aecbfa',
          text:      '#e8eaed',   // primary text
          muted:     '#9aa0a6',   // secondary/muted text
          user:      '#1a3a5c',   // user message bubble background
          ai:        '#1e1f25',   // AI message bubble background
        }
      },
      fontFamily: {
        sans: ['Google Sans', 'Roboto', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        mono: ['Roboto Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      typography: (theme) => ({
        basalt: {
          css: {
            '--tw-prose-body': theme('colors.basalt.text'),
            '--tw-prose-headings': theme('colors.basalt.text'),
            '--tw-prose-code': theme('colors.basalt.accent'),
            '--tw-prose-pre-bg': theme('colors.basalt.surface2'),
            color: theme('colors.basalt.text'),
          }
        }
      })
    },
  },
  plugins: [
    require('@tailwindcss/typography'),
  ],
};

