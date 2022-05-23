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

import bisq.common.proto.ProtoEnum;
import bisq.common.util.ProtobufUtils;

// Used for persistence of Cookie. Entries must not be changes or removed. Only adding entries is permitted.
public enum CookieKey implements ProtoEnum {
    STAGE_X,
    STAGE_Y,
    STAGE_W,
    STAGE_H,
    NAVIGATION_TARGET,
    BISQ_EASY_ONBOARDED;

    public bisq.settings.protobuf.CookieKey toProto() {
        return bisq.settings.protobuf.CookieKey.valueOf(name());
    }

    public static CookieKey fromProto(bisq.settings.protobuf.CookieKey proto) {
        return ProtobufUtils.enumFromProto(CookieKey.class, proto.name());
    }
}
