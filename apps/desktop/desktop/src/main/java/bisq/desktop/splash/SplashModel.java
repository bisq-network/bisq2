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

package bisq.desktop.splash;

import bisq.common.util.Version;
import bisq.desktop.common.view.Model;
import bisq.desktop.splash.temp.BootstrapStateDisplay;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SplashModel implements Model {
    private final StringProperty applicationServiceState = new SimpleStringProperty();
    private final String version;
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final List<BootstrapStateDisplay> bootstrapStateDisplays = new ArrayList<>();

    public SplashModel(Version version) {
        this.version = "v" + version.getVersionAsString();
    }
}
