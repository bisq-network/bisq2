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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@ToString
@EqualsAndHashCode(callSuper = true)
public final class BsqSwapAccountPayload extends AccountPayload {

    public BsqSwapAccountPayload(String id, String paymentMethodName) {
        super(id, paymentMethodName);
    }

    @Override
    public bisq.account.protobuf.AccountPayload.Builder getBuilder(boolean serializeForHash) {
        return getAccountPayloadBuilder(serializeForHash)
                .setBsqSwapAccountPayload(toBsqSwapAccountPayloadProto(serializeForHash));
    }

    public static BsqSwapAccountPayload fromProto(bisq.account.protobuf.AccountPayload proto) {
        return new BsqSwapAccountPayload(
                proto.getId(),
                proto.getPaymentMethodName()
        );
    }

    private bisq.account.protobuf.BsqSwapAccountPayload toBsqSwapAccountPayloadProto(boolean serializeForHash) {
        return resolveBuilder(getBsqSwapAccountPayloadBuilder(serializeForHash), serializeForHash).build();
    }

    private bisq.account.protobuf.BsqSwapAccountPayload.Builder getBsqSwapAccountPayloadBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.BsqSwapAccountPayload.newBuilder();
    }
}