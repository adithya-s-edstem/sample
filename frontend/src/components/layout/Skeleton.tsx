/*
 * Loading-skeleton primitive (P5-4). A rounded shimmering block standing in for
 * content while a query is in flight, mirroring the wireframe `.sk` elements
 * (docs/wireframes/loading.html). The shimmer animation + gradient live in
 * index.css (`.animate-skeleton`); size/shape come from utility classes passed
 * via `className`, so callers compose the wireframe variants (amount, line,
 * donut, bar, row) at the call site. Decorative and hidden from assistive tech;
 * sections that show skeletons expose an `aria-busy`/loading label instead.
 */
type SkeletonProps = {
  className?: string
}

function Skeleton({ className = '' }: SkeletonProps) {
  return <div aria-hidden="true" className={`animate-skeleton rounded-lg ${className}`} />
}

export default Skeleton
