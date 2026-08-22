export function AuthReconnectBanner(props: { onSignIn: () => void; message?: string }) {
  return (
    <div
      role="alert"
      className="flex flex-wrap items-center justify-between gap-2 rounded-lg border-2 border-[var(--cc-brand-now)] bg-[var(--cc-surface)] px-3 py-2"
    >
      <p className="min-w-0 text-sm font-medium text-[var(--cc-text)]">
        {props.message ??
          'Google sign-in expired. Events you edit stay on this PC. Sign in again to sync with your phone.'}
      </p>
      <button
        type="button"
        className="shrink-0 rounded bg-[var(--cc-brand-now)] px-3 py-1.5 text-sm font-medium text-white"
        onClick={props.onSignIn}
      >
        Sign in again
      </button>
    </div>
  )
}
