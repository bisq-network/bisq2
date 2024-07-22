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

package bisq.bonded_roles.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.common.AddressByTransportTypeMap;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class BondedRoleRegistrationRequest implements MailboxMessage, ExternalNetworkMessage {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    private final String profileId;
    private final String authorizedPublicKey;
    private final BondedRoleType bondedRoleType;
    private final String bondUserName;
    private final String signatureBase64;
    private final NetworkId networkId;
    private final boolean isCancellationRequest;
    private final Optional<AddressByTransportTypeMap> addressByTransportTypeMap;

    public BondedRoleRegistrationRequest(String profileId,
                                         String authorizedPublicKey,
                                         BondedRoleType bondedRoleType,
                                         String bondUserName,
                                         String signatureBase64,
                                         Optional<AddressByTransportTypeMap> addressByTransportTypeMap,
                                         NetworkId networkId,
                                         boolean isCancellationRequest) {
        this.profileId = profileId;
        this.authorizedPublicKey = authorizedPublicKey;
        this.bondedRoleType = bondedRoleType;
        this.bondUserName = bondUserName;
        this.signatureBase64 = signatureBase64;
        this.addressByTransportTypeMap = addressByTransportTypeMap;
        this.networkId = networkId;
        this.isCancellationRequest = isCancellationRequest;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validatePubKeyHex(authorizedPublicKey);
        NetworkDataValidation.validateBondUserName(bondUserName);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
    }

    @Override
    public bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest.Builder getValueBuilder(boolean serializeForHash) {
        var builder = bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest.newBuilder()
                .setProfileId(profileId)
                .setAuthorizedPublicKey(authorizedPublicKey)
                .setBondedRoleType(bondedRoleType.toProtoEnum())
                .setBondUserName(bondUserName)
                .setSignatureBase64(signatureBase64)
                .setNetworkId(networkId.toProto(serializeForHash))
                .setIsCancellationRequest(isCancellationRequest);
        addressByTransportTypeMap.ifPresent(e -> builder.setAddressByTransportTypeMap(e.toProto(serializeForHash)));
        return builder;
    }

    public static BondedRoleRegistrationRequest fromProto(bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest proto) {
        return new BondedRoleRegistrationRequest(proto.getProfileId(),
                proto.getAuthorizedPublicKey(),
                BondedRoleType.fromProto(proto.getBondedRoleType()),
                proto.getBondUserName(),
                proto.getSignatureBase64(),
                proto.hasAddressByTransportTypeMap() ?
                        Optional.of(AddressByTransportTypeMap.fromProto(proto.getAddressByTransportTypeMap())) :
                        Optional.empty(),
                NetworkId.fromProto(proto.getNetworkId()),
                proto.getIsCancellationRequest());
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest proto = any.unpack(bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest.class);
                return BondedRoleRegistrationRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.5, 1);
    }
}