export function badgeLabel(count: number): string {
  if (count <= 0) return ''
  return count > 99 ? '99+' : String(count)
}

function canvasPngBytes(canvas: HTMLCanvasElement): number[] {
  const dataUrl = canvas.toDataURL('image/png')
  const b64 = dataUrl.split(',')[1] ?? ''
  if (!b64) return []
  const bin = atob(b64)
  const out = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i)
  return Array.from(out)
}

function loadMarkImage(): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error('mark'))
    img.src = '/continuum-mark.png'
  })
}

/** App mark; remaining count is a small red overlay, idle is the logo only. */
export async function renderBadgePng(count: number): Promise<number[]> {
  if (typeof document === 'undefined') return []
  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 32
  const ctx = canvas.getContext('2d')
  if (!ctx) return []
  try {
    ctx.drawImage(await loadMarkImage(), 0, 0, 32, 32)
  } catch {
    return []
  }
  const label = badgeLabel(count)
  if (label) {
    ctx.fillStyle = '#d32f2f'
    ctx.beginPath()
    ctx.arc(23, 23, 9, 0, Math.PI * 2)
    ctx.fill()
    ctx.fillStyle = '#ffffff'
    ctx.font = label.length > 2 ? 'bold 8px Segoe UI, sans-serif' : 'bold 11px Segoe UI, sans-serif'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.fillText(label, 23, 24)
  }
  return canvasPngBytes(canvas)
}

/** Taskbar overlay only: red count, no app mark. */
export function renderOverlayPng(count: number): number[] {
  if (typeof document === 'undefined') return []
  const label = badgeLabel(count)
  if (!label) return []
  const canvas = document.createElement('canvas')
  canvas.width = 32
  canvas.height = 32
  const ctx = canvas.getContext('2d')
  if (!ctx) return []
  ctx.fillStyle = '#d32f2f'
  ctx.beginPath()
  ctx.arc(16, 16, 15, 0, Math.PI * 2)
  ctx.fill()
  ctx.fillStyle = '#ffffff'
  ctx.font = label.length > 2 ? 'bold 11px Segoe UI, sans-serif' : 'bold 15px Segoe UI, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(label, 16, 17)
  return canvasPngBytes(canvas)
}
