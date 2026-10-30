# HTTP response bodies

The node reads the body of an HTTP response only up to a limit which the request service sets for its endpoint. A
provider controls the size of its responses, and a faulty or malicious provider must not fill the memory of the node
with an unbounded body.

## Rules

- Each HTTP request service sets the largest response body which it reads. The limit lies well above the largest
  legitimate response of its endpoint:

  | Service | Endpoint | Largest legitimate response | Limit |
  | --- | --- | --- | --- |
  | Market prices | `getAllMarketPrices` of a price node | about 12 KB for 77 markets | 1 MiB |
  | Reference time | `getAllMarketPrices` of a price node | as above | 1 MiB |
  | Explorer | one transaction, `api/tx/<txid>` | below about 1.7 MB for a standard transaction | 4 MiB |
  | Mobile notification relay | the answer to a push notification | a JSON object of a few fields | 64 KiB |

- The limit counts the bytes of the body as the client reads them, after the transfer and content decoding, so a
  compressed body cannot grow past it.
- A body above the limit fails the request at the limit. The client does not read the rest of the body and closes the
  connection, also where the HTTP library would read the rest to reuse the connection. The failure counts as an I/O
  failure for retries and provider failover, see [HTTP request retries](http-request-retries.md).
- An error response whose body is above the limit, or cannot be read, keeps its status code in both clients: the
  request fails with that status and an empty body, so that the status code still decides about retries.

## Rationale

The limit of the explorer: the JSON of a transaction has at most about 4 bytes per weight unit, because the explorer
writes each witness byte as hex in the witness and again in the assembly of the script. A standard transaction has at
most 400,000 weight units. A larger transaction is not standard. A lookup of such a transaction fails, and the user can
still check it in a block explorer.

## Verification

Tests read a body of exactly the limit and fail one byte above it, in the Tor client and in the clearnet client with a
local HTTP server. An endless body fails after at most one buffer above the limit. An error response above the limit
keeps its status code in both clients. Through a fake SOCKS proxy, a body above the limit fails the request and ends
the connection before the proxy can send the rest of a body of 256 MiB. The request service passes its limit to the
client which it creates.
