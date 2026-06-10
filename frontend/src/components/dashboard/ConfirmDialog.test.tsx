import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import ConfirmDialog from './ConfirmDialog'

/*
 * ConfirmDialog presentational tests (P7-4): the small confirm prompt shown
 * before a destructive action (docs/solution.md §4). It renders the supplied
 * copy as an accessible alertdialog, fires onConfirm/onClose, dismisses on
 * overlay click and Escape, disables its actions while `busy`, and surfaces an
 * inline `error` — all without owning any network state.
 */
describe('ConfirmDialog (P7-4)', () => {
  function setup(overrides: Partial<React.ComponentProps<typeof ConfirmDialog>> = {}) {
    const onConfirm = vi.fn()
    const onClose = vi.fn()
    render(
      <ConfirmDialog
        title="Delete expense?"
        message="This can't be undone."
        onConfirm={onConfirm}
        onClose={onClose}
        {...overrides}
      />,
    )
    return { onConfirm, onClose }
  }

  it('renders the title and message in an accessible alertdialog', () => {
    setup()
    const dialog = screen.getByRole('alertdialog')
    expect(dialog).toBeInTheDocument()
    expect(screen.getByText('Delete expense?')).toBeInTheDocument()
    expect(screen.getByText("This can't be undone.")).toBeInTheDocument()
  })

  it('fires onConfirm when the confirm action is clicked', () => {
    const { onConfirm } = setup()
    fireEvent.click(screen.getByRole('button', { name: 'Delete' }))
    expect(onConfirm).toHaveBeenCalledTimes(1)
  })

  it('fires onClose from Cancel, Escape, and overlay click', () => {
    const { onClose } = setup()
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    fireEvent.keyDown(document, { key: 'Escape' })
    fireEvent.mouseDown(screen.getByRole('alertdialog').parentElement as HTMLElement)
    expect(onClose).toHaveBeenCalledTimes(3)
  })

  it('uses a custom confirm label when provided', () => {
    setup({ confirmLabel: 'Remove' })
    expect(screen.getByRole('button', { name: 'Remove' })).toBeInTheDocument()
  })

  it('disables the actions and ignores dismiss gestures while busy', () => {
    const { onConfirm, onClose } = setup({ busy: true })
    expect(screen.getByRole('button', { name: 'Deleting…' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Cancel' })).toBeDisabled()
    fireEvent.keyDown(document, { key: 'Escape' })
    fireEvent.mouseDown(screen.getByRole('alertdialog').parentElement as HTMLElement)
    expect(onConfirm).not.toHaveBeenCalled()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('surfaces an inline error message', () => {
    setup({ error: 'Could not delete expense' })
    expect(screen.getByRole('alert')).toHaveTextContent('Could not delete expense')
  })
})
