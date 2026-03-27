package bisq.api.dto.account.fiat;


public record RevolutAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        RevolutAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public RevolutAccountDto {
        if (paymentRail != FiatPaymentRailDto.REVOLUT) {
            throw new IllegalArgumentException("paymentRail must be REVOLUT");
        }
    }

    public RevolutAccountDto(String accountName,
                             RevolutAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.REVOLUT, accountPayload);
    }
}
