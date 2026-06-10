import { getErrorMessage } from '../../api/client'

/*
 * Reusable error state (P5-4) for a failed query within a dashboard card. Shows a
 * graceful, human-readable message (derived from the backend's uniform error
 * shape via getErrorMessage) and a Retry button that re-runs the query — this is
 * the "graceful error + retry on failure" behavior from docs/testing-plan.md.
 * Rendered inside the card region that failed so the rest of the page stays
 * usable; `onRetry` is wired to the TanStack Query `refetch`.
 */
type ErrorStateProps = {
  /** The error thrown by the query (axios/API error or generic Error). */
  error: unknown
  /** Re-run the failed query (TanStack Query `refetch`). */
  onRetry: () => void
  /** Optional override for the leading message; defaults to a generic line. */
  title?: string
}

function ErrorState({ error, onRetry, title = "Couldn't load this section" }: ErrorStateProps) {
  return (
    <div role="alert" className="flex flex-col items-center gap-3 py-8 text-center">
      <div>
        <p className="text-sm font-semibold text-ink">{title}</p>
        <p className="mt-1 text-[13px] text-muted">{getErrorMessage(error)}</p>
      </div>
      <button
        type="button"
        onClick={onRetry}
        className="inline-flex cursor-pointer items-center rounded-[10px] border border-line bg-card px-4 py-2 text-sm font-medium text-ink shadow-card"
      >
        Retry
      </button>
    </div>
  )
}

export default ErrorState
