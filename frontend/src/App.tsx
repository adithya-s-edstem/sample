// Placeholder shell — the real layout, theme, and routing land in P5-1.
// Kept intentionally minimal so `npm run dev` / `npm run build` succeed and
// Tailwind is verified to be wired up.
function App() {
  return (
    <main className="min-h-screen bg-slate-50 text-slate-900">
      <div className="mx-auto max-w-3xl px-6 py-16">
        <h1 className="text-3xl font-semibold tracking-tight">Expense Tracker</h1>
        <p className="mt-2 text-slate-600">
          Frontend scaffold ready (Vite + React + TS + Tailwind). UI is built out
          from Phase 5.
        </p>
      </div>
    </main>
  )
}

export default App
