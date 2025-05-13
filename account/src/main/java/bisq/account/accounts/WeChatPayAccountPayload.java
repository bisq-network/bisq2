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

import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class WeChatPayAccountPayload extends AccountPayload {

    private final String accountNr;

    public WeChatPayAccountPayload(String id, String paymentMethodName, String accountNr) {
        super(id, paymentMethodName);
        this.accountNr = accountNr;

        verify();
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateText(accountNr, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setWeChatPayAccountPayload(toWeChatPayAccountPayloadProto(serializeForHash));
    }

    public static WeChatPayAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        var weChatPayPayload = proto.getWeChatPayAccountPayload();
        return new WeChatPayAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName(),
                weChatPayPayload.getAccountNr()
        );
    }

    private bisq.account.protobuf.WeChatPayAccountPayload toWeChatPayAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getWeChatPayAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.WeChatPayAccountPayload.Builder getWeChatPayAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.WeChatPayAccountPayload.newBuilder()
                .setAccountNr(accountNr);
    }
}