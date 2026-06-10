import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { useState } from 'react'
import ConfirmDialog from './ConfirmDialog'

/*
 * Accessibility focus-management tests for the modal dialog (P9-1). Per the
 * WAI-ARIA dialog pattern: focus is trapped inside the dialog (Tab/Shift+Tab wrap
 * within it) and returns to the trigger when the dialog closes. ConfirmDialog is
 * used as the harness since it shares the focus-trap hook with ExpenseModal and
 * has a small, predictable set of focusable controls (Cancel + Confirm).
 */

function Harness() {
  const [open, setOpen] = useState(false)
  return (
    <div>
      <button type="button" onClick={() => setOpen(true)}>
        Open
      </button>
      {open && (
        <ConfirmDialog
          title="Delete expense?"
          message="This cannot be undone."
          onConfirm={vi.fn()}
          onClose={() => setOpen(false)}
        />
      )}
    </div>
  )
}

describe('modal focus management (P9-1)', () => {
  it('focuses the confirm action on open and traps Tab within the dialog', () => {
    render(<Harness />)
    fireEvent.click(screen.getByRole('button', { name: 'Open' }))

    const confirm = screen.getByRole('button', { name: 'Delete' })
    const cancel = screen.getByRole('button', { name: 'Cancel' })
    // The dialog auto-focuses the confirm (destructive) action on open.
    expect(confirm).toHaveFocus()

    // Tab from the last focusable wraps back to the first within the dialog.
    confirm.focus()
    fireEvent.keyDown(document.activeElement!, { key: 'Tab' })
    expect(cancel).toHaveFocus()

    // Shift+Tab from the first wraps to the last.
    cancel.focus()
    fireEvent.keyDown(document.activeElement!, { key: 'Tab', shiftKey: true })
    expect(confirm).toHaveFocus()
  })

  it('restores focus to the trigger when the dialog closes', () => {
    render(<Harness />)
    const trigger = screen.getByRole('button', { name: 'Open' })
    trigger.focus()
    fireEvent.click(trigger)

    // Close via Escape; focus should return to the element that opened it.
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(trigger).toHaveFocus()
  })
})
