package bisq.api.dto.account.crypto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "paymentRail",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = MoneroAccountDto.class, name = "MONERO"),
        @JsonSubTypes.Type(value = OtherCryptoAssetAccountDto.class, name = "OTHER_CRYPTO_ASSET")
})
public interface CryptoAccountDto {
    String accountName();

    CryptoPaymentRailDto paymentRail();

    CryptoAccountPayloadDto accountPayload();
}
