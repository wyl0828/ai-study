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
        primary: "#005a90",
        "primary-container": "#0073b7",
        "on-primary": "#ffffff",
        "on-primary-container": "#ebf3ff",
        surface: "#faf8ff",
        "surface-container-lowest": "#ffffff",
        "surface-container-low": "#f2f3ff",
        "surface-container": "#eaedff",
        "surface-container-high": "#e2e7ff",
        "surface-container-highest": "#dae2fd",
        "on-surface": "#131b2e",
        "on-surface-variant": "#404750",
        secondary: "#515f74",
        "secondary-container": "#d5e3fc",
        "on-secondary-container": "#57657a",
        outline: "#707882",
        "outline-variant": "#c0c7d2",
        "surface-variant": "#dae2fd",
        background: "#faf8ff",
        error: "#ba1a1a",
        "error-container": "#ffdad6",
        "on-error-container": "#93000a",
        "inverse-surface": "#283044",
        "inverse-on-surface": "#eef0ff",
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
