package bisq.api.dto.account.crypto.create;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

import java.util.Optional;

public record CreateOtherCryptoAssetAccountPayloadDto(
        String currencyCode,
        String address,
        boolean isInstant,
        Optional<Boolean> isAutoConf,
        Optional<Integer> autoConfNumConfirmations,
        Optional<Long> autoConfMaxTradeAmount,
        Optional<String> autoConfExplorerUrls
) implements CreatePaymentAccountPayloadDto {
}
