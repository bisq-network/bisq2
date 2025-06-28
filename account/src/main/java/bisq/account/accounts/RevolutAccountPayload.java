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

package bisq.account.accounts;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.FiatPaymentRail;
import bisq.account.payment_method.FiatPaymentRailUtil;
import bisq.common.validation.NetworkDataValidation;
import bisq.common.validation.PaymentAccountValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class RevolutAccountPayload extends AccountPayload<FiatPaymentMethod> implements MultiCurrencyAccountPayload {
    public static final int USER_NAME_MIN_LENGTH = 2;
    public static final int USER_NAME_MAX_LENGTH = 70;

    private final List<String> selectedCurrencyCodes;
    private final String userName;

    public RevolutAccountPayload(String id,
                                 List<String> selectedCurrencyCodes,
                                 String userName) {
        super(id);
        this.selectedCurrencyCodes = selectedCurrencyCodes;
        this.userName = userName;
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(userName, USER_NAME_MIN_LENGTH, USER_NAME_MAX_LENGTH);
        PaymentAccountValidation.isValidCountryCodes(selectedCurrencyCodes,
                FiatPaymentRailUtil.getRevolutCountryCodes(),
                "Revolut country codes");
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setRevolutAccountPayload(toRevolutAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.RevolutAccountPayload toRevolutAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getRevolutAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.RevolutAccountPayload.Builder getRevolutAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.RevolutAccountPayload.newBuilder()
                .addAllSelectedCurrencyCodes(selectedCurrencyCodes)
                .setUserName(userName);
    }

    public static RevolutAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        bisq.account.protobuf.RevolutAccountPayload revolutAccountPayload = proto.getRevolutAccountPayload();
        return new RevolutAccountPayload(proto.getId(),
                revolutAccountPayload.getSelectedCurrencyCodesList(),
                revolutAccountPayload.getUserName());
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.REVOLUT);
    }
}