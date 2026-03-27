package bisq.api.dto.account.fiat;


public record PromptPayAccountDto(
        String accountName,
        FiatPaymentRailDto paymentRail,
        PromptPayAccountPayloadDto accountPayload
) implements FiatAccountDto {

    public PromptPayAccountDto {
        if (paymentRail != FiatPaymentRailDto.PROMPT_PAY) {
            throw new IllegalArgumentException("paymentRail must be PROMPT_PAY");
        }
    }

    public PromptPayAccountDto(String accountName,
                               PromptPayAccountPayloadDto accountPayload) {
        this(accountName, FiatPaymentRailDto.PROMPT_PAY, accountPayload);
    }
}
