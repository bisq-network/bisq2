package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.CashDepositAccount;
import bisq.account.accounts.fiat.CashDepositAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.CashDepositAccountDto;
import bisq.api.dto.account.fiat.CashDepositAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class CashDepositAccountDtoMapping {
    public static CashDepositAccount toBisq2Model(CashDepositAccountDto dto) {
        CashDepositAccountPayloadDto payloadDto = dto.accountPayload();
        CashDepositAccountPayload payload = new CashDepositAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.holderName().orElse(null),
                payloadDto.holderTaxId(),
                payloadDto.bankName().orElse(null),
                payloadDto.bankId(),
                payloadDto.branchId(),
                payloadDto.accountNr(),
                payloadDto.bankAccountType(),
                payloadDto.nationalAccountId(),
                payloadDto.requirements()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new CashDepositAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static CashDepositAccountDto fromBisq2Model(CashDepositAccount account) {
        return new CashDepositAccountDto(
                account.getAccountName(),
                new CashDepositAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getHolderName(),
                        account.getAccountPayload().getHolderId(),
                        account.getAccountPayload().getBankName(),
                        account.getAccountPayload().getBankId(),
                        account.getAccountPayload().getBranchId(),
                        account.getAccountPayload().getAccountNr(),
                        account.getAccountPayload().getBankAccountType(),
                        account.getAccountPayload().getNationalAccountId(),
                        account.getAccountPayload().getRequirements()
                )
        );
    }
}
