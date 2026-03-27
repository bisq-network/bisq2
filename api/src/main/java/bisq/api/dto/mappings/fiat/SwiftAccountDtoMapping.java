package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.SwiftAccount;
import bisq.account.accounts.fiat.SwiftAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.SwiftAccountDto;
import bisq.api.dto.account.fiat.SwiftAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class SwiftAccountDtoMapping {
    public static SwiftAccount toBisq2Model(SwiftAccountDto dto) {
        SwiftAccountPayloadDto payloadDto = dto.accountPayload();
        SwiftAccountPayload payload = new SwiftAccountPayload(
                StringUtils.createUid(),
                payloadDto.bankCountryCode(),
                payloadDto.beneficiaryName(),
                payloadDto.beneficiaryAccountNr(),
                payloadDto.beneficiaryPhone(),
                payloadDto.beneficiaryAddress(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.bankSwiftCode(),
                payloadDto.bankName(),
                payloadDto.bankBranch(),
                payloadDto.bankAddress(),
                payloadDto.intermediaryBankCountryCode(),
                payloadDto.intermediaryBankSwiftCode(),
                payloadDto.intermediaryBankName(),
                payloadDto.intermediaryBankBranch(),
                payloadDto.intermediaryBankAddress(),
                payloadDto.additionalInstructions()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new SwiftAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static SwiftAccountDto fromBisq2Model(SwiftAccount account) {
        return new SwiftAccountDto(
                account.getAccountName(),
                new SwiftAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getBeneficiaryName(),
                        account.getAccountPayload().getBeneficiaryAccountNr(),
                        account.getAccountPayload().getBeneficiaryPhone(),
                        account.getAccountPayload().getBeneficiaryAddress(),
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getBankSwiftCode(),
                        account.getAccountPayload().getBankName(),
                        account.getAccountPayload().getBankBranch(),
                        account.getAccountPayload().getBankAddress(),
                        account.getAccountPayload().getIntermediaryBankCountryCode(),
                        account.getAccountPayload().getIntermediaryBankSwiftCode(),
                        account.getAccountPayload().getIntermediaryBankName(),
                        account.getAccountPayload().getIntermediaryBankBranch(),
                        account.getAccountPayload().getIntermediaryBankAddress(),
                        account.getAccountPayload().getAdditionalInstructions()
                )
        );
    }
}
