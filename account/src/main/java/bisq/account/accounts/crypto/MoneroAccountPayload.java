/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.account.accounts.crypto;

import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.crypto.CryptoPaymentMethod;
import bisq.common.asset.CryptoAssetRepository;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class MoneroAccountPayload extends CryptoAssetAccountPayload {
    private final boolean useSubAddresses;
    private final Optional<String> mainAddress;
    private final Optional<String> privateViewKey;
    // subAddress is generated from privateViewKey, accountIndex and subAddressIndex
    private final Optional<String> subAddress;
    private final Optional<Integer> accountIndex;
    private final Optional<Integer> initialSubAddressIndex;

    public MoneroAccountPayload(String id,
                                String address,
                                boolean isInstant,
                                Optional<Boolean> isAutoConf,
                                Optional<Integer> autoConfNumConfirmations,
                                Optional<Long> autoConfMaxTradeAmount,
                                Optional<String> autoConfExplorerUrls,
                                boolean useSubAddresses,
                                Optional<String> mainAddress,
                                Optional<String> privateViewKey,
                                Optional<String> subAddress,
                                Optional<Integer> accountIndex,
                                Optional<Integer> initialSubAddressIndex) {
        this(id,
                AccountUtils.generateSalt(),
                CryptoAssetRepository.XMR.getCode(),
                address,
                isInstant,
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                autoConfExplorerUrls,
                useSubAddresses,
                mainAddress,
                privateViewKey,
                subAddress,
                accountIndex,
                initialSubAddressIndex);
    }

    public MoneroAccountPayload(String id,
                                byte[] salt,
                                String currencyCode,
                                String address,
                                boolean isInstant,
                                Optional<Boolean> isAutoConf,
                                Optional<Integer> autoConfNumConfirmations,
                                Optional<Long> autoConfMaxTradeAmount,
                                Optional<String> autoConfExplorerUrls,
                                boolean useSubAddresses,
                                Optional<String> mainAddress,
                                Optional<String> privateViewKey,
                                Optional<String> subAddress,
                                Optional<Integer> accountIndex,
                                Optional<Integer> initialSubAddressIndex) {
        super(id,
                salt,
                currencyCode,
                address,
                isInstant,
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                autoConfExplorerUrls);
        this.useSubAddresses = useSubAddresses;
        this.mainAddress = mainAddress;
        this.privateViewKey = privateViewKey;
        this.subAddress = subAddress;
        this.accountIndex = accountIndex;
        this.initialSubAddressIndex = initialSubAddressIndex;

        verify();
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoAssetAccountPayload.Builder builder = getCryptoAssetAccountPayloadBuilder(serializeForHash)
                .setMoneroAccountPayload(getMoneroAccountPayloadBuilder(serializeForHash));
        bisq.account.protobuf.CryptoAssetAccountPayload cryptoAssetAccountPayload = resolveBuilder(builder, serializeForHash).build();
        return getAccountPayloadBuilder(serializeForHash)
                .setCryptoAssetAccountPayload(cryptoAssetAccountPayload);
    }

    private bisq.account.protobuf.MoneroAccountPayload.Builder getMoneroAccountPayloadBuilder(
            boolean serializeForHash) {
        bisq.account.protobuf.MoneroAccountPayload.Builder builder = bisq.account.protobuf.MoneroAccountPayload.newBuilder()
                .setUseSubAddresses(useSubAddresses);
        mainAddress.ifPresent(builder::setMainAddress);
        privateViewKey.ifPresent(builder::setPrivateViewKey);
        subAddress.ifPresent(builder::setSubAddress);
        accountIndex.ifPresent(builder::setAccountIndex);
        initialSubAddressIndex.ifPresent(builder::setInitialSubAddressIndex);
        return builder;
    }

    public static MoneroAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var cryptoAssetAccountPayload = proto.getCryptoAssetAccountPayload();
        var monero = cryptoAssetAccountPayload.getMoneroAccountPayload();
        return new MoneroAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                cryptoAssetAccountPayload.getCurrencyCode(),
                cryptoAssetAccountPayload.getAddress(),
                cryptoAssetAccountPayload.getIsInstant(),
                cryptoAssetAccountPayload.hasIsAutoConf() ? Optional.of(cryptoAssetAccountPayload.getIsAutoConf()) : Optional.empty(),
                cryptoAssetAccountPayload.hasAutoConfNumConfirmations() ? Optional.of(cryptoAssetAccountPayload.getAutoConfNumConfirmations()) : Optional.empty(),
                cryptoAssetAccountPayload.hasAutoConfMaxTradeAmount() ? Optional.of(cryptoAssetAccountPayload.getAutoConfMaxTradeAmount()) : Optional.empty(),
                cryptoAssetAccountPayload.hasAutoConfExplorerUrls() ? Optional.of(cryptoAssetAccountPayload.getAutoConfExplorerUrls()) : Optional.empty(),
                monero.getUseSubAddresses(),
                monero.hasMainAddress() ? Optional.of(monero.getMainAddress()) : Optional.empty(),
                monero.hasPrivateViewKey() ? Optional.of(monero.getPrivateViewKey()) : Optional.empty(),
                monero.hasSubAddress() ? Optional.of(monero.getSubAddress()) : Optional.empty(),
                monero.hasAccountIndex() ? Optional.of(monero.getAccountIndex()) : Optional.empty(),
                monero.hasInitialSubAddressIndex() ? Optional.of(monero.getInitialSubAddressIndex()) : Optional.empty()
        );
    }

    @Override
    public CryptoPaymentMethod getPaymentMethod() {
        return new CryptoPaymentMethod(CryptoAssetRepository.XMR.getCode());
    }

    @Override
    public byte[] getFingerprint() {
        String data = privateViewKey.orElse("");
        return super.getFingerprint(data.getBytes(StandardCharsets.UTF_8));
    }
}
