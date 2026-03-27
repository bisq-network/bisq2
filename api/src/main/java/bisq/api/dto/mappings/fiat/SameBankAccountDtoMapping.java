package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SameBankAccount;
import bisq.account.accounts.fiat.SameBankAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SameBankAccountDto;
import bisq.api.dto.account.fiat.SameBankAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class SameBankAccountDtoMapping {
    public static SameBankAccount toBisq2Model(SameBankAccountDto dto) {
        SameBankAccountPayloadDto payloadDto = dto.accountPayload();
        SameBankAccountPayload payload = new SameBankAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.holderName(),
                payloadDto.holderId(),
                payloadDto.bankName(),
                payloadDto.bankId(),
                payloadDto.branchId(),
                payloadDto.accountNr(),
                payloadDto.bankAccountType(),
                payloadDto.nationalAccountId()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SameBankAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SameBankAccountDto fromBisq2Model(SameBankAccount account) {
        return new SameBankAccountDto(
                account.getAccountName(),
                new SameBankAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getHolderId(),
                        account.getAccountPayload().getBankName(),
                        account.getAccountPayload().getBankId(),
                        account.getAccountPayload().getBranchId(),
                        account.getAccountPayload().getAccountNr(),
                        account.getAccountPayload().getBankAccountType(),
                        account.getAccountPayload().getNationalAccountId()
                )
        );
    }
}
