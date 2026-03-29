package bisq.api.dto.account.crypto;

import bisq.api.dto.account.PaymentAccountDto;

public record MoneroAccountDto(
        String accountName,
        CryptoPaymentRailDto paymentRail,
        MoneroAccountPayloadDto accountPayload,
        Long creationDate
) implements PaymentAccountDto {

    public MoneroAccountDto {
        if (paymentRail != CryptoPaymentRailDto.MONERO) {
            throw new IllegalArgumentException("paymentRail must be MONERO");
        }
    }

    public MoneroAccountDto(String accountName,
                            MoneroAccountPayloadDto accountPayload,
                            Long creationDate) {
        this(accountName, CryptoPaymentRailDto.MONERO, accountPayload, creationDate);
    }
}
