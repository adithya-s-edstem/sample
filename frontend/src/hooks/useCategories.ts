import { useQuery } from '@tanstack/react-query'
import { listCategories } from '../api/expenses'
import { queryKeys } from '../api/queryKeys'

/**
 * The fixed category set for populating dropdowns (`GET /api/categories`). The
 * list never changes within a session, so it is held effectively forever.
 */
export function useCategories() {
  return useQuery({
    queryKey: queryKeys.categories,
    queryFn: listCategories,
    staleTime: Infinity,
  })
}
