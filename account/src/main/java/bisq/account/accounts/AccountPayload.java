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

import bisq.common.proto.NetworkProto;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * AccountPayload is sent over the wire to the peer. It must not contain sensitive data.
 */
@Getter
@Slf4j
@ToString
@EqualsAndHashCode
public abstract class AccountPayload implements NetworkProto {
    protected final String id;
    private final String paymentMethodName;

    public AccountPayload(String id, String paymentMethodName) {
        this.id = id;
        this.paymentMethodName = paymentMethodName;
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateText(paymentMethodName, 100);
    }

    @Override
    public bisq.account.protobuf.AccountPayload toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.account.protobuf.AccountPayload completeProto() {
        return toProto(false);
    }

    protected bisq.account.protobuf.AccountPayload.Builder getAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AccountPayload.newBuilder()
                .setId(id)
                .setPaymentMethodName(paymentMethodName);
    }

    public static AccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return switch (proto.getMessageCase()) {
            case ZELLEACCOUNTPAYLOAD -> ZelleAccountPayload.fromProto(proto);
            case COUNTRYBASEDACCOUNTPAYLOAD -> CountryBasedAccountPayload.fromProto(proto);
            case REVOLUTACCOUNTPAYLOAD -> RevolutAccountPayload.fromProto(proto);
            case USERDEFINEDFIATACCOUNTPAYLOAD -> UserDefinedFiatAccountPayload.fromProto(proto);
            case FASTERPAYMENTSACCOUNTPAYLOAD -> FasterPaymentsAccountPayload.fromProto(proto);
            case CASHBYMAILACCOUNTPAYLOAD -> CashByMailAccountPayload.fromProto(proto);
            case INTERACETRANSFERACCOUNTPAYLOAD -> InteracETransferAccountPayload.fromProto(proto);
            case CASHAPPACCOUNTPAYLOAD -> CashAppAccountPayload.fromProto(proto);
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
            default -> throw new UnresolvableProtobufMessageException(proto);
        };
    }
}