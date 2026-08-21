export function UpdateAvailableDialog(props: {
  version: string
  onLater: () => void
  onInstall: () => void
}) {
  return (
    <div
      className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="cc-update-title"
    >
      <div className="w-full max-w-md space-y-3 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4 shadow-xl">
        <h2 id="cc-update-title" className="text-base font-semibold text-[var(--cc-text)]">
          Update available
        </h2>
        <p className="text-sm text-[var(--cc-muted)]">
          Continuum Calendar {props.version} is on GitHub. Install it when you are ready — this
          prompt is only about the app, not donations.
        </p>
        <div className="flex flex-wrap justify-end gap-2 pt-1">
          <button type="button" className="rounded px-3 py-1.5 text-sm" onClick={props.onLater}>
            Later
          </button>
          <button
            type="button"
            className="rounded bg-[var(--cc-accent)] px-3 py-1.5 text-sm font-medium text-white"
            onClick={props.onInstall}
          >
            Install
          </button>
        </div>
      </div>
    </div>
  )
}

export function DonateNudgeDialog(props: { onLater: () => void; onDonate: () => void }) {
  return (
    <div
      className="absolute inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="cc-donate-title"
    >
      <div className="w-full max-w-md space-y-3 rounded-xl border border-[var(--cc-border)] bg-[var(--cc-surface)] p-4 shadow-xl">
        <h2 id="cc-donate-title" className="text-base font-semibold text-[var(--cc-text)]">
          Development is still going
        </h2>
        <p className="text-sm text-[var(--cc-muted)]">
          You just got a new Continuum build. If it helps you, you can support ongoing work on
          Venmo. This is optional and will not appear again until the next update.
        </p>
        <div className="flex flex-wrap justify-end gap-2 pt-1">
          <button type="button" className="rounded px-3 py-1.5 text-sm" onClick={props.onLater}>
            Not now
          </button>
          <button
            type="button"
            className="rounded bg-[var(--cc-accent)] px-3 py-1.5 text-sm font-medium text-white"
            onClick={props.onDonate}
          >
            Donate via Venmo
          </button>
        </div>
      </div>
    </div>
  )
}
