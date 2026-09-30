package bisq.api.dto.account.create;

import bisq.api.dto.account.payment_rail.PaymentRailDto;
import bisq.api.dto.account.payment_rail.PaymentRailDtoDeserializer;
import bisq.api.dto.account.crypto.common.CryptoPaymentRailDto;
import bisq.api.dto.account.crypto.monero.CreateMoneroAccountPayloadDto;
import bisq.api.dto.account.crypto.other_crypto_asset.CreateOtherCryptoAssetAccountPayloadDto;
import bisq.api.dto.account.fiat.common.FiatPaymentRailDto;
import bisq.api.dto.account.fiat.ach.CreateAchTransferAccountPayloadDto;
import bisq.api.dto.account.fiat.alipay.CreateAliPayAccountPayloadDto;
import bisq.api.dto.account.fiat.amazon_gift_card.CreateAmazonGiftCardAccountPayloadDto;
import bisq.api.dto.account.fiat.cash_deposit.CreateCashDepositAccountPayloadDto;
import bisq.api.dto.account.fiat.money_gram.CreateMoneyGramAccountPayloadDto;
import bisq.api.dto.account.fiat.revolut.CreateRevolutAccountPayloadDto;
import bisq.api.dto.account.fiat.sepa.CreateSepaAccountPayloadDto;
import bisq.api.dto.account.fiat.sepa_instant.CreateSepaInstantAccountPayloadDto;
import bisq.api.dto.account.fiat.user_defined.CreateUserDefinedFiatAccountPayloadDto;
import bisq.api.dto.account.fiat.wise.CreateWiseAccountPayloadDto;
import bisq.api.dto.account.fiat.zelle.CreateZelleAccountPayloadDto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

public class CreatePaymentAccountDtoDeserializer extends JsonDeserializer<CreatePaymentAccountDto> {
    @Override
    public CreatePaymentAccountDto deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        JsonNode paymentRailNode = node.get("paymentRail");
        if (paymentRailNode == null || paymentRailNode.asText().isBlank()) {
            throw context.weirdStringException(null, PaymentRailDto.class, "Payment rail must not be empty");
        }

        String accountName = node.hasNonNull("accountName") ? node.get("accountName").asText() : null;
        PaymentRailDto paymentRail = PaymentRailDtoDeserializer.fromString(paymentRailNode.asText(), context);
        JsonNode accountPayloadNode = node.get("accountPayload");
        if (accountPayloadNode == null || accountPayloadNode.isNull()) {
            throw context.weirdStringException(null, CreatePaymentAccountPayloadDto.class, "Account payload cannot be null");
        }

        CreatePaymentAccountPayloadDto accountPayload = parser.getCodec().treeToValue(
                accountPayloadNode,
                getPayloadClass(paymentRail)
        );
        return new CreatePaymentAccountDto(accountName, paymentRail, accountPayload);
    }

    private static Class<? extends CreatePaymentAccountPayloadDto> getPayloadClass(PaymentRailDto paymentRail) {
        if (paymentRail instanceof FiatPaymentRailDto fiatPaymentRail) {
            return switch (fiatPaymentRail) {
                case CUSTOM -> CreateUserDefinedFiatAccountPayloadDto.class;
                case ACH_TRANSFER -> CreateAchTransferAccountPayloadDto.class;
                case ALI_PAY -> CreateAliPayAccountPayloadDto.class;
                case AMAZON_GIFT_CARD -> CreateAmazonGiftCardAccountPayloadDto.class;
                case CASH_DEPOSIT -> CreateCashDepositAccountPayloadDto.class;
                case MONEY_GRAM -> CreateMoneyGramAccountPayloadDto.class;
                case REVOLUT -> CreateRevolutAccountPayloadDto.class;
                case SEPA -> CreateSepaAccountPayloadDto.class;
                case SEPA_INSTANT -> CreateSepaInstantAccountPayloadDto.class;
                case WISE -> CreateWiseAccountPayloadDto.class;
                case ZELLE -> CreateZelleAccountPayloadDto.class;
                default -> throw new IllegalArgumentException("Unsupported fiat payment rail: " + paymentRail);
            };
        }
        if (paymentRail instanceof CryptoPaymentRailDto cryptoPaymentRail) {
            return switch (cryptoPaymentRail) {
                case MONERO -> CreateMoneroAccountPayloadDto.class;
                case OTHER_CRYPTO_ASSET -> CreateOtherCryptoAssetAccountPayloadDto.class;
            };
        }
        throw new IllegalArgumentException("Unsupported payment rail: " + paymentRail);
    }
}
