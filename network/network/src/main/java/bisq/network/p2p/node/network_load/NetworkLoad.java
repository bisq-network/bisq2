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

package bisq.network.p2p.node.network_load;

import bisq.common.proto.NetworkProto;
import bisq.common.util.MathUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public final class NetworkLoad implements NetworkProto {
    public final static double INITIAL_LOAD = 0.1;
    public final static double DEFAULT_DIFFICULTY_ADJUSTMENT = 1;
    public final static double MAX_DIFFICULTY_ADJUSTMENT = 160000; // 1048576/65536/0.01/0.01=160000
    public final static NetworkLoad INITIAL_NETWORK_LOAD = new NetworkLoad();

    private final double load;
    private final double difficultyAdjustmentFactor;

    public NetworkLoad() {
        this(INITIAL_LOAD, DEFAULT_DIFFICULTY_ADJUSTMENT);
    }

    /**
     * @param load                       The load of the users network node
     * @param difficultyAdjustmentFactor The factor to adjust the PoW difficulty. It allows to go higher than the
     *                                   TARGET_DIFFICULTY(2^16) up to the MAX_DIFFICULTY(2^20).
     *                                   The max. value which would result in the MAX_DIFFICULTY independent of messageCost
     *                                   and load would be 1048576/65536/0.01/0.01=160000 (min values for messageCost
     *                                   and load are 0.01).
     *                                   Min. value is 0 which would result in MIN_DIFFICULTY (2^7)
     */
    public NetworkLoad(double load, double difficultyAdjustmentFactor) {
        this.load = MathUtils.bounded(0, 1, load);
        this.difficultyAdjustmentFactor = MathUtils.bounded(0, MAX_DIFFICULTY_ADJUSTMENT, difficultyAdjustmentFactor);

        verify();
    }

    @Override
    public void verify() {
    }

    @Override
    public bisq.network.protobuf.NetworkLoad toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }

    @Override
    public bisq.network.protobuf.NetworkLoad.Builder getBuilder(boolean serializeForHash) {
        return bisq.network.protobuf.NetworkLoad.newBuilder()
                .setLoad(load)
                .setDifficultyAdjustmentFactor(difficultyAdjustmentFactor);
    }

    public static NetworkLoad fromProto(bisq.network.protobuf.NetworkLoad proto) {
        double difficultyAdjustmentFactor = proto.getDifficultyAdjustmentFactor();
        return new NetworkLoad(proto.getLoad(), difficultyAdjustmentFactor);
    }
}