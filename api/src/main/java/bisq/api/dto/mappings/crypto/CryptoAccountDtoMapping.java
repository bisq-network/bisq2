package bisq.api.dto.mappings.crypto;

import bisq.account.accounts.Account;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.OtherCryptoAssetAccount;
import bisq.account.payment_method.PaymentMethod;
import bisq.api.dto.account.crypto.CryptoAccountDto;
import bisq.api.dto.account.crypto.MoneroAccountDto;
import bisq.api.dto.account.crypto.OtherCryptoAssetAccountDto;

public class CryptoAccountDtoMapping {
    public static Account<? extends PaymentMethod<?>, ?> toBisq2Model(CryptoAccountDto dto) {
        if (dto instanceof MoneroAccountDto moneroAccountDto) {
            return MoneroAccountDtoMapping.toBisq2Model(moneroAccountDto);
        }
        if (dto instanceof OtherCryptoAssetAccountDto otherCryptoAssetAccountDto) {
            return OtherCryptoAssetAccountDtoMapping.toBisq2Model(otherCryptoAssetAccountDto);
        }
        throw new IllegalArgumentException("Unsupported crypto account type: " + dto.getClass().getSimpleName());
    }

    public static CryptoAccountDto fromBisq2Model(Account<? extends PaymentMethod<?>, ?> account) {
        if (account instanceof MoneroAccount moneroAccount) {
            return MoneroAccountDtoMapping.fromBisq2Model(moneroAccount);
        }
        if (account instanceof OtherCryptoAssetAccount otherCryptoAssetAccount) {
            return OtherCryptoAssetAccountDtoMapping.fromBisq2Model(otherCryptoAssetAccount);
        }

        throw new IllegalArgumentException("Unsupported crypto account type: " + account.getClass().getSimpleName());
    }
}
