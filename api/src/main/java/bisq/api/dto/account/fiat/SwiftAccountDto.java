package bisq.api.dto.account.fiat;


public record SwiftAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        SwiftAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public SwiftAccountDto {
        if (paymentRail != FiatPaymentRailDto.SWIFT) {
            throw new IllegalArgumentException("paymentRail must be SWIFT");
        }
    }

    public SwiftAccountDto(String accountName,
                           SwiftAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.SWIFT, accountPayload);
    }
}
