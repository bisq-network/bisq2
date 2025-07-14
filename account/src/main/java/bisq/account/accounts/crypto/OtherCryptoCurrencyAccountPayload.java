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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class OtherCryptoCurrencyAccountPayload extends CryptoCurrencyAccountPayload {
    public OtherCryptoCurrencyAccountPayload(String id,
                                             String currencyCode,
                                             String address,
                                             boolean isInstant,
                                             boolean isAutoConf) {
        super(id, currencyCode, address, isInstant, isAutoConf);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        bisq.account.protobuf.CryptoCurrencyAccountPayload.Builder builder = getCryptoCurrencyAccountPayloadBuilder(serializeForHash)
                .setOtherCryptoCurrencyAccountPayload(getOtherCryptoCurrencyAccountPayloadBuilder(serializeForHash));
        return getAccountPayloadBuilder(serializeForHash)
                .setCryptoCurrencyAccountPayload(resolveBuilder(builder, serializeForHash).build());
    }

    private bisq.account.protobuf.OtherCryptoCurrencyAccountPayload.Builder getOtherCryptoCurrencyAccountPayloadBuilder(
            boolean serializeForHash) {
        return bisq.account.protobuf.OtherCryptoCurrencyAccountPayload.newBuilder();
    }

    public static OtherCryptoCurrencyAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var cryptoCurrency = proto.getCryptoCurrencyAccountPayload();
        return new OtherCryptoCurrencyAccountPayload(
                proto.getId(),
                cryptoCurrency.getCurrencyCode(),
                cryptoCurrency.getAddress(),
                cryptoCurrency.getIsInstant(),
                cryptoCurrency.getIsAutoConf());
    }
}
