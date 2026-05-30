package bisq.api.dto.mappings.account.create;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.api.dto.account.create.CreatePaymentAccountDto;
import bisq.api.dto.account.create.CreatePaymentAccountPayloadDto;
import bisq.api.dto.account.crypto.monero.CreateMoneroAccountPayloadDto;
import bisq.api.dto.account.crypto.other_crypto_asset.CreateOtherCryptoAssetAccountPayloadDto;
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
import bisq.api.dto.mappings.account.crypto.MoneroAccountDtoMapping;
import bisq.api.dto.mappings.account.crypto.OtherCryptoAssetAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.AchTransferAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.AliPayAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.AmazonGiftCardAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.CashDepositAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.MoneyGramAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.RevolutAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.SepaAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.SepaInstantAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.UserDefinedFiatAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.WiseAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.ZelleAccountDtoMapping;

import static bisq.api.dto.account.crypto.common.CryptoPaymentRailDto.MONERO;
import static bisq.api.dto.account.crypto.common.CryptoPaymentRailDto.OTHER_CRYPTO_ASSET;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.ACH_TRANSFER;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.ALI_PAY;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.AMAZON_GIFT_CARD;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.CASH_DEPOSIT;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.CUSTOM;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.MONEY_GRAM;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.REVOLUT;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.SEPA;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.SEPA_INSTANT;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.WISE;
import static bisq.api.dto.account.fiat.common.FiatPaymentRailDto.ZELLE;

public class CreatePaymentAccountDtoMapping {
    public static Account<? extends PaymentMethod<?>, ?> toBisq2Model(CreatePaymentAccountDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("CreatePaymentAccountDto cannot be null");
        }
        if (dto.paymentRail() == null) {
            throw new IllegalArgumentException("Payment rail cannot be null");
        }
        if (dto.accountPayload() == null) {
            throw new IllegalArgumentException("Account payload cannot be null");
        }

        String accountName = dto.accountName();
        CreatePaymentAccountPayloadDto payload = dto.accountPayload();
        return switch (dto.paymentRail()) {
            // Fiat
            case CUSTOM -> {
                if (payload instanceof CreateUserDefinedFiatAccountPayloadDto userDefinedPayload) {
                    yield UserDefinedFiatAccountDtoMapping.toBisq2Model(accountName, userDefinedPayload);
                }
                throw expectedPayloadException("CreateUserDefinedFiatAccountPayloadDto", dto);
            }
            case ACH_TRANSFER -> {
                if (payload instanceof CreateAchTransferAccountPayloadDto achTransferPayload) {
                    yield AchTransferAccountDtoMapping.toBisq2Model(accountName, achTransferPayload);
                }
                throw expectedPayloadException("CreateAchTransferAccountPayloadDto", dto);
            }
            case ALI_PAY -> {
                if (payload instanceof CreateAliPayAccountPayloadDto aliPayPayload) {
                    yield AliPayAccountDtoMapping.toBisq2Model(accountName, aliPayPayload);
                }
                throw expectedPayloadException("CreateAliPayAccountPayloadDto", dto);
            }
            case AMAZON_GIFT_CARD -> {
                if (payload instanceof CreateAmazonGiftCardAccountPayloadDto amazonGiftCardPayload) {
                    yield AmazonGiftCardAccountDtoMapping.toBisq2Model(accountName, amazonGiftCardPayload);
                }
                throw expectedPayloadException("CreateAmazonGiftCardAccountPayloadDto", dto);
            }
            case CASH_DEPOSIT -> {
                if (payload instanceof CreateCashDepositAccountPayloadDto cashDepositPayload) {
                    yield CashDepositAccountDtoMapping.toBisq2Model(accountName, cashDepositPayload);
                }
                throw expectedPayloadException("CreateCashDepositAccountPayloadDto", dto);
            }
            case MONEY_GRAM -> {
                if (payload instanceof CreateMoneyGramAccountPayloadDto moneyGramPayload) {
                    yield MoneyGramAccountDtoMapping.toBisq2Model(accountName, moneyGramPayload);
                }
                throw expectedPayloadException("CreateMoneyGramAccountPayloadDto", dto);
            }
            case REVOLUT -> {
                if (payload instanceof CreateRevolutAccountPayloadDto revolutPayload) {
                    yield RevolutAccountDtoMapping.toBisq2Model(accountName, revolutPayload);
                }
                throw expectedPayloadException("CreateRevolutAccountPayloadDto", dto);
            }
            case SEPA -> {
                if (payload instanceof CreateSepaAccountPayloadDto sepaPayload) {
                    yield SepaAccountDtoMapping.toBisq2Model(accountName, sepaPayload);
                }
                throw expectedPayloadException("CreateSepaAccountPayloadDto", dto);
            }
            case SEPA_INSTANT -> {
                if (payload instanceof CreateSepaInstantAccountPayloadDto sepaInstantPayload) {
                    yield SepaInstantAccountDtoMapping.toBisq2Model(accountName, sepaInstantPayload);
                }
                throw expectedPayloadException("CreateSepaInstantAccountPayloadDto", dto);
            }
            case WISE -> {
                if (payload instanceof CreateWiseAccountPayloadDto wisePayload) {
                    yield WiseAccountDtoMapping.toBisq2Model(accountName, wisePayload);
                }
                throw expectedPayloadException("CreateWiseAccountPayloadDto", dto);
            }
            case ZELLE -> {
                if (payload instanceof CreateZelleAccountPayloadDto zellePayload) {
                    yield ZelleAccountDtoMapping.toBisq2Model(accountName, zellePayload);
                }
                throw expectedPayloadException("CreateZelleAccountPayloadDto", dto);
            }
            // Crypto
            case MONERO -> {
                if (payload instanceof CreateMoneroAccountPayloadDto moneroPayload) {
                    yield MoneroAccountDtoMapping.toBisq2Model(accountName, moneroPayload);
                }
                throw expectedPayloadException("CreateMoneroAccountPayloadDto", dto);
            }
            case OTHER_CRYPTO_ASSET -> {
                if (payload instanceof CreateOtherCryptoAssetAccountPayloadDto otherCryptoPayload) {
                    yield OtherCryptoAssetAccountDtoMapping.toBisq2Model(accountName, otherCryptoPayload);
                }
                throw expectedPayloadException("CreateOtherCryptoAssetAccountPayloadDto", dto);
            }
            default -> throw new IllegalArgumentException("Unsupported payment rail: " + dto.paymentRail());
        };
    }

    private static IllegalArgumentException expectedPayloadException(String expectedPayloadType,
                                                                     CreatePaymentAccountDto dto) {
        return new IllegalArgumentException("Expected " + expectedPayloadType + " for " + dto.paymentRail() + " payment rail");
    }
}
