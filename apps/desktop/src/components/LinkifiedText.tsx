import { openExternal } from '../about/openExternal'
import { linkifyText } from '../lib/linkifyText'

export function LinkifiedText(props: { text: string; className?: string }) {
  const segments = linkifyText(props.text)
  return (
    <span className={props.className ?? 'whitespace-pre-wrap break-words'}>
      {segments.map((seg, i) => {
        if (seg.type === 'text') {
          return <span key={i}>{seg.value}</span>
        }
        return (
          <button
            key={i}
            type="button"
            className="text-[var(--cc-accent)] underline underline-offset-2 hover:opacity-90"
            onClick={(e) => {
              e.preventDefault()
              e.stopPropagation()
              void openExternal(seg.href)
            }}
          >
            {seg.value}
          </button>
        )
      })}
    </span>
  )
}
