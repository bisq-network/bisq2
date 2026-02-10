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
public final class PerfectMoneyAccountPayload extends AccountPayload<FiatPaymentMethod> implements SingleCurrencyAccountPayload {
    public static final int ACCOUNT_NR_MIN_LENGTH = 1;
    public static final int ACCOUNT_NR_MAX_LENGTH = 50;

    private final String accountNr;

    public PerfectMoneyAccountPayload(String id, String accountNr) {
        this(id, AccountUtils.generateSalt(), accountNr);
    }

    public PerfectMoneyAccountPayload(String id, byte[] salt, String accountNr) {
        super(id, salt);
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();

        NetworkDataValidation.validateRequiredText(accountNr, ACCOUNT_NR_MIN_LENGTH, ACCOUNT_NR_MAX_LENGTH);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setPerfectMoneyAccountPayload(toPerfectMoneyAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.PerfectMoneyAccountPayload toPerfectMoneyAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getPerfectMoneyAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.PerfectMoneyAccountPayload.Builder getPerfectMoneyAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.PerfectMoneyAccountPayload.newBuilder()
                .setAccountNr(accountNr);
    }

    public static PerfectMoneyAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var payload = proto.getPerfectMoneyAccountPayload();
        return new PerfectMoneyAccountPayload(
                proto.getId(),
                proto.getSalt().toByteArray(),
                payload.getAccountNr()
        );
    }

    @Override
    public FiatPaymentMethod getPaymentMethod() {
        return FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.PERFECT_MONEY);
    }

    @Override
    public String getAccountDataDisplayString() {
        return new AccountDataDisplayStringBuilder(
                Res.get("paymentAccounts.accountNr"), accountNr
        ).toString();
    }

    @Override
    public byte[] getFingerprint() {
        return super.getFingerprint(accountNr.getBytes(StandardCharsets.UTF_8));
    }
}
