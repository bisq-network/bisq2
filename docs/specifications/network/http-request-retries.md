# HTTP request retries

HTTP provider failover must not duplicate an operation unless the caller permits retries after possible delivery.

## Rules

- A busy client detected before execution may select another provider for any request method.
- Once execution starts, a failed GET request tries another provider. This includes I/O failures, timeouts, response
  parsing failures, and every HTTP error response, also a 4xx.
- A provider that failed a request is not selected for later requests until the other providers have failed as well.
- Once execution starts, POST requests may retry only when `retryOnServerError` is true. This includes I/O failures,
  response read timeouts, framework timeouts, response parsing failures, and HTTP 5xx, 408, or 429 responses.
- Other HTTP 4xx responses to a POST are caller errors and are not retried. A service may classify a 4xx of its
  endpoint as transient when the response body states so. The mobile notification relay does this for a 400 that
  carries a transient gateway error code. Such a response is then handled like a 5xx, so the rule for POST retries
  above still applies.
- The caller enables POST retries only when duplicate execution is harmless or explicitly acceptable. Mobile push
  notifications allow duplicates because delivery is best effort.

## Rationale

An I/O failure does not establish that the server missed the request. A response can be lost after the server applied
the operation. A parsing failure occurs after a response arrived and likewise does not undo the operation.

The HTTP clients do not expose whether a failed call sent the request. A POST without retry opt-in therefore fails
without provider failover even when the underlying error happened before delivery. Retrying such failures safely
would require a client guarantee that the request was never sent.

A GET applies nothing on the server, so another provider cannot duplicate an operation. The providers of a GET
service are independent, and their answers can differ. An explorer that has not seen a transaction yet answers 404,
while another explorer returns the transaction. A provider behind a misconfigured proxy can answer 403 or 404 for a
path that the other providers serve. A 4xx to a GET therefore describes the provider, not only the request.

The fallback providers of a service are used only after the configured providers have failed. A node that uses only
Tor often has a single configured onion provider for a service. If a 4xx to a GET ended the request without marking
the provider as failed, that node would never reach a fallback provider. It would send every later request, for
example every periodic market price request, to the same provider again.

## Verification

Tests exercise provider failover through the request service and count client calls. A POST without retry opt-in
executes once after a response read or parsing failure. GET and POST with retry opt-in can use a second provider.
A busy client can select another provider before the first execution. A GET whose provider answers 404 uses a second
provider, and a later GET does not return to the provider that answered 404. A POST that receives a 400 executes
once, also with retry opt-in. A transient 400 of the mobile notification relay uses a second provider only with retry
opt-in.
