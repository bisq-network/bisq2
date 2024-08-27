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

package bisq.network.p2p.services.data;

import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.broadcast.BroadcastMessage;
import bisq.network.p2p.services.data.storage.append.AddAppendOnlyDataRequest;
import bisq.network.p2p.services.data.storage.auth.AddAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RefreshAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.auth.RemoveAuthenticatedDataRequest;
import bisq.network.p2p.services.data.storage.mailbox.AddMailboxRequest;
import bisq.network.p2p.services.data.storage.mailbox.RemoveMailboxRequest;

public interface DataRequest extends BroadcastMessage {
    default bisq.network.protobuf.DataRequest.Builder newDataRequestBuilder() {
        return bisq.network.protobuf.DataRequest.newBuilder();
    }

    @Override
    default bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean serializeForHash) {
        return newEnvelopePayloadMessageBuilder().setDataRequest(toDataRequestProto(serializeForHash));
    }

    default bisq.network.protobuf.DataRequest toDataRequestProto(boolean serializeForHash) {
        return resolveBuilder(getDataRequestBuilder(serializeForHash), serializeForHash).build();
    }

    bisq.network.protobuf.DataRequest.Builder getDataRequestBuilder(boolean serializeForHash);

    static DataRequest fromProto(bisq.network.protobuf.DataRequest proto) {
        return switch (proto.getMessageCase()) {
            case ADDAUTHENTICATEDDATAREQUEST ->
                    AddAuthenticatedDataRequest.fromProto(proto.getAddAuthenticatedDataRequest());
            case REMOVEAUTHENTICATEDDATAREQUEST ->
                    RemoveAuthenticatedDataRequest.fromProto(proto.getRemoveAuthenticatedDataRequest());
            case REFRESHAUTHENTICATEDDATAREQUEST ->
                    RefreshAuthenticatedDataRequest.fromProto(proto.getRefreshAuthenticatedDataRequest());
            case ADDMAILBOXREQUEST -> AddMailboxRequest.fromProto(proto.getAddMailboxRequest());
            case REMOVEMAILBOXREQUEST -> RemoveMailboxRequest.fromProto(proto.getRemoveMailboxRequest());
            case ADDAPPENDONLYDATAREQUEST -> AddAppendOnlyDataRequest.fromProto(proto.getAddAppendOnlyDataRequest());
            case MESSAGE_NOT_SET -> throw new UnresolvableProtobufMessageException("MESSAGE_NOT_SET", proto);
        };
    }

    boolean isExpired();

    long getCreated();

    int getMaxMapSize();
}
