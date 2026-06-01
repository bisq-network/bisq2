package bisq.api.dto.mappings.account.crypto;

import bisq.account.accounts.AccountOrigin;
import bisq.account.accounts.crypto.MoneroAccount;
import bisq.account.accounts.crypto.MoneroAccountPayload;
import bisq.api.dto.account.PaymentAccountDto;
import bisq.api.dto.account.crypto.monero.MoneroAccountPayloadDto;
import bisq.api.dto.account.crypto.common.CryptoPaymentRailDto;
import bisq.api.dto.account.crypto.monero.CreateMoneroAccountPayloadDto;
import bisq.api.dto.mappings.account.PaymentAccountDtoMappingHelper;
import bisq.common.asset.CryptoAssetRepository;
import bisq.common.util.StringUtils;

public class MoneroAccountDtoMapping {
    public static MoneroAccount toBisq2Model(String accountName, CreateMoneroAccountPayloadDto payloadDto) {
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
        return new MoneroAccount(
                StringUtils.createUid(),
                System.currentTimeMillis(),
                accountName,
                payload,
                PaymentAccountDtoMappingHelper.createDefaultKeyPair(),
                PaymentAccountDtoMappingHelper.getDefaultKeyType(),
                AccountOrigin.BISQ2_NEW
        );
    }

    public static PaymentAccountDto fromBisq2Model(MoneroAccount account) {

        MoneroAccountPayload payload = account.getAccountPayload();
        return new PaymentAccountDto(
                account.getAccountName(),
                CryptoPaymentRailDto.MONERO,
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
                        payload.getPaymentMethod().getName(),
                        payload.getCurrencyCode(),
                        CryptoAssetRepository.isAutoConfSupported(payload.getCurrencyCode())
                ),
                PaymentAccountDtoMappingHelper.getCreationDate(account),
                PaymentAccountDtoMappingHelper.getTradeLimitInfo(account),
                PaymentAccountDtoMappingHelper.getTradeDuration(account)
        );
    }
}
