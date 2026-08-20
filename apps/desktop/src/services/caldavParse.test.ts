import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
  calDavTimeRange,
  decodeXmlText,
  extractCalendarDataBlocks,
  parseCalDavDisplayName,
} from './caldavParse.ts'

test('extracts calendar-data ICS from Multi-Status XML', () => {
  const xml = `<?xml version="1.0"?>
<d:multistatus xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
  <d:response>
    <c:calendar-data><![CDATA[BEGIN:VCALENDAR
BEGIN:VEVENT
SUMMARY:Standup
END:VEVENT
END:VCALENDAR]]></c:calendar-data>
  </d:response>
</d:multistatus>`
  const blocks = extractCalendarDataBlocks(xml)
  assert.equal(blocks.length, 1)
  assert.match(blocks[0] ?? '', /SUMMARY:Standup/)
})

test('decodeXmlText unescapes entities', () => {
  assert.equal(decodeXmlText('&lt;BEGIN:VEVENT&gt;'), '<BEGIN:VEVENT>')
})

test('parseCalDavDisplayName reads DAV displayname', () => {
  const xml = `<d:prop xmlns:d="DAV:"><d:displayname>Work</d:displayname></d:prop>`
  assert.equal(parseCalDavDisplayName(xml), 'Work')
})

test('calDavTimeRange is UTC compact ICS', () => {
  const { start } = calDavTimeRange(new Date('2026-08-20T00:00:00.000Z'), new Date('2026-11-18T00:00:00.000Z'))
  assert.equal(start, '20260820T000000Z')
})
