package bisq.api.dto.account.fiat.amazon_gift_card;

import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;

public record CreateAmazonGiftCardAccountPayloadDto(
        String selectedCountryCode,
        String emailOrMobileNr
) implements CreatePaymentAccountPayloadDto {
}
