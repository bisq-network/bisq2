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
import bisq.account.accounts.SelectableCurrencyAccountPayload;
import bisq.account.accounts.util.AccountDataDisplayStringBuilder;
import bisq.account.accounts.util.AccountUtils;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.util.ByteArrayUtils;
import bisq.common.validation.PaymentAccountValidation;
import bisq.i18n.Res;
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
public final class CashByMailAccountPayload extends AccountPayload<FiatPaymentMethod> implements SelectableCurrencyAccountPayload {
    private final String selectedCurrencyCode;
    private final String postalAddress;
    private final String contact;
    private final String extraInfo;

    public CashByMailAccountPayload(String id,
                                    String selectedCurrencyCode,
                                    String postalAddress,
                                    String contact,
                                    String extraInfo) {
        this(id, AccountUtils.generateSalt(), selectedCurrencyCode, postalAddress, contact, extraInfo);
    }

    public CashByMailAccountPayload(String id,
                                    byte[] salt,
                                    String selectedCurrencyCode,
                                    String postalAddress,
                                    String contact,
                                    String extraInfo) {
        super(id, salt);
        this.selectedCurrencyCode = selectedCurrencyCode;
        this.postalAddress = postalAddress;
        this.contact = contact;
        this.extraInfo = extraInfo;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        PaymentAccountValidation.validateCurrencyCode(selectedCurrencyCode);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setCashByMailAccountPayload(toCashByMailAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.CashByMailAccountPayload toCashByMailAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCashByMailAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.CashByMailAccountPayload.Builder getCashByMailAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CashByMailAccountPayload.newBuilder()
                .setSelectedCurrencyCode(selectedCurrencyCode)
                .setPostalAddress(postalAddress)
                .setContact(contact)
                .setExtraInfo(extraInfo);
    }

    public static CashByMailAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var cashByMailPayload = proto.getCashByMailAccountPayload();
        return new CashByMailAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                cashByMailPayload.getSelectedCurrencyCode(),
                cashByMailPayload.getPostalAddress(),
                cashByMailPayload.getContact(),
                cashByMailPayload.getExtraInfo()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.CASH_BY_MAIL);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.postalAddress"), postalAddress,
                Res.get("paymentAccounts.cashByMail.contact"), contact,
                Res.get("paymentAccounts.cashByMail.extraInfo"), extraInfo
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        byte[] data = ByteArrayUtils.concat(contact.getBytes(StandardCharsets.UTF_8),
                postalAddress.getBytes(StandardCharsets.UTF_8));
        return super.getFingerprint(data);
    }
}
