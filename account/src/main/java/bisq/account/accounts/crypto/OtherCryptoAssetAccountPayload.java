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

import bisq.account.accounts.AccountUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class OtherCryptoAssetAccountPayload extends CryptoAssetAccountPayload {
    public OtherCryptoAssetAccountPayload(String id,
                                          String currencyCode,
                                          String address,
                                          boolean isInstant,
                                          Optional<Boolean> isAutoConf,
                                          Optional<Integer> autoConfNumConfirmations,
                                          Optional<Long> autoConfMaxTradeAmount,
                                          Optional<String> autoConfExplorerUrls) {
        this(id,
                AccountUtils.generateSalt(),
                currencyCode,
                address,
                isInstant,
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                autoConfExplorerUrls);
    }

    private OtherCryptoAssetAccountPayload(String id,
                                           byte[] salt,
                                           String currencyCode,
                                           String address,
                                           boolean isInstant,
                                           Optional<Boolean> isAutoConf,
                                           Optional<Integer> autoConfNumConfirmations,
                                           Optional<Long> autoConfMaxTradeAmount,
                                           Optional<String> autoConfExplorerUrls) {
        super(id,
                salt,
                currencyCode,
                address,
                isInstant,
                isAutoConf,
                autoConfNumConfirmations,
                autoConfMaxTradeAmount,
                autoConfExplorerUrls);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoAssetAccountPayload.Builder builder = getCryptoAssetAccountPayloadBuilder(serializeForHash)
                .setOtherCryptoAssetAccountPayload(getOtherCryptoAssetAccountPayloadBuilder(serializeForHash));
        return getAccountPayloadBuilder(serializeForHash)
                .setCryptoAssetAccountPayload(resolveBuilder(builder, serializeForHash).build());
    }

    private bisq.account.protobuf.OtherCryptoAssetAccountPayload.Builder getOtherCryptoAssetAccountPayloadBuilder(
            boolean serializeForHash) {
        return bisq.account.protobuf.OtherCryptoAssetAccountPayload.newBuilder();
    }

    public static OtherCryptoAssetAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var cryptoAssetAccountPayload = proto.getCryptoAssetAccountPayload();
        return new OtherCryptoAssetAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                cryptoAssetAccountPayload.getCurrencyCode(),
                cryptoAssetAccountPayload.getAddress(),
                cryptoAssetAccountPayload.getIsInstant(),
                cryptoAssetAccountPayload.hasIsAutoConf() ? Optional.of(cryptoAssetAccountPayload.getIsAutoConf()) : Optional.empty(),
                cryptoAssetAccountPayload.hasAutoConfNumConfirmations() ? Optional.of(cryptoAssetAccountPayload.getAutoConfNumConfirmations()) : Optional.empty(),
                cryptoAssetAccountPayload.hasAutoConfMaxTradeAmount() ? Optional.of(cryptoAssetAccountPayload.getAutoConfMaxTradeAmount()) : Optional.empty(),
                cryptoAssetAccountPayload.hasAutoConfExplorerUrls() ? Optional.of(cryptoAssetAccountPayload.getAutoConfExplorerUrls()) : Optional.empty());
    }

    @Override
    public byte[] getFingerprint() {
        String data = currencyCode + address;
        return super.getFingerprint(data.getBytes(StandardCharsets.UTF_8));
    }
}
