# API architecture

The API module provides a **unified, transport-agnostic interface** for interacting with the server via **REST**, **WebSocket**, or a combination of both. It is fully configurable, supports optional authentication, TLS, Tor client authentication and is designed to allow future protocols (e.g., gRPC) to be added with minimal impact.

---

## Purpose

The API module defines the **public application interface** and enforces cross-cutting concerns such as:

- Authentication and authorization
- Request validation and normalization
- Transport-agnostic execution of API logic

Its core goals are:

- Provide a single, consistent API contract
- Support multiple transports without duplicating business logic
- Allow new protocols to be added with minimal changes
- Keep transport concerns separate from API semantics

---

## Configuration

The API module is configured via a type-safe `api` object:

```hocon
api = {
    accessTransportType = "CLEARNET" // Options: CLEARNET | TOR | I2P

    server = {
        restEnabled = true
        websocketEnabled = true
        // grpcEnabled = false

        bind = {
            host = "127.0.0.1"
            port = 8090
        }

        security = {
            authRequired = true
            tlsRequired = false
            torClientAuthRequired = false

            rest = {
                denyEndpoints = []
            }

            websocket = {
                denyEndpoints = []
            }

            // grpc = {
            //     allowServices = ["bisq.trade.TradeService"]
            //     denyMethods = []
            // }
        }
    }
}
```

The API can operate in three modes:

1. **REST only** – Exposed via `JdkHttpServer`.
2. **WebSocket** – Session-level authentication bound to the WebSocket connection.
3. **REST-over-WebSocket** – REST requests are wrapped into WebSocket messages and dispatched internally to local REST endpoints, reusing existing REST resources and filters.

The **REST-over-WebSocket** mode runs inside the same Grizzly WebSocket server. This means there are effectively **two server environments for handling REST requests**:

* `JdkHttpServer` when operating in REST-only mode
* Grizzly WebSocket server with embedded REST handling in REST-over-WebSocket mode

For the current mobile application, some API methods are **not yet supported** in REST-over-WebSocket mode. Requests for these methods are sent directly to the REST endpoint.

It is important to ensure that **all request variants (direct REST, WebSocket, and REST-over-WebSocket) and server environment variants are consistently protected** by the authentication and authorization model.

---

## Transport-Agnostic Design

The module focuses on **API execution**, not transport. Transports are treated as interchangeable carriers that deliver requests into the API layer. Once a request reaches the API module, it is handled uniformly, ensuring:

* Consistent behavior across transports
* Shared security and permission model
* Easier testing and maintenance

---

## API Execution Model

1. Transport layer receives the request
2. Transport-specific filters handle connection concerns
3. Request is converted into an API-level representation
4. API-level filters are applied
5. Request is dispatched to the API implementation
6. Response is returned via the originating transport

This ensures **consistent semantics and security** regardless of transport.

---

## Filter Responsibilities

### Transport-Level Filters

* WebSocket handshake handling
* Connection metadata enrichment
* Session lifecycle management

### API-Level Filters

* Authentication context resolution
* Authorization and permission checks
* Request validation and normalization

Transport-level filters are protocol-specific, while API-level filters are **transport-agnostic**.

---

## Security

### Authentication

If `authRequired = true`, clients must complete a **pairing protocol**:

1. **Server address detection** – LAN or Tor address
2. **QR code generation** – Encodes `pairingCode`, `webSocketUrl`, and TLS/Tor context
3. **Client identity creation** – Generates `ClientIdentity` with device name and key pair
4. **Client pairing request** – Scans QR, signs `PairingRequest`, and sends to `/pair` http endpoint
5. **Server validation** – Verifies signature, pairing code, and expiration; creates `DeviceProfile`
6. **Session creation** – Issues `sessionToken` and persists session
7. **Client session usage** – Adds headers (`sessionId`, nonce, timestamp, signature) in WebSocket handshake or REST requests
8. **Client start WebSocket connection**

### Authorization

* **REST / REST-over-WebSocket**: Handled by `RestApiAuthorizationFilter` using endpoint allow/deny lists and permissions.
* **WebSocket messages**: No permission model currently enforced; can be added via new Filter added to `AccessFilterAddOn`.

---

## Transport Types

* **CLEARNET** – Standard LAN or public IP communication
* **TOR** – Onion routing; client waits until the onion address is published
* **I2P** – Potential future support, though not feasible for mobile clients

---

## Extensibility

Adding a new transport (e.g., gRPC) involves:

* Adding a protocol-specific server at the edge
* Mapping requests into the API execution model
* Reusing existing filters and authorization logic
* Keeping business logic unchanged

This allows new transports without fragmenting the API or security model.

---

## Naming and Structure Principles

* Components named after **API scope**, not transport
* Transports are pluggable and optional
* API logic is centralized
* Filters clearly declare their responsibility layer
