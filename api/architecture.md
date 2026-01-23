# API Architecture

The API module provides a **unified, transport-agnostic interface** for interacting with the server via **REST**, **WebSocket**, or a combination of both.
It supports **clearnet** and **Tor** transports. When clearnet is selected, **TLS is mandatory**.

Authorization and session handling are optional and can be enabled as needed.
The module is designed to allow future protocols (e.g. **gRPC**) to be added with minimal impact.

---

## Purpose

The API module defines the **public application interface** and enforces cross-cutting concerns such as:

* Authentication and authorization
* Request validation and normalization
* Transport-agnostic execution of API logic

Its core goals are to:

* Provide a single, consistent API contract
* Support multiple transports without duplicating business logic
* Enable new protocols with minimal architectural changes

---

## Configuration

The API module is configured via a type-safe `api` object:

```hocon
api = {
    accessTransportType = "TOR"   # CLEAR | TOR | I2P

    server = {
        restEnabled = true
        websocketEnabled = true
        # grpcEnabled = false

        bind = {
            host = "127.0.0.1"
            port = 8090
        }

        tor = {
            onionServicePort = 80        # external virtual port
            clientAuthRequired = false
        }

        tls = {
            required = false

            keystore = {
                password = ""
            }

            certificate = {
                san = ["127.0.0.1"]
            }
        }

        security = {
            supportSessionHandling = false
            authorizationRequired = true

            session = {
                # Session validity in minutes
                # Set to -1 to disable session expiration
                ttlInMinutes = 60
            }

            # grpc = {
            #     allowServices = [
            #         "bisq.trade.TradeService"
            #     ]
            #     denyMethods = []
            # }
        }
    }
}
```

---

## API Operation Modes

The API can operate in three modes:

1. **REST only**
2. **WebSocket only**
3. **REST-over-WebSocket**

### REST-over-WebSocket

In this mode, REST requests are wrapped into WebSocket messages and internally dispatched to local REST endpoints.
This allows reuse of existing REST resources, filters, and authorization logic.

The REST-over-WebSocket mode runs inside the same **Grizzly WebSocket server**.

> ⚠️ For the current mobile application, some API methods are not yet supported in REST-over-WebSocket mode.
> Such requests are sent directly to the REST endpoint instead.

It is essential that **all request variants**—direct REST, WebSocket, and REST-over-WebSocket—are **consistently protected** by the authentication and authorization model.

---

## Transport-Agnostic Design

The module focuses on **API execution**, not transport mechanics.

Transports are treated as interchangeable carriers that deliver requests into the API layer. Once a request enters the API module, it is processed uniformly, ensuring:

* Consistent behavior across all transports
* A shared security and permission model
* No duplication of business logic

---

## Security

### Pairing Protocol

The pairing process establishes a trusted client profile and initial session:

1. Select transport type: **Clearnet** or **Tor**
2. If clearnet is used:

    * The LAN address is detected and can be applied
    * TLS is required
3. Permissions are defined and associated with a **pairing code**
4. The pairing code expires after **5 minutes**
5. A QR code is generated containing:

    * WebSocket server URL
    * Pairing code
    * Optional TLS certificate fingerprint
    * Optional Tor client authentication secret
6. The client scans the QR code or pastes the Base64-encoded data
7. The client sends a pairing request containing:

    * Pairing code
    * Client name
8. The server:

    * Creates a client profile (client ID, client secret, client name)
    * Persists the profile and granted permissions
    * Deletes the pairing code
    * Creates a session token (kept in memory, not persisted)
9. The server responds with:

    * `clientId`
    * `clientSecret`
    * `sessionId`
    * `sessionExpiryDate`
10. The client persists the relevant data and connects to the WebSocket server, sending:

    * `clientId`
    * `sessionId`
      in the request headers

#### Subsequent Client Starts

After pairing is complete, a client can request a new session by sending:

* `clientId`
* `clientSecret`

The server:

* Verifies that the client profile exists
* Validates the client secret
* Issues a new session token
* Returns the new session ID and expiry date

---

### Authentication

If `supportSessionHandling = true`, authentication filters are enabled.
They validate:

* The presence of a valid session ID
* The existence of a client profile for the given client ID

---

### Authorization

* **REST / REST-over-WebSocket**
  Enforced by `RestApiAuthorizationFilter` using permission rules.

* **WebSocket messages**
  No permission model is currently enforced.
  Authorization can be added by introducing a WebSocket-specific filter.

---

## Transport Types

* **CLEAR** – Standard LAN or public IP communication
* **TOR** – Onion routing; clients wait until the onion service is published
* **I2P** – Potential future support (currently not feasible for mobile clients)

