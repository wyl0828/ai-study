---
name: Technical Interview Excellence
colors:
  surface: '#faf8ff'
  surface-dim: '#d2d9f4'
  surface-bright: '#faf8ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f3ff'
  surface-container: '#eaedff'
  surface-container-high: '#e2e7ff'
  surface-container-highest: '#dae2fd'
  on-surface: '#131b2e'
  on-surface-variant: '#404750'
  inverse-surface: '#283044'
  inverse-on-surface: '#eef0ff'
  outline: '#707882'
  outline-variant: '#c0c7d2'
  surface-tint: '#00629e'
  primary: '#005a90'
  on-primary: '#ffffff'
  primary-container: '#0073b7'
  on-primary-container: '#ebf3ff'
  inverse-primary: '#99cbff'
  secondary: '#515f74'
  on-secondary: '#ffffff'
  secondary-container: '#d5e3fc'
  on-secondary-container: '#57657a'
  tertiary: '#844600'
  on-tertiary: '#ffffff'
  tertiary-container: '#a85a00'
  on-tertiary-container: '#ffefe6'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cfe5ff'
  primary-fixed-dim: '#99cbff'
  on-primary-fixed: '#001d34'
  on-primary-fixed-variant: '#004a78'
  secondary-fixed: '#d5e3fc'
  secondary-fixed-dim: '#b9c7df'
  on-secondary-fixed: '#0d1c2e'
  on-secondary-fixed-variant: '#3a485b'
  tertiary-fixed: '#ffdcc3'
  tertiary-fixed-dim: '#ffb77e'
  on-tertiary-fixed: '#2f1500'
  on-tertiary-fixed-variant: '#6e3900'
  background: '#faf8ff'
  on-background: '#131b2e'
  surface-variant: '#dae2fd'
typography:
  h1:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  h2:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: -0.01em
  h3:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: '1.4'
    letterSpacing: '0'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: '0'
  body-base:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: '0'
  body-sm:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: '0'
  code-base:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: '0'
  label-caps:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 12px
  md: 24px
  lg: 40px
  xl: 64px
  gutter: 24px
  container-max: 1280px
---

## Brand & Style

The brand personality of this design system is authoritative, precise, and intellectually rigorous. It is engineered for software professionals who value efficiency and technical depth over decorative flair. The aesthetic sits at the intersection of a high-end Integrated Development Environment (IDE) and premium technical documentation.

The design style follows a **Minimalist Corporate** approach with a focus on logical information density. It prioritizes clarity and high-contrast readability to reduce cognitive load during intense coding sessions. Every element serves a functional purpose, utilizing whitespace not just for aesthetics, but to delineate complex hierarchies of code and logic. The overall feel is one of a "professional workbench"—stable, reliable, and performance-oriented.

## Colors

This design system utilizes a palette that mirrors the classic Java ecosystem while modernizing it for web-based learning. 

- **Primary:** The "Java Blue" (#0073b7) is used for primary actions, progress indicators, and key brand touchpoints.
- **Neutrals:** A scale of Slate Grays (from #0f172a to #f8fafc) provides the technical framework. Darker slates are used for text and iconography, while lighter slates create the "surface-container" tiers typical of an IDE.
- **Feedback:** Success and Error colors are saturated and high-contrast, ensuring that test results and compiler errors are immediately identifiable.
- **Surface:** The background uses a very light slate (#f8fafc) to reduce eye strain compared to pure white, maintaining a "clean paper" feel for documentation reading.

## Typography

Typography is the backbone of this design system, split between UI navigation and code consumption.

- **UI Sans (Inter):** Chosen for its exceptional legibility and systematic feel. It handles high-density data and nested navigation menus without becoming cluttered.
- **Code Monospace (JetBrains Mono):** This is the gold standard for Java development. It is used for all code snippets, terminal outputs, and inline technical terms. 
- **Hierarchy:** Headlines are bold and tight to anchor sections of documentation. Labels use an uppercase style with slight tracking for categorization, such as identifying "Difficulty Levels" or "Topic Tags."

## Layout & Spacing

This design system employs a **Fixed Grid** philosophy for content-heavy pages (like lessons and problem statements) to ensure optimal line lengths for reading. For the dashboard and coding editor environments, it shifts to a **Fluid Grid** to maximize screen real estate.

The spacing rhythm is built on an 8px base unit. 
- **Margins:** Standard 24px margins provide a clear gutter between layout columns.
- **Content Width:** Documentation and interview questions are capped at a readable width (approx. 720px within the 1280px container) to prevent horizontal eye fatigue.
- **Density:** The system allows for a "Compact" mode in the IDE view, reducing vertical padding to allow more lines of code to be visible at once.

## Elevation & Depth

To maintain a crisp, developer-centric feel, depth is handled primarily through **low-contrast outlines** and **tonal layering** rather than heavy shadows.

- **The IDE Stack:** Surfaces are layered by color. The main background is the lightest, while sidebars and navigation panels use a slightly darker or "sunken" slate tone.
- **Cards:** Used to encapsulate interview questions or modules. They feature a 1px solid border (#e2e8f0) and a very subtle, diffused shadow (0px 2px 4px rgba(0,0,0,0.05)) to distinguish them from the background without feeling bulky.
- **Interaction:** Hover states on interactive cards should see the shadow intensify slightly and the border color shift to the Primary Java Blue.

## Shapes

The shape language is strictly **Soft (Level 1)**. This implies a 4px (0.25rem) base radius.

This subtle rounding strikes a balance: it feels modern and polished, but avoids the "friendly/bubbly" aesthetic of consumer-facing social apps. Buttons, input fields, and code blocks all share this 4px radius. Large containers or feature cards may use a 8px (0.5rem) radius to define major layout sections. This geometric consistency reinforces the "technical documentation" feel.

## Components

The components of this design system are designed for high utility and developer familiarity.

- **Primary Button:** Solid Java Blue (#0073b7) with white Inter-bold text. Used for "Submit Solution" or "Start Interview."
- **Secondary/Ghost Button:** Slate-600 border and text. Used for "Save Draft" or "View Hint."
- **Code Editor:** Must utilize JetBrains Mono. The container should have a dark slate background (#1e293b) to provide high-contrast syntax highlighting, even in the light-themed UI.
- **Status Chips:** Small, pill-like badges for "Easy", "Medium", and "Hard." They use low-saturation background tints of success, warning, and error colors respectively.
- **Test Results Card:** A specialized card component with a 4px left-border accent (Success Green or Error Red). It includes a "Trace" section using the monospace font for compiler output.
- **Sidebar Navigation:** A multi-level tree structure reminiscent of an IDE's file explorer, using `body-sm` typography and subtle hover highlights.
- **Input Fields:** Crisp, 1px bordered boxes that turn Java Blue on focus, with a clear monospace font for inputting variable names or parameters.