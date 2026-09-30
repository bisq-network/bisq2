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

package bisq.settings;

import bisq.common.proto.PersistableProto;

public record TacAcceptance(String version, long acceptedAtEpochMillis) implements PersistableProto {
    public static TacAcceptance current() {
        return new TacAcceptance(TacVersion.CURRENT, System.currentTimeMillis());
    }

    public boolean isCurrent() {
        return TacVersion.CURRENT.equals(version);
    }

    @Override
    public bisq.settings.protobuf.TacAcceptance.Builder getBuilder(boolean serializeForHash) {
        return bisq.settings.protobuf.TacAcceptance.newBuilder()
                .setVersion(version)
                .setAcceptedAtEpochMillis(acceptedAtEpochMillis);
    }

    @Override
    public bisq.settings.protobuf.TacAcceptance toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static TacAcceptance fromProto(bisq.settings.protobuf.TacAcceptance proto) {
        return new TacAcceptance(proto.getVersion(), proto.getAcceptedAtEpochMillis());
    }
}
