---
name: Cognitive Prep
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#40474f'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#707881'
  outline-variant: '#c0c7d1'
  surface-tint: '#006399'
  primary: '#00507d'
  on-primary: '#ffffff'
  primary-container: '#0369a1'
  on-primary-container: '#cbe4ff'
  inverse-primary: '#94ccff'
  secondary: '#576065'
  on-secondary: '#ffffff'
  secondary-container: '#dbe4ea'
  on-secondary-container: '#5d666b'
  tertiary: '#494c4e'
  on-tertiary: '#ffffff'
  tertiary-container: '#616466'
  on-tertiary-container: '#dfe1e3'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#cde5ff'
  primary-fixed-dim: '#94ccff'
  on-primary-fixed: '#001d32'
  on-primary-fixed-variant: '#004b74'
  secondary-fixed: '#dbe4ea'
  secondary-fixed-dim: '#bfc8ce'
  on-secondary-fixed: '#141d21'
  on-secondary-fixed-variant: '#3f484d'
  tertiary-fixed: '#e0e3e5'
  tertiary-fixed-dim: '#c4c7c9'
  on-tertiary-fixed: '#191c1e'
  on-tertiary-fixed-variant: '#444749'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  headline-lg:
    fontFamily: Hanken Grotesk
    fontSize: 30px
    fontWeight: '700'
    lineHeight: 38px
  headline-md:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
  headline-sm:
    fontFamily: Hanken Grotesk
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 26px
  body-lg:
    fontFamily: Hanken Grotesk
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Hanken Grotesk
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-md:
    fontFamily: Hanken Grotesk
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
  label-sm:
    fontFamily: Hanken Grotesk
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
  headline-lg-mobile:
    fontFamily: Hanken Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  container-max: 1200px
  gutter: 1.5rem
  margin-page: 2rem
  stack-sm: 0.5rem
  stack-md: 1rem
  stack-lg: 2rem
---

## Brand & Style

The brand personality is professional, encouraging, and highly organized, tailored for high-stakes career preparation. The target audience includes software engineers and students who require a distraction-free, structured environment for cognitive training.

The design style is **Corporate / Modern** with subtle **Minimalist** influences. It prioritizes clarity and information hierarchy through a clean, light-filled interface. The aesthetic relies on soft-edged containers, ample white space, and a refined "tech-professional" color palette to evoke a sense of calm focus and reliability. Visual complexity is minimized to allow the educational content to take center stage.

## Colors

The palette is anchored by a deep **Cyan-Blue (#0369A1)** as the primary brand color, used for calls to action, active states, and critical information. 

- **Backgrounds:** A very soft lavender-tinted gray (#F8FAFC) serves as the primary canvas, providing a gentler alternative to pure white.
- **Surfaces:** Cards and containers use pure white (#FFFFFF) to pop against the background.
- **Accents:** Selection states and secondary backgrounds utilize a pale sky blue (#F0F9FF).
- **Typography:** Deep slate tones are used for high legibility, with lighter grays (#94A3B8) reserved for secondary metadata and icons.

## Typography

The system utilizes **Hanken Grotesk** across all levels to maintain a contemporary, sharp, and highly legible feel typical of modern SaaS platforms. 

- **Headlines:** Use Bold (700) or Semi-Bold (600) weights with tight tracking to establish clear section hierarchy.
- **Body:** Standard body text uses a 14px base (Regular 400) for high-density information display, scaling to 16px for narrative content.
- **Labels:** Small labels and tags use Medium (500) weight to ensure readability even at small scale (12px-13px).

## Layout & Spacing

The design system employs a **Fixed Grid** philosophy for the main content area, centering information within a **1200px max-width container**.

- **Navigation:** A persistent horizontal top bar spans the full width, housing the logo and primary navigation links.
- **Grid:** A 12-column grid is used for desktop, reflowing to a single column for mobile devices.
- **Rhythm:** An 8px base unit drives all spacing. Page margins are set to 32px (2rem), while internal card padding is consistently 24px (1.5rem).
- **Responsive:** Below 768px, the layout transitions to a fluid mobile view with reduced 16px side margins.

## Elevation & Depth

Hierarchy is established through **Tonal Layers** combined with **Low-contrast outlines**.

- **Cards:** White surfaces feature a very fine 1px border (#E2E8F0) and a subtle, large-radius shadow (0px 4px 12px rgba(0,0,0,0.03)). This creates a soft "lifted" effect without feeling heavy.
- **Interactive Layers:** Elements like dropdowns or modals use a more pronounced shadow to indicate priority over the base content.
- **Background Depth:** The lavender-gray background acts as the lowest layer, while all interactive content sits on white "elevated" cards.

## Shapes

The shape language is consistently **Rounded**, using a 0.5rem (8px) base radius for standard cards and input fields. 

- **Buttons & Pills:** These follow a strict **Pill-shaped** (Full Round) style to differentiate them from the structural containers. This soft geometry makes interactive elements feel approachable and distinct from the rectangular information blocks.
- **Status Indicators:** Small dots and selection markers are circular to maintain the friendly, soft-edged theme.

## Components

- **Buttons:** All buttons must be pill-shaped. Primary buttons use a solid primary blue background with white text. Secondary buttons use the sky-blue accent background with primary blue text.
- **Selection Pills (Tags/Chips):** Used for filtering and categorization. They feature high roundedness (full pill) and use light gray or tinted backgrounds with dark text. The "Active" state uses the primary blue color.
- **Cards:** White background, 8px corner radius, 1px light border, and soft shadow. Content within cards should follow a consistent vertical rhythm.
- **Input Fields:** Large search bars or text inputs should have an 8px radius, a subtle border, and a magnifying glass icon for search affordance.
- **List Items:** Use horizontal rows within cards, separated by thin 1px dividers or subtle background shifts on hover.
- **Progress Indicators:** Use thin linear bars or circular percentage rings in the primary cyan-blue to show learning progress.