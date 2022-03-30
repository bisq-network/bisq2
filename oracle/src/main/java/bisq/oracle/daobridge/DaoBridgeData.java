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

package bisq.oracle.daobridge;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

/**
 * Preparation for DAO related data which are distributed as authorized data as long the DAO is not integrated.
 * Atm it's just a dummy field.
 */
@Getter
@ToString
@EqualsAndHashCode
public class DaoBridgeData implements DistributedData {
    private final String txId;
    private final MetaData metaData = new MetaData(TimeUnit.MINUTES.toMillis(5), 100000, DaoBridgeData.class.getSimpleName());

    public DaoBridgeData(String txId) {
        this.txId = txId;
    }

    public bisq.oracle.protobuf.DaoBridgeData toProto() {
        return bisq.oracle.protobuf.DaoBridgeData.newBuilder().setTxId(txId).build();
    }

    public static DaoBridgeData fromProto(bisq.oracle.protobuf.DaoBridgeData baseProto) {
        return new DaoBridgeData(baseProto.getTxId());
    }

    public static ProtoResolver<DistributedData> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.oracle.protobuf.DaoBridgeData.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return metaData;
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}