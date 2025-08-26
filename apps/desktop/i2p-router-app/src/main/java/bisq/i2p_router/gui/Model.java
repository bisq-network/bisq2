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

package bisq.i2p_router.gui;

import bisq.network.i2p.router.state.RouterState;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Model {
    private final double width;
    private final double height;
    private final String version;
    private final int maxExpectedStartupTime = 180;

    @Setter
    private long startupDuration;

    private final StringProperty headline = new SimpleStringProperty();
    private final ObjectProperty<RouterState> routerState = new SimpleObjectProperty<>();
    private final StringProperty routerStateString = new SimpleStringProperty();
    private final StringProperty startupDurationString = new SimpleStringProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty();
    private final StringProperty routerStateDetails = new SimpleStringProperty();
    private final StringProperty routerSetupDetails = new SimpleStringProperty();
    private final StringProperty dateDirPath = new SimpleStringProperty();

    private final StringProperty buttonLabel = new SimpleStringProperty();
    private final BooleanProperty buttonDisabled = new SimpleBooleanProperty();

    private final BooleanProperty startupExpectedTimePassed = new SimpleBooleanProperty();

    private final StringProperty errorHeadline = new SimpleStringProperty();
    private final StringProperty errorMessage = new SimpleStringProperty();
    private final BooleanProperty errorBoxVisible = new SimpleBooleanProperty();

    public Model(double width, double height, String version) {
        this.width = width;
        this.height = height;
        this.version = version;
    }
}
