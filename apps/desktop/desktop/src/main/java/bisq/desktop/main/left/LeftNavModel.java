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

package bisq.desktop.main.left;

import bisq.bisq_easy.NavigationTarget;
import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@Getter
public class LeftNavModel implements Model {
    private final boolean isWalletEnabled;

    @Setter
    private String version;

    private final Set<NavigationTarget> navigationTargets = new HashSet<>();
    private final List<LeftNavButton> leftNavButtons = new ArrayList<>();
    private final ObjectProperty<NavigationTarget> selectedNavigationTarget = new SimpleObjectProperty<>();
    private final ObjectProperty<LeftNavButton> selectedNavigationButton = new SimpleObjectProperty<>();
    private final BooleanProperty menuHorizontalExpanded = new SimpleBooleanProperty();
    private final BooleanProperty authorizedRoleVisible = new SimpleBooleanProperty(false);
    private final BooleanProperty isNewReleaseAvailable = new SimpleBooleanProperty(false);

    public LeftNavModel(boolean isWalletEnabled) {
        this.isWalletEnabled = isWalletEnabled;
    }
}
