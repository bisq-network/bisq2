package bisq.api.dto.account.crypto;

import bisq.api.dto.account.PaymentAccountPayloadDto;

import java.util.Optional;

public interface CryptoAssetAccountPayloadDto extends PaymentAccountPayloadDto {
    String currencyCode();

    String address();

    boolean isInstant();

    Optional<Boolean> isAutoConf();

    Optional<Integer> autoConfNumConfirmations();

    Optional<Long> autoConfMaxTradeAmount();

    Optional<String> autoConfExplorerUrls();
}
