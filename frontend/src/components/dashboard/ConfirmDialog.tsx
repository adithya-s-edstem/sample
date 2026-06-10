import { useEffect, useId, useRef } from 'react'
import { useFocusTrap } from '../../lib/useFocusTrap'

/*
 * Small confirm prompt (P7-4) shown before a destructive action — currently the
 * row delete (docs/solution.md §4: "the 🗑 icon triggers a small confirm prompt
 * before deleting"). It mirrors the ExpenseModal chrome (dimmed overlay, centered
 * dialog, Escape/overlay-to-dismiss) but is action-agnostic: the caller supplies
 * the copy and the confirm handler.
 *
 * Purely presentational — it owns no network state. `busy` disables the actions
 * while the caller's delete mutation is in flight; `error` surfaces a failed
 * delete inline so the user can retry or cancel. The confirm button is styled as
 * a danger action.
 */
type ConfirmDialogProps = {
  title: string
  /** Body copy explaining what will happen. */
  message: string
  /** Label for the confirm (destructive) button; defaults to "Delete". */
  confirmLabel?: string
  /** Runs the destructive action; the caller closes the dialog on success. */
  onConfirm: () => void
  /** Dismisses without acting (Cancel, overlay click, or Escape). */
  onClose: () => void
  /** Disables the actions while the action is in flight. */
  busy?: boolean
  /** A failure message from the action; shown inline so the user can retry. */
  error?: string
}

function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Delete',
  onConfirm,
  onClose,
  busy = false,
  error,
}: ConfirmDialogProps) {
  const titleId = useId()
  const messageId = useId()
  const confirmRef = useRef<HTMLButtonElement>(null)
  // Trap focus within the dialog and restore it to the trigger on close (P9-1).
  const dialogRef = useFocusTrap<HTMLDivElement>()

  // Close on Escape from anywhere in the dialog (unless an action is in flight).
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape' && !busy) onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose, busy])

  // Focus the confirm action when the dialog opens.
  useEffect(() => {
    confirmRef.current?.focus()
  }, [])

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-[rgba(17,24,39,0.45)] px-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !busy) onClose()
      }}
    >
      <div
        ref={dialogRef}
        role="alertdialog"
        aria-modal="true"
        aria-labelledby={titleId}
        aria-describedby={messageId}
        className="w-full max-w-[400px] rounded-2xl bg-card p-7 shadow-[0_20px_50px_rgba(16,24,40,0.25)]"
      >
        <h3 id={titleId} className="text-[19px] font-bold text-ink">
          {title}
        </h3>
        <p id={messageId} className="mb-5 mt-1 text-[13px] text-muted">
          {message}
        </p>

        {error && (
          <p
            role="alert"
            className="mb-4 rounded-[10px] border border-danger bg-danger/5 px-3.5 py-2.5 text-sm text-danger"
          >
            {error}
          </p>
        )}

        <div className="mt-6 flex justify-end gap-2.5">
          <button
            type="button"
            onClick={onClose}
            disabled={busy}
            className="inline-flex cursor-pointer items-center rounded-[10px] border border-line bg-card px-4 py-2.5 text-sm font-medium text-ink shadow-card disabled:cursor-not-allowed disabled:opacity-60"
          >
            Cancel
          </button>
          <button
            ref={confirmRef}
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className="inline-flex cursor-pointer items-center rounded-[10px] border-none bg-danger px-4 py-2.5 text-sm font-semibold text-white shadow-card disabled:cursor-not-allowed disabled:opacity-60"
          >
            {busy ? 'Deleting…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ConfirmDialog
