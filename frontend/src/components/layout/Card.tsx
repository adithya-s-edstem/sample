import type { ReactNode } from 'react'

/*
 * Reusable card surface used across the dashboard (summary, trend, list).
 * Matches the wireframe .card: white surface, hairline border, soft shadow,
 * 14px radius. `title` renders the uppercase muted .card-title when provided.
 */
type CardProps = {
  title?: string
  className?: string
  children: ReactNode
}

function Card({ title, className = '', children }: CardProps) {
  return (
    <section
      className={`rounded-card border border-line bg-card px-6 py-[22px] shadow-card ${className}`}
    >
      {title ? (
        <p className="mb-3.5 text-[13px] font-semibold uppercase tracking-[0.04em] text-muted">
          {title}
        </p>
      ) : null}
      {children}
    </section>
  )
}

export default Card
