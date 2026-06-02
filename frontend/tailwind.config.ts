import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#0f766e",
        "primary-container": "#14b8a6",
        "on-primary": "#ffffff",
        "on-primary-container": "#ccfbf1",
        surface: "#eaf3f3",
        "surface-container-lowest": "#ffffff",
        "surface-container-low": "#f6fbfb",
        "surface-container": "#dfecec",
        "surface-container-high": "#c9dada",
        "surface-container-highest": "#a8bcbc",
        "on-surface": "#0f172a",
        "on-surface-variant": "#42545a",
        secondary: "#d97706",
        "secondary-container": "#fef3c7",
        "on-secondary-container": "#92400e",
        outline: "#667b80",
        "outline-variant": "#b9caca",
        "surface-variant": "#dfecec",
        background: "#eaf3f3",
        error: "#ba1a1a",
        "error-container": "#ffdad6",
        "on-error-container": "#93000a",
        "inverse-surface": "#0f172a",
        "inverse-on-surface": "#f8fafc",
      },
      fontFamily: {
        sans: ["Inter", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
      },
    },
  },
  plugins: [],
};
export default config;
