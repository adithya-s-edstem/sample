import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import App from './App.tsx'
import { MonthProvider } from './context/MonthContext.tsx'
import { FilterProvider } from './context/FilterContext.tsx'

// A single shared QueryClient for the app. Query hooks (useExpenses, useSummary,
// …) land in P5-2; this just wires the provider so they have a client.
const queryClient = new QueryClient()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <MonthProvider>
        <FilterProvider>
          <App />
        </FilterProvider>
      </MonthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
