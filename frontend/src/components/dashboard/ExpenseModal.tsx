import { useEffect, useId, useRef, useState } from 'react'
import { format } from 'date-fns'
import { CATEGORIES, type Category, type Expense, type ExpenseRequest } from '../../api/types'
import { categoryLabel } from '../../lib/category'

/*
 * Add/Edit expense modal (P7-2), matching docs/wireframes/modal.html and the
 * UX in docs/solution.md §3: a centered dialog over a dimmed overlay with the
 * three form fields (Amount ₹, Date, Category), a Cancel and a Save action, and
 * inline client-side validation.
 *
 * This component is presentational: it owns the form state and validation but
 * not the network call. On a valid Save it hands a clean `ExpenseRequest` to
 * `onSubmit`; the create/update mutation + cache invalidation are wired in P7-3,
 * and delete (P7-4) is separate. `submitting` lets the caller disable the form
 * while the request is in flight.
 *
 * Modes:
 *  - Add: `expense` omitted → empty amount/category, date defaults to today.
 *  - Edit: `expense` provided → fields pre-filled from it; Save issues an update.
 *
 * Validation (client-side, mirrors the ExpenseRequest contract in
 * docs/api-contracts.md): amount required and > 0, date required and a valid
 * `YYYY-MM-DD`, category required and one of the fixed enum values. Errors are
 * shown inline under each field; the server remains the source of truth and a
 * 400 surfaces through the mutation layer in P7-3.
 *
 * Money is exact: the amount is parsed only to validate `> 0`; the original
 * trimmed string is forwarded as `Number(...)` for the two-decimal wire value,
 * never reformatted or rounded here.
 */
type ExpenseModalProps = {
  /** When set, the modal is in edit mode and pre-fills from this expense. */
  expense?: Expense
  /** Receives a validated request body on Save. */
  onSubmit: (body: ExpenseRequest) => void
  /** Closes the modal (Cancel, overlay click, or Escape). */
  onClose: () => void
  /** Disables the form/actions while a submit is in flight. */
  submitting?: boolean
}

type FieldErrors = {
  amount?: string
  date?: string
  category?: string
}

const AMOUNT_PATTERN = /^\d+(\.\d{1,2})?$/

function today(): string {
  return format(new Date(), 'yyyy-MM-dd')
}

function validate(amount: string, date: string, category: string): FieldErrors {
  const errors: FieldErrors = {}

  const trimmedAmount = amount.trim()
  if (trimmedAmount === '') {
    errors.amount = 'Amount is required'
  } else if (!AMOUNT_PATTERN.test(trimmedAmount)) {
    errors.amount = 'Enter a valid amount (up to 2 decimals)'
  } else if (Number(trimmedAmount) <= 0) {
    errors.amount = 'Amount must be greater than 0'
  }

  if (date.trim() === '') {
    errors.date = 'Date is required'
  }

  if (category === '') {
    errors.category = 'Category is required'
  } else if (!(CATEGORIES as readonly string[]).includes(category)) {
    errors.category = 'Choose a valid category'
  }

  return errors
}

function ExpenseModal({ expense, onSubmit, onClose, submitting = false }: ExpenseModalProps) {
  const isEdit = expense !== undefined
  const [amount, setAmount] = useState(expense ? String(expense.amount) : '')
  const [date, setDate] = useState(expense ? expense.date : today())
  const [category, setCategory] = useState<Category | ''>(expense ? expense.category : '')
  const [submitted, setSubmitted] = useState(false)

  const titleId = useId()
  const amountId = useId()
  const dateId = useId()
  const categoryId = useId()
  const amountRef = useRef<HTMLInputElement>(null)

  /*
   * Validation is derived from the live field values on every render, never
   * stored — so it stays in sync without a setState-in-effect. Errors only
   * surface once the user has attempted a Save (`submitted`); until then the
   * form stays quiet so we don't flag fields the user hasn't reached yet.
   */
  const found = validate(amount, date, category)
  const errors: FieldErrors = submitted ? found : {}

  // Close on Escape from anywhere in the dialog.
  useEffect(() => {
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [onClose])

  // Focus the first field when the modal opens.
  useEffect(() => {
    amountRef.current?.focus()
  }, [])

  function handleSubmit(event: React.FormEvent) {
    event.preventDefault()
    setSubmitted(true)
    if (Object.keys(found).length > 0) return
    onSubmit({ amount: Number(amount.trim()), date, category: category as Category })
  }

  const errorClass = 'border-danger'
  const inputBase =
    'w-full rounded-[10px] border bg-white px-3.5 py-2.5 text-sm text-ink outline-none focus:border-accent'

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center bg-[rgba(17,24,39,0.45)] px-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose()
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="w-full max-w-[440px] rounded-2xl bg-card p-7 shadow-[0_20px_50px_rgba(16,24,40,0.25)]"
      >
        <h3 id={titleId} className="text-[19px] font-bold text-ink">
          {isEdit ? 'Edit Expense' : 'Add Expense'}
        </h3>
        <p className="mb-5 mt-1 text-[13px] text-muted">Record where your money went.</p>

        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-4">
            <label htmlFor={amountId} className="mb-1.5 block text-[13px] font-semibold text-ink">
              Amount (₹)
            </label>
            <input
              id={amountId}
              ref={amountRef}
              type="text"
              inputMode="decimal"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              aria-invalid={errors.amount ? true : undefined}
              aria-describedby={errors.amount ? `${amountId}-error` : undefined}
              className={`${inputBase} tabular-nums ${errors.amount ? errorClass : 'border-line'}`}
              placeholder="0.00"
            />
            {errors.amount && (
              <p id={`${amountId}-error`} className="mt-1.5 text-xs text-danger">
                {errors.amount}
              </p>
            )}
          </div>

          <div className="mb-4">
            <label htmlFor={dateId} className="mb-1.5 block text-[13px] font-semibold text-ink">
              Date
            </label>
            <input
              id={dateId}
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              aria-invalid={errors.date ? true : undefined}
              aria-describedby={errors.date ? `${dateId}-error` : undefined}
              className={`${inputBase} ${errors.date ? errorClass : 'border-line'}`}
            />
            {errors.date && (
              <p id={`${dateId}-error`} className="mt-1.5 text-xs text-danger">
                {errors.date}
              </p>
            )}
          </div>

          <div className="mb-4">
            <label htmlFor={categoryId} className="mb-1.5 block text-[13px] font-semibold text-ink">
              Category
            </label>
            <select
              id={categoryId}
              value={category}
              onChange={(event) => setCategory(event.target.value as Category | '')}
              aria-invalid={errors.category ? true : undefined}
              aria-describedby={errors.category ? `${categoryId}-error` : undefined}
              className={`${inputBase} ${errors.category ? errorClass : 'border-line'}`}
            >
              <option value="" disabled>
                Select a category
              </option>
              {CATEGORIES.map((value) => (
                <option key={value} value={value}>
                  {categoryLabel(value)}
                </option>
              ))}
            </select>
            {errors.category ? (
              <p id={`${categoryId}-error`} className="mt-1.5 text-xs text-danger">
                {errors.category}
              </p>
            ) : (
              <p className="mt-1.5 text-xs text-muted">Choose from predefined categories.</p>
            )}
          </div>

          <div className="mt-6 flex justify-end gap-2.5">
            <button
              type="button"
              onClick={onClose}
              disabled={submitting}
              className="inline-flex cursor-pointer items-center rounded-[10px] border border-line bg-card px-4 py-2.5 text-sm font-medium text-ink shadow-card disabled:cursor-not-allowed disabled:opacity-60"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex cursor-pointer items-center rounded-[10px] border-none bg-accent px-4 py-2.5 text-sm font-semibold text-white shadow-card disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting ? 'Saving…' : 'Save Expense'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}

export default ExpenseModal
