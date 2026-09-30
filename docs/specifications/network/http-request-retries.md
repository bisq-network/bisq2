# HTTP request retries

HTTP provider failover must not duplicate an operation unless the caller permits retries after possible delivery.

## Rules

- A busy client detected before execution may select another provider for any request method.
- Once execution starts, GET requests may retry transient failures against another provider.
- Once execution starts, POST requests may retry only when `retryOnServerError` is true. This includes I/O failures,
  response read timeouts, framework timeouts, response parsing failures, and HTTP 5xx, 408, or 429 responses.
- Other HTTP 4xx responses are caller errors and are not retried.
- The caller enables POST retries only when duplicate execution is harmless or explicitly acceptable. Mobile push
  notifications allow duplicates because delivery is best effort.

## Rationale

An I/O failure does not establish that the server missed the request. A response can be lost after the server applied
the operation. A parsing failure occurs after a response arrived and likewise does not undo the operation.

The HTTP clients do not expose whether a failed call sent the request. A POST without retry opt-in therefore fails
without provider failover even when the underlying error happened before delivery. Retrying such failures safely
would require a client guarantee that the request was never sent.

## Verification

Tests exercise provider failover through the request service and count client calls. A POST without retry opt-in
executes once after a response read or parsing failure. GET and POST with retry opt-in can use a second provider.
A busy client can select another provider before the first execution.
