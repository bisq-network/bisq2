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

package bisq.account.accounts.fiat;

import bisq.account.accounts.AccountUtils;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class UpiAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String virtualPaymentAddress;

    public UpiAccountPayload(String id, String countryCode, String virtualPaymentAddress) {
        this(id, AccountUtils.generateSalt(), countryCode, virtualPaymentAddress);
    }

    private UpiAccountPayload(String id, byte[] salt, String countryCode, String virtualPaymentAddress) {
        super(id, salt, countryCode);
        this.virtualPaymentAddress = virtualPaymentAddress;
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setUpiAccountPayload(
                toUpiAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.UpiAccountPayload toUpiAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getUpiAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.UpiAccountPayload.Builder getUpiAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.UpiAccountPayload.newBuilder().setVirtualPaymentAddress(virtualPaymentAddress);
    }

    public static UpiAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var countryBasedAccountPayload = proto.getCountryBasedAccountPayload();
        return new UpiAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),

                countryBasedAccountPayload.getCountryCode(),
                countryBasedAccountPayload.getUpiAccountPayload().getVirtualPaymentAddress());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.UPI);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.upi.virtualPaymentAddress"), virtualPaymentAddress
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(virtualPaymentAddress.getBytes(StandardCharsets.UTF_8));
    }
}
