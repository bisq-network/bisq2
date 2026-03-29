package bisq.api.dto.mappings.account;

import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.accounts.fiat.UserDefinedFiatAccount;
import bisq.account.accounts.fiat.ZelleAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.crypto.MoneroAccountDto;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountDto;
import bisq.api.dto.account.fiat.UserDefinedFiatAccountDto;
import bisq.api.dto.account.fiat.ZelleAccountDto;
import bisq.api.dto.mappings.account.crypto.MoneroAccountDtoMapping;
import bisq.api.dto.mappings.account.crypto.OtherCryptoAssetAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.UserDefinedFiatAccountDtoMapping;
import bisq.api.dto.mappings.account.fiat.ZelleAccountDtoMapping;

import static bisq.api.dto.account.crypto.CryptoPaymentRailDto.MONERO;
import static bisq.api.dto.account.crypto.CryptoPaymentRailDto.OTHER_CRYPTO_ASSET;
import static bisq.api.dto.account.fiat.FiatPaymentRailDto.CUSTOM;
import static bisq.api.dto.account.fiat.FiatPaymentRailDto.ZELLE;

public class PaymentAccountDtoMapping {

    public static Account<? extends PaymentMethod<?>, ?> toBisq2Model(PaymentAccountDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("PaymentAccountDto cannot be null");
        }

        return switch (dto.paymentRail()) {
            // Fiat
            case CUSTOM -> {
                if (dto instanceof UserDefinedFiatAccountDto userDefinedDto) {
                    yield UserDefinedFiatAccountDtoMapping.toBisq2Model(userDefinedDto);
                }
                throw new IllegalArgumentException("Expected UserDefinedFiatAccountDto for CUSTOM payment rail");
            }
            case ZELLE -> {
                if (dto instanceof ZelleAccountDto zelleDto) {
                    yield ZelleAccountDtoMapping.toBisq2Model(zelleDto);
                }
                throw new IllegalArgumentException("Expected ZelleAccountDto for ZELLE payment rail");
            }
            // Crypto
            case MONERO -> {
                if (dto instanceof MoneroAccountDto moneroDto) {
                    yield MoneroAccountDtoMapping.toBisq2Model(moneroDto);
                }
                throw new IllegalArgumentException("Expected MoneroAccountDto for MONERO payment rail");
            }
            case OTHER_CRYPTO_ASSET -> {
                if (dto instanceof OtherCryptoAssetAccountDto otherCryptoDto) {
                    yield OtherCryptoAssetAccountDtoMapping.toBisq2Model(otherCryptoDto);
                }
                throw new IllegalArgumentException("Expected OtherCryptoAssetAccountDto for OTHER_CRYPTO_ASSET payment rail");
            }
            // TODO: Add cases for additional payment rails when implemented:
            // case SEPA -> { ... }
            // case REVOLUT -> { ... }
            default -> throw new IllegalArgumentException("Unsupported payment rail: " + dto.paymentRail());
        };
    }

    /**
     * Convert a Bisq2 Account model to a PaymentAccountDto.
     * Currently supports:
     * - Fiat: UserDefinedFiatAccount, ZelleAccount
     * - Crypto: MoneroAccount, OtherCryptoAssetAccount
     * TODO: Add support for additional account types as they are implemented.
     */
    public static PaymentAccountDto fromBisq2Model(Account<? extends PaymentMethod<?>, ?> account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }

        if (account instanceof UserDefinedFiatAccount userDefinedAccount) {
            return UserDefinedFiatAccountDtoMapping.fromBisq2Model(userDefinedAccount);
        }
        if (account instanceof ZelleAccount zelleAccount) {
            return ZelleAccountDtoMapping.fromBisq2Model(zelleAccount);
        }
        if (account instanceof MoneroAccount moneroAccount) {
            return MoneroAccountDtoMapping.fromBisq2Model(moneroAccount);
        }
        if (account instanceof OtherCryptoAssetAccount otherCryptoAssetAccount) {
            return OtherCryptoAssetAccountDtoMapping.fromBisq2Model(otherCryptoAssetAccount);
        }

        // TODO: Add conversions for other account types when implemented:
        // if (account instanceof SepaAccount sepaAccount) { ... }
        // if (account instanceof RevolutAccount revolutAccount) { ... }

        throw new IllegalArgumentException("Unsupported account type: " + account.getClass().getSimpleName());
    }
}
