package bisq.api.dto.account.crypto.common;

import bisq.api.dto.account.PaymentAccountPayloadDto;

import java.util.Optional;

public interface CryptoAssetAccountPayloadDto extends PaymentAccountPayloadDto {
    String currencyName();

    String currencyCode();

    String address();

    boolean isInstant();

    boolean supportAutoConf();

    Optional<Boolean> isAutoConf();

    Optional<Integer> autoConfNumConfirmations();

    Optional<Long> autoConfMaxTradeAmount();

    Optional<String> autoConfExplorerUrls();
}
