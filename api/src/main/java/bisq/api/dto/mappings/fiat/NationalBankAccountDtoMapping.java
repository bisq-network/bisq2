package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.NationalBankAccount;
import bisq.account.accounts.fiat.NationalBankAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.NationalBankAccountDto;
import bisq.api.dto.account.fiat.NationalBankAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class NationalBankAccountDtoMapping {
    public static NationalBankAccount toBisq2Model(NationalBankAccountDto dto) {
        NationalBankAccountPayloadDto payloadDto = dto.accountPayload();
        NationalBankAccountPayload payload = new NationalBankAccountPayload(
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
        return new NationalBankAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static NationalBankAccountDto fromBisq2Model(NationalBankAccount account) {
        return new NationalBankAccountDto(
                account.getAccountName(),
                new NationalBankAccountPayloadDto(
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
