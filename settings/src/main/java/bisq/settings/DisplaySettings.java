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

import bisq.common.proto.Proto;

public final class DisplaySettings implements Proto {
    private boolean useAnimations = true;

    public DisplaySettings() {
    }

    @Override
    public bisq.settings.protobuf.DisplaySettings toProto() {
        return bisq.settings.protobuf.DisplaySettings.newBuilder()
                .setUseAnimations(useAnimations)
                .build();
    }

    static DisplaySettings fromProto(bisq.settings.protobuf.DisplaySettings proto) {
        DisplaySettings displaySettings = new DisplaySettings();
        displaySettings.setUseAnimations(proto.getUseAnimations());
        return displaySettings;
    }

    public boolean isUseAnimations() {
        return useAnimations;
    }

    void setUseAnimations(boolean useAnimations) {
        this.useAnimations = useAnimations;
    }
}