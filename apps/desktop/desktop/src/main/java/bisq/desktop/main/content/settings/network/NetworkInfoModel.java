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

package bisq.desktop.main.content.settings.network;

import bisq.common.data.Pair;
import bisq.desktop.common.view.Model;
import bisq.i18n.Res;
import bisq.network.common.TransportType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
public class NetworkInfoModel implements Model {
    private final List<Pair<String, Double>> versionDistribution = new ArrayList<>();
    @Setter
    private String versionDistributionTooltip;
    private final BooleanProperty clearNetDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty torDisabled = new SimpleBooleanProperty(false);
    private final BooleanProperty i2pDisabled = new SimpleBooleanProperty(false);
    private final Set<TransportType> supportedTransportTypes;
    private final StringProperty myDefaultNodeAddress = new SimpleStringProperty(Res.get("data.na"));

    public NetworkInfoModel(Set<TransportType> supportedTransportTypes,
                            boolean clearNetDisabled,
                            boolean torDisabled,
                            boolean i2pDisabled) {
        this.supportedTransportTypes = supportedTransportTypes;
        this.clearNetDisabled.set(clearNetDisabled);
        this.torDisabled.set(torDisabled);
        this.i2pDisabled.set(i2pDisabled);
    }
}
