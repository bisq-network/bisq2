package bisq.api.dto.account.fiat;

public record AmazonGiftCardAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        AmazonGiftCardAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public AmazonGiftCardAccountDto {
        if (paymentRail != FiatPaymentRailDto.AMAZON_GIFT_CARD) {
            throw new IllegalArgumentException("paymentRail must be AMAZON_GIFT_CARD");
        }
    }

    public AmazonGiftCardAccountDto(String accountName,
                                    AmazonGiftCardAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.AMAZON_GIFT_CARD, accountPayload);
    }
}
