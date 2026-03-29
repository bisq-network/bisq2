package bisq.api.dto.account.crypto;

import java.util.Optional;

public record OtherCryptoAssetAccountPayloadDto(
        String currencyCode,
        String address,
        boolean isInstant,
        Optional<Boolean> isAutoConf,
        Optional<Integer> autoConfNumConfirmations,
        Optional<Long> autoConfMaxTradeAmount,
        Optional<String> autoConfExplorerUrls
) implements CryptoAssetAccountPayloadDto {
}
