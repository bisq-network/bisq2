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
import bisq.common.locale.Country;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class DomesticWireTransferAccount extends BankAccount<DomesticWireTransferAccountPayload> {
    private static final FiatPaymentMethod PAYMENT_METHOD = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.NATIONAL_BANK);

    public DomesticWireTransferAccount(String accountName, DomesticWireTransferAccountPayload payload) {
        super(accountName, PAYMENT_METHOD, payload);
    }

    public static DomesticWireTransferAccount fromProto(bisq.account.protobuf.Account proto) {
        return new DomesticWireTransferAccount(
                proto.getAccountName(),
                DomesticWireTransferAccountPayload.fromProto(proto.getAccountPayload()));
    }

    @Override
    protected bisq.account.protobuf.BankAccount.Builder getBankAccountBuilder(boolean serializeForHash) {
        return super.getBankAccountBuilder(serializeForHash).setDomesticWireTransferAccount(
                toDomesticWireTransferAccountProto(serializeForHash));
    }

    private bisq.account.protobuf.DomesticWireTransferAccount toDomesticWireTransferAccountProto(boolean serializeForHash) {
        return resolveBuilder(getDomesticWireTransferAccountBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.DomesticWireTransferAccount.Builder getDomesticWireTransferAccountBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.DomesticWireTransferAccount.newBuilder();
    }
}