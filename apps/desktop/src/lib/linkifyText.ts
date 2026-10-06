export type LinkifySegment =
  | { type: 'text'; value: string }
  | { type: 'link'; value: string; href: string }

/** Stop before whitespace, HTML delimiters, and quotes so href="…" does not swallow ">View". */
const URL_RE =
  /(?:https?:\/\/[^\s<>"'`]+|www\.[^\s<>"'`]+|mailto:[^\s<>"'`]+)/gi

const TRAILING_PUNCT_RE = /[.,;:!?)\]}'"]+$/

const ANCHOR_RE = /<a\b[^>]*\bhref\s*=\s*(["'])(.*?)\1[^>]*>([\s\S]*?)<\/a>/gi

function decodeEntities(s: string): string {
  return s
    .replace(/&nbsp;/gi, ' ')
    .replace(/&amp;/gi, '&')
    .replace(/&lt;/gi, '<')
    .replace(/&gt;/gi, '>')
    .replace(/&quot;/gi, '"')
    .replace(/&#39;/g, "'")
    .replace(/&#x27;/gi, "'")
}

function stripTags(s: string): string {
  return s
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n')
    .replace(/<[^>]+>/g, '')
}

function normalizeHref(raw: string): string | null {
  let candidate = decodeEntities(raw.trim())
  const trailing = candidate.match(TRAILING_PUNCT_RE)
  if (trailing) {
    candidate = candidate.slice(0, -trailing[0].length)
  }
  let href = candidate
  if (/^www\./i.test(href)) {
    href = `https://${href}`
  }
  try {
    const u = new URL(href)
    const protocol = u.protocol.toLowerCase()
    if (protocol !== 'http:' && protocol !== 'https:' && protocol !== 'mailto:') {
      return null
    }
    return href
  } catch {
    return null
  }
}

function coalesce(segments: LinkifySegment[]): LinkifySegment[] {
  const out: LinkifySegment[] = []
  for (const seg of segments) {
    if (seg.type === 'text' && seg.value === '' && out.length > 0) continue
    const prev = out[out.length - 1]
    if (seg.type === 'text' && prev?.type === 'text') {
      prev.value += seg.value
    } else {
      out.push({ ...seg })
    }
  }
  return out.length > 0 ? out : [{ type: 'text', value: '' }]
}

function linkifyPlain(text: string): LinkifySegment[] {
  if (!text) return []
  const segments: LinkifySegment[] = []
  let last = 0
  URL_RE.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = URL_RE.exec(text)) !== null) {
    const raw = match[0]
    const start = match.index
    if (start > last) {
      segments.push({ type: 'text', value: text.slice(last, start) })
    }
    const trailing = raw.match(TRAILING_PUNCT_RE)
    const core = trailing ? raw.slice(0, -trailing[0].length) : raw
    const punct = trailing ? trailing[0] : ''
    const href = normalizeHref(core)
    if (href) {
      segments.push({ type: 'link', value: core, href })
      if (punct) segments.push({ type: 'text', value: punct })
    } else {
      segments.push({ type: 'text', value: raw })
    }
    last = start + raw.length
  }
  if (last < text.length) {
    segments.push({ type: 'text', value: text.slice(last) })
  }
  return segments
}

/**
 * Split plain text (or light HTML from Google Calendar) into text/link segments.
 * Parses `<a href>` into labeled links; strips other tags; allowlists http(s)/mailto only.
 */
export function linkifyText(input: string | null | undefined): LinkifySegment[] {
  const text = input ?? ''
  if (!text) return [{ type: 'text', value: '' }]

  const segments: LinkifySegment[] = []
  let last = 0
  ANCHOR_RE.lastIndex = 0
  let match: RegExpExecArray | null
  while ((match = ANCHOR_RE.exec(text)) !== null) {
    if (match.index > last) {
      segments.push(...linkifyPlain(stripTags(decodeEntities(text.slice(last, match.index)))))
    }
    const href = normalizeHref(match[2] ?? '')
    const labelRaw = stripTags(decodeEntities(match[3] ?? '')).trim()
    if (href) {
      segments.push({ type: 'link', value: labelRaw || href, href })
    } else if (labelRaw) {
      segments.push({ type: 'text', value: labelRaw })
    }
    last = match.index + match[0].length
  }
  if (last < text.length) {
    segments.push(...linkifyPlain(stripTags(decodeEntities(text.slice(last)))))
  }
  return coalesce(segments)
}
