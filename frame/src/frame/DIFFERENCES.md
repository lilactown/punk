# Differences

- All top-level functions take `frame` as an initial argument
- No subscriptions
- 0 dependency on reagent
- No default `app-db` cofx / fx
- `inject-cofx` returns an interceptor FACTORY that takes `frame` before
  returning the interceptor
