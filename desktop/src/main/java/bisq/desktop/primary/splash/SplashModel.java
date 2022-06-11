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

package bisq.desktop.primary.splash;

import bisq.application.DefaultApplicationService;
import bisq.desktop.common.view.Model;
import bisq.network.p2p.ServiceNode;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;

@Getter
public class SplashModel implements Model {
    private final ObjectProperty<DefaultApplicationService.State> applicationState = new SimpleObjectProperty<>();
    private final ObjectProperty<ServiceNode.State> clearServiceNodeState = new SimpleObjectProperty<>();
    private final ObjectProperty<ServiceNode.State> torServiceNodeState = new SimpleObjectProperty<>();
    private final ObjectProperty<ServiceNode.State> i2pServiceNodeState = new SimpleObjectProperty<>();
    private  final DoubleProperty progress = new SimpleDoubleProperty(-1);
}