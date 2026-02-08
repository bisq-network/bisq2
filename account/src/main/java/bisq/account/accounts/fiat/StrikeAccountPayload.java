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

import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.account.protobuf.AccountPayload;
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
public final class StrikeAccountPayload extends CountryBasedAccountPayload implements SingleCurrencyAccountPayload {
    private final String holderName;

    public StrikeAccountPayload(String id, String countryCode, String holderName) {
        super(id, countryCode);
        this.holderName = holderName;
    }

    @Override
    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return super.getCountryBasedAccountPayloadBuilder(serializeForHash).setStrikeAccountPayload(
                toStrikeAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.StrikeAccountPayload toStrikeAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getStrikeAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.StrikeAccountPayload.Builder getStrikeAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.StrikeAccountPayload.newBuilder().setHolderName(holderName);
    }

    public static StrikeAccountPayload fromProto(AccountPayload proto) {
        return new StrikeAccountPayload(
                proto.getId(),
                proto.getCountryBasedAccountPayload().getCountryCode(),
                proto.getCountryBasedAccountPayload().getStrikeAccountPayload().getHolderName());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.STRIKE);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(holderName.getBytes(StandardCharsets.UTF_8));
    }
}
