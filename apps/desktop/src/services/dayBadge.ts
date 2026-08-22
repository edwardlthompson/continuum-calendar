export async function renderBadgePng(count: number): Promise<number[]> {
  if (count <= 0 || typeof document === 'undefined') return []
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
  ctx.font = 'bold 15px Segoe UI, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(count > 99 ? '99+' : String(count), 16, 17)
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/png'))
  if (!blob) return []
  return Array.from(new Uint8Array(await blob.arrayBuffer()))
}
