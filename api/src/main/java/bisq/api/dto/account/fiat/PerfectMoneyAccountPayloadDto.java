package bisq.api.dto.account.fiat;

public record PerfectMoneyAccountPayloadDto(
        String selectedCurrencyCode,
        String accountNr
) implements FiatAccountPayloadDto {
}
