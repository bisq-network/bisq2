package bisq.api.dto.mappings.account.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.api.dto.account.AccountMetadataDto;
import bisq.api.dto.account.crypto.MoneroAccountDto;
import bisq.api.dto.account.crypto.MoneroAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountKeyMapping;
import bisq.api.dto.mappings.account.PaymentAccountMetadataDtoMapping;
import bisq.common.util.StringUtils;

public class MoneroAccountDtoMapping {
    public static MoneroAccount toBisq2Model(MoneroAccountDto dto) {
        MoneroAccountPayloadDto payloadDto = dto.accountPayload();
        MoneroAccountPayload payload = new MoneroAccountPayload(
                StringUtils.createUid(),
                payloadDto.address(),
                payloadDto.isInstant(),
                payloadDto.isAutoConf(),
                payloadDto.autoConfNumConfirmations(),
                payloadDto.autoConfMaxTradeAmount(),
                payloadDto.autoConfExplorerUrls(),
                payloadDto.useSubAddresses(),
                payloadDto.mainAddress(),
                payloadDto.privateViewKey(),
                payloadDto.subAddress(),
                payloadDto.accountIndex(),
                payloadDto.initialSubAddressIndex()
        );
        PaymentAccountKeyMapping.KeyData keyData = PaymentAccountKeyMapping.createDefault();
        return new MoneroAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                dto.accountName(),
                payload,
                keyData.keyPair(),
                keyData.keyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static MoneroAccountDto fromBisq2Model(MoneroAccount account) {
        AccountMetadataDto accountMetadata = PaymentAccountMetadataDtoMapping.mapAccountMetadata(account);

        MoneroAccountPayload payload = account.getAccountPayload();
        return new MoneroAccountDto(
                account.getAccountName(),
                new MoneroAccountPayloadDto(
                        payload.getAddress(),
                        payload.isInstant(),
                        payload.getIsAutoConf(),
                        payload.getAutoConfNumConfirmations(),
                        payload.getAutoConfMaxTradeAmount(),
                        payload.getAutoConfExplorerUrls(),
                        payload.isUseSubAddresses(),
                        payload.getMainAddress(),
                        payload.getPrivateViewKey(),
                        payload.getSubAddress(),
                        payload.getAccountIndex(),
                        payload.getInitialSubAddressIndex(),
                        payload.getPaymentMethod().getDisplayString()
                ),
                accountMetadata.creationDate(),
                accountMetadata.tradeLimitInfo(),
                accountMetadata.tradeDuration()
        );
    }
}
