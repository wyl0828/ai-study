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
        primary: "#075985",
        "primary-container": "#0284c7",
        "on-primary": "#ffffff",
        "on-primary-container": "#e0f2fe",
        surface: "#f6f8fb",
        "surface-container-lowest": "#ffffff",
        "surface-container-low": "#f8fafc",
        "surface-container": "#eef3f8",
        "surface-container-high": "#e2e8f0",
        "surface-container-highest": "#cbd5e1",
        "on-surface": "#0f172a",
        "on-surface-variant": "#475569",
        secondary: "#475569",
        "secondary-container": "#e2e8f0",
        "on-secondary-container": "#334155",
        outline: "#64748b",
        "outline-variant": "#cbd5e1",
        "surface-variant": "#e2e8f0",
        background: "#f6f8fb",
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
