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

package bisq.presentation.notifications;

import bisq.common.proto.Proto;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class DateAndConsumedFlag implements Proto {
    private final long date;
    @Setter
    private boolean consumed;

    public DateAndConsumedFlag(long date, boolean consumed) {
        this.date = date;
        this.consumed = consumed;
    }

    public bisq.presentation.protobuf.DateAndConsumedFlag toProto() {
        return bisq.presentation.protobuf.DateAndConsumedFlag.newBuilder()
                .setDate(date)
                .setConsumed(consumed)
                .build();
    }

    public static DateAndConsumedFlag fromProto(bisq.presentation.protobuf.DateAndConsumedFlag proto) {
        return new DateAndConsumedFlag(proto.getDate(),
                proto.getConsumed());
    }
}
