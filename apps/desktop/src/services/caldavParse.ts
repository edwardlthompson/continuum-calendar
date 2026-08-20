/** Pure CalDAV XML helpers (no fetch) — unit-tested. */

export function decodeXmlText(raw: string): string {
  return raw
    .replace(/<!\[CDATA\[([\s\S]*?)]]>/g, '$1')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&amp;/g, '&')
    .replace(/&quot;/g, '"')
    .trim()
}

export function extractCalendarDataBlocks(xml: string): string[] {
  const blocks: string[] = []
  const re = /<(?:[\w-]+:)?calendar-data(?:\s[^>]*)?>([\s\S]*?)<\/(?:[\w-]+:)?calendar-data>/gi
  let match: RegExpExecArray | null
  while ((match = re.exec(xml))) {
    const ics = decodeXmlText(match[1] ?? '')
    if (ics.includes('BEGIN:VEVENT')) blocks.push(ics)
  }
  return blocks
}

export function parseCalDavDisplayName(xml: string): string | undefined {
  const match = xml.match(
    /<(?:[\w-]+:)?displayname(?:\s[^>]*)?>([\s\S]*?)<\/(?:[\w-]+:)?displayname>/i,
  )
  const name = decodeXmlText(match?.[1]?.replace(/<[^>]+>/g, '') ?? '')
  return name || undefined
}

export function calDavTimeRange(from: Date, to: Date): { start: string; end: string } {
  const fmt = (d: Date) =>
    d.toISOString().replace(/[-:]/g, '').replace(/\.\d{3}/, '')
  return { start: fmt(from), end: fmt(to) }
}
