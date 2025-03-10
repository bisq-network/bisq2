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

import bisq.common.platform.Version;
import bisq.desktop.common.view.Model;
import javafx.beans.property.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SplashModel implements Model {
    private final StringProperty applicationServiceState = new SimpleStringProperty();
    private final String version;
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final StringProperty duration = new SimpleStringProperty();
    private final BooleanProperty isSlowStartup = new SimpleBooleanProperty();
    private final List<BootstrapElementsPerTransport> bootstrapElementsPerTransports = new ArrayList<>();
    private final int maxExpectedStartupTime = 120;

    public SplashModel(Version version) {
        this.version = "v" + version.getVersionAsString();
    }
}
