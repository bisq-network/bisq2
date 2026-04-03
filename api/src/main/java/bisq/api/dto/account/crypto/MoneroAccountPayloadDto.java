package bisq.api.dto.account.crypto;

import java.util.Optional;

public record MoneroAccountPayloadDto(
        String address,
        boolean isInstant,
        Optional<Boolean> isAutoConf,
        Optional<Integer> autoConfNumConfirmations,
        Optional<Long> autoConfMaxTradeAmount,
        Optional<String> autoConfExplorerUrls,
        boolean useSubAddresses,
        Optional<String> mainAddress,
        Optional<String> privateViewKey,
        Optional<String> subAddress,
        Optional<Integer> accountIndex,
        Optional<Integer> initialSubAddressIndex,
        String currencyName
) implements CryptoAssetAccountPayloadDto {
}
