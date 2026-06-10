import { useEffect, useRef } from 'react'

/*
 * Accessibility (P9-1): a focus trap for modal dialogs. While a dialog is open,
 * keyboard focus must stay within it — Tab from the last focusable element wraps
 * to the first and Shift+Tab from the first wraps to the last — and when the
 * dialog closes, focus returns to whatever was focused before it opened (the
 * trigger). This is the standard WAI-ARIA dialog behavior and complements the
 * existing role="dialog"/aria-modal markup and Escape-to-close.
 *
 * Returns a ref to attach to the dialog container. The component still owns its
 * initial focus (it focuses a specific field/button on open); this hook only
 * keeps focus from escaping and restores it afterward.
 */
const FOCUSABLE_SELECTOR = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

export function useFocusTrap<T extends HTMLElement>() {
  const containerRef = useRef<T>(null)

  useEffect(() => {
    const container = containerRef.current
    // Remember the element that had focus before the dialog opened so we can
    // restore it on close (e.g. the row's edit/delete button, the Add button).
    const previouslyFocused = document.activeElement as HTMLElement | null

    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== 'Tab' || !container) return

      // All enabled, non-hidden focusables in DOM order. We avoid filtering on
      // `offsetParent`/layout (jsdom doesn't compute it) — the dialog only holds
      // visible controls, and `:not([disabled])`/`tabindex` already excludes
      // inert ones.
      const focusable = Array.from(
        container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR),
      ).filter((el) => el.getAttribute('aria-hidden') !== 'true')
      if (focusable.length === 0) return

      const first = focusable[0]
      const last = focusable[focusable.length - 1]
      const active = document.activeElement

      if (event.shiftKey) {
        if (active === first || !container.contains(active)) {
          event.preventDefault()
          last.focus()
        }
      } else if (active === last || !container.contains(active)) {
        event.preventDefault()
        first.focus()
      }
    }

    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('keydown', onKeyDown)
      // Restore focus to the trigger when the dialog unmounts.
      previouslyFocused?.focus?.()
    }
  }, [])

  return containerRef
}
