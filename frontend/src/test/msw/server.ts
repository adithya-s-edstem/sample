import { setupServer } from 'msw/node'
import { handlers } from './handlers'

/*
 * Node MSW server used by the test suite. The default `handlers` mirror the happy
 * paths of docs/api-contracts.md; individual tests override them with
 * `server.use(...)` to exercise loading/error cases.
 */
export const server = setupServer(...handlers)
