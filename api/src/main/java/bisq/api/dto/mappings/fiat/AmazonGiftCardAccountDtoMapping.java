package bisq.api.dto.mappings.fiat;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.fiat.AmazonGiftCardAccount;
import bisq.account.accounts.fiat.AmazonGiftCardAccountPayload;
import bisq.account.timestamp.KeyType;
import bisq.api.dto.account.fiat.AmazonGiftCardAccountDto;
import bisq.api.dto.account.fiat.AmazonGiftCardAccountPayloadDto;
import bisq.common.util.StringUtils;
import bisq.security.keys.KeyGeneration;

import java.security.KeyPair;

public class AmazonGiftCardAccountDtoMapping {
    public static AmazonGiftCardAccount toBisq2Model(AmazonGiftCardAccountDto dto) {
        AmazonGiftCardAccountPayloadDto payloadDto = dto.accountPayload();
        AmazonGiftCardAccountPayload payload = new AmazonGiftCardAccountPayload(
                StringUtils.createUid(),
                payloadDto.countryCode(),
                payloadDto.selectedCurrencyCode(),
                payloadDto.emailOrMobileNr()
        );
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        KeyType keyType = KeyType.EC;
        return new AmazonGiftCardAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyPair,
                keyType,
                AccountOrigin.BISQ2_NEW
        );
    }

    public static AmazonGiftCardAccountDto fromBisq2Model(AmazonGiftCardAccount account) {
        return new AmazonGiftCardAccountDto(
                account.getAccountName(),
                new AmazonGiftCardAccountPayloadDto(
                        account.getAccountPayload().getCountryCode(),
                        account.getAccountPayload().getSelectedCurrencyCode(),
                        account.getAccountPayload().getEmailOrMobileNr()
                )
        );
    }
}
