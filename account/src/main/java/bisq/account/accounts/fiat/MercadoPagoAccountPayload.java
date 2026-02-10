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
import bisq.account.accounts.util.AccountUtils;
import bisq.account.accounts.SingleCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.NetworkDataValidation;
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
public final class MercadoPagoAccountPayload extends AccountPayload<FiatPaymentMethod>
        implements SingleCurrencyAccountPayload {
    public static final String COUNTRY_CODE = "AR";
    public static final int HOLDER_NAME_MIN_LENGTH = 2;
    public static final int HOLDER_NAME_MAX_LENGTH = 70;
    public static final int HOLDER_ID_MIN_LENGTH = 1;
    public static final int HOLDER_ID_MAX_LENGTH = 50;

    private final String holderName;
    private final String holderId;

    public MercadoPagoAccountPayload(String id, String holderName, String holderId) {
        this(id, AccountUtils.generateSalt(), holderName, holderId);
    }

    public MercadoPagoAccountPayload(String id, byte[] salt, String holderName, String holderId) {
        super(id, salt);
        this.holderName = holderName;
        this.holderId = holderId;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(holderName, HOLDER_NAME_MIN_LENGTH, HOLDER_NAME_MAX_LENGTH);
        NetworkDataValidation.validateRequiredText(holderId, HOLDER_ID_MIN_LENGTH, HOLDER_ID_MAX_LENGTH);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setMercadoPagoAccountPayload(toMercadoPagoAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.MercadoPagoAccountPayload toMercadoPagoAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getMercadoPagoAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.MercadoPagoAccountPayload.Builder getMercadoPagoAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.MercadoPagoAccountPayload.newBuilder()
                .setHolderName(holderName)
                .setHolderId(holderId);
    }

    public static MercadoPagoAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getMercadoPagoAccountPayload();
        return new MercadoPagoAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getHolderName(),
                payload.getHolderId()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.MERCADO_PAGO);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.mercadoPago.holderId"), holderId,
                Res.get("paymentAccounts.holderName"), holderName
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(COUNTRY_CODE.getBytes(StandardCharsets.UTF_8),
                holderId.getBytes(StandardCharsets.UTF_8),
                holderName.getBytes(StandardCharsets.UTF_8));
        return super.getFingerprint(data);
    }
}
