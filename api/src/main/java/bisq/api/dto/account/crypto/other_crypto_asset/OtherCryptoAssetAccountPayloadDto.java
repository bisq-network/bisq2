package bisq.api.dto.account.crypto.other_crypto_asset;

import bisq.api.dto.account.crypto.common.CryptoAssetAccountPayloadDto;

import java.util.Optional;

public record OtherCryptoAssetAccountPayloadDto(
        String currencyCode,
        String currencyName,
        String address,
        boolean isInstant,
        Optional<Boolean> isAutoConf,
        Optional<Integer> autoConfNumConfirmations,
        Optional<Long> autoConfMaxTradeAmount,
        Optional<String> autoConfExplorerUrls,
        boolean supportAutoConf
) implements CryptoAssetAccountPayloadDto {
}
