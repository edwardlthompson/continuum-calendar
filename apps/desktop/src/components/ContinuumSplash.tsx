import { useEffect, useState } from 'react'
import { SplashMark } from '../illustrations/Marks'

/** Neon marketing splash — short fade on cold start. App icon stays flat 2D mark. */
export function ContinuumSplash() {
  const [phase, setPhase] = useState<'in' | 'out' | 'done'>('in')

  useEffect(() => {
    let reduce = false
    try {
      reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    } catch {
      reduce = false
    }
    if (reduce) {
      setPhase('done')
      return
    }
    const fade = window.setTimeout(() => setPhase('out'), 1200)
    const done = window.setTimeout(() => setPhase('done'), 1700)
    return () => {
      window.clearTimeout(fade)
      window.clearTimeout(done)
    }
  }, [])

  if (phase === 'done') return null

  return (
    <div
      className={`cc-splash${phase === 'out' ? ' cc-splash--out' : ''}`}
      aria-hidden="true"
    >
      <div className="flex flex-col items-center gap-3 text-[var(--cc-accent)]">
        <img
          className="cc-splash__mark"
          src="/continuum-splash-neon.png"
          alt=""
          width={320}
          height={320}
          draggable={false}
        />
        <SplashMark />
      </div>
    </div>
  )
}
