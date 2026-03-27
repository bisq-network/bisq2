package bisq.api.dto.account.crypto;

public record MoneroAccountDto(
        String accountName,
        CryptoPaymentRailDto paymentRail,
        MoneroAccountPayloadDto accountPayload
) implements CryptoAccountDto {

    public MoneroAccountDto {
        if (paymentRail != CryptoPaymentRailDto.MONERO) {
            throw new IllegalArgumentException("paymentRail must be MONERO");
        }
    }

    public MoneroAccountDto(String accountName,
                            MoneroAccountPayloadDto accountPayload) {
        this(accountName, CryptoPaymentRailDto.MONERO, accountPayload);
    }
}
