package bisq.api.rest_api.endpoints.payment_accounts;

import bisq.api.dto.account.crypto.CryptoAccountDto;

public record AddCryptoAccountRequest(CryptoAccountDto account) {
}
