module.exports = {
  content: [
    './src/**/*',
    './resources/**/*',
  ],
  theme: {
    extend: {
      fontFamily: {
        "giza": ["Giza", "sans-serif"],
        "giza-stencil": ["Giza-Stencil", "sans-serif"],
        "sans": ["Inter", "ui-sans-serif", "system-ui", "-apple-system", "sans-serif"],
      },
      colors: {
        // Emerald is the product accent. Exposed as `brand` so intent is explicit.
        brand: {
          50:  "#ecfdf5",
          100: "#d1fae5",
          500: "#10b981",
          600: "#059669",
          700: "#047857",
          900: "#064e3b",
        },
      },
      boxShadow: {
        // Layered, zinc-tinted shadows for softer, more premium depth.
        card: "0 1px 2px 0 rgb(24 24 27 / 0.04), 0 1px 3px 0 rgb(24 24 27 / 0.06)",
        "card-md": "0 4px 10px -2px rgb(24 24 27 / 0.08), 0 2px 6px -2px rgb(24 24 27 / 0.05)",
        "card-hover": "0 10px 26px -6px rgb(24 24 27 / 0.12), 0 4px 10px -4px rgb(24 24 27 / 0.07)",
      },
      borderRadius: {
        xl: "0.75rem",
        "2xl": "1rem",
      },
    },
  },
  plugins: [
    require('@tailwindcss/forms'),
  ],
}
