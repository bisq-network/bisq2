package bisq.api.rest_api.endpoints.payment_accounts;

import bisq.api.dto.account.crypto.CryptoAccountDto;
import bisq.api.dto.account.fiat.FiatAccountDto;

import java.util.List;

public record PaymentAccountsDto(
        List<FiatAccountDto> fiat,
        List<CryptoAccountDto> crypto
) {
}
