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

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public abstract class CountryBasedAccountPayload extends AccountPayload {
    protected final String countryCode;

    public CountryBasedAccountPayload(String id, String paymentMethodName, String countryCode) {
        super(id, paymentMethodName);
        this.countryCode = countryCode;
    }

    @Override
    public void verify() {
        super.verify();
        NetworkDataValidation.validateCode(countryCode);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setCountryBasedAccountPayload(toCountryBasedAccountPayloadProto(serializeForHash));
    }

    private bisq.account.protobuf.CountryBasedAccountPayload toCountryBasedAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getCountryBasedAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    protected bisq.account.protobuf.CountryBasedAccountPayload.Builder getCountryBasedAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.CountryBasedAccountPayload.newBuilder()
                .setCountryCode(countryCode);
    }

    public static CountryBasedAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        switch (proto.getCountryBasedAccountPayload().getMessageCase()) {
            case BANKACCOUNTPAYLOAD: {
                return BankAccountPayload.fromProto(proto);
            }
            case SEPAACCOUNTPAYLOAD: {
                return SepaAccountPayload.fromProto(proto);
            }
            case F2FACCOUNTPAYLOAD: {
                return F2FAccountPayload.fromProto(proto);
            }
            case PIXACCOUNTPAYLOAD: {
                return PixAccountPayload.fromProto(proto);
            }
            case STRIKEACCOUNTPAYLOAD: {
                return StrikeAccountPayload.fromProto(proto);
            }
            case AMAZONGIFTCARDACCOUNTPAYLOAD: {
                return AmazonGiftCardAccountPayload.fromProto(proto);
            }
            case UPIACCOUNTPAYLOAD: {
                return UpiAccountPayload.fromProto(proto);
            }
            case BIZUMACCOUNTPAYLOAD: {
                return BizumAccountPayload.fromProto(proto);
            }
            case MESSAGE_NOT_SET: {
                throw new UnresolvableProtobufMessageException(proto);
            }
        }
        throw new UnresolvableProtobufMessageException(proto);
    }
}