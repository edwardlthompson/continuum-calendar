/** WCAG 2.x relative luminance and contrast for Continuum CTA tokens. */

function hexToRgb(hex: string): [number, number, number] | null {
  const h = hex.trim().replace(/^#/, '')
  if (!/^[0-9a-fA-F]{6}$/.test(h)) return null
  return [parseInt(h.slice(0, 2), 16), parseInt(h.slice(2, 4), 16), parseInt(h.slice(4, 6), 16)]
}

function channel(c: number): number {
  const s = c / 255
  return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
}

export function relativeLuminance(hex: string): number | null {
  const rgb = hexToRgb(hex)
  if (!rgb) return null
  const [r, g, b] = rgb.map(channel) as [number, number, number]
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

/** Contrast ratio of two `#rrggbb` colors. Invalid hex → 0. */
export function contrastRatio(a: string, b: string): number {
  const la = relativeLuminance(a)
  const lb = relativeLuminance(b)
  if (la == null || lb == null) return 0
  const light = Math.max(la, lb)
  const dark = Math.min(la, lb)
  return (light + 0.05) / (dark + 0.05)
}

export const CC_DARK_ACCENT = '#4eb6d4'
export const CC_DARK_ON_ACCENT = '#0b1220'
export const CC_LIGHT_ACCENT = '#0f6e8c'
export const CC_LIGHT_ON_ACCENT = '#ffffff'
