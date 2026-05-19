package bisq.api.dto.account.crypto.create;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

import java.util.Optional;

public record CreateMoneroAccountPayloadDto(
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
        Optional<Integer> initialSubAddressIndex
) implements CreatePaymentAccountPayloadDto {
}
