# Bisq API

REST API for testing and interacting with Bisq2.

## API Documentation

Access the interactive Swagger UI documentation at:

**Swagger UI:** `http://localhost:8090/doc/v1/index.html`

The Swagger UI provides:
- Complete API documentation
- Interactive endpoint testing
- Request/Response schemas
- Example values

## Quick Start

1. **Start the Bisq2 application** with API enabled
2. **Open Swagger UI** in your browser: `http://localhost:8090/doc/v1/index.html`
3. **Explore and test** the available endpoints

## Configuration

- **Default Port:** `8090`
- **Base URL:** `http://localhost:8090/api/v1`
- **Server Host:** `localhost` (configured in `api_app.conf` under `websocket.server.host`)

**Note:** The server is configured to use `localhost` instead of `0.0.0.0` to ensure Swagger UI can make API requests
from the browser without CORS issues.

## Available API Categories

- **User Profile API** - Manage user profiles, report/ignore users
- **Market Price API** - Get market price quotes
- **Settings API** - User settings management
- **Payment Accounts API** - Payment account management
- **Explorer API** - Blockchain explorer data

## Example Endpoints

```
GET    /user-profiles?ids={ids}           - Get user profiles by IDs
POST   /user-profiles/report/{profileId}  - Report a user profile
GET    /market-price/quotes                - Get market price quotes
GET    /settings                           - Get user settings
GET    /payment-accounts                   - Get payment accounts
GET    /explorer/selected                  - Get selected explorer provider
GET    /explorer/tx/{txId}                 - Get transaction details
```

## Support

For more information:
- Check the Swagger documentation for detailed endpoint information
- Refer to the main Bisq2 project documentation
- Review the source code in the `api` module

