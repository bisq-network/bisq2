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

import bisq.account.accounts.AccountPayload;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.util.StringUtils;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class SatispayAccountPayload extends AccountPayload<FiatPaymentMethod>
        implements SingleCurrencyAccountPayload {
    public static final String COUNTRY_CODE = "IT";

    private final String holderName;
    private final String mobileNr;

    public SatispayAccountPayload(String id, String holderName, String mobileNr) {
        this(id, AccountUtils.generateSalt(), holderName, mobileNr);
    }

    public SatispayAccountPayload(String id, byte[] salt, String holderName, String mobileNr) {
        super(id, salt);
        this.holderName = holderName;
        this.mobileNr = mobileNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        PaymentAccountValidation.validateHolderName(holderName);
        checkArgument(StringUtils.isNotEmpty(mobileNr), "mobileNr must not be empty");
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setSatispayAccountPayload(toSatispayAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.SatispayAccountPayload toSatispayAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getSatispayAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.SatispayAccountPayload.Builder getSatispayAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.SatispayAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setMobileNr(mobileNr);
    }

    public static SatispayAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getSatispayAccountPayload();
        return new SatispayAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getHolderName(),
                payload.getMobileNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SATISPAY);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.holderName"), holderName,
                Res.get("paymentAccounts.mobileNr"), mobileNr
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(COUNTRY_CODE.getBytes(StandardCharsets.UTF_8),
                holderName.getBytes(StandardCharsets.UTF_8));
        return super.getFingerprint(data);
    }
}
