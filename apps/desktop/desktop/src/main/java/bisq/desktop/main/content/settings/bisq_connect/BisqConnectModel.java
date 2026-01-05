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

package bisq.desktop.main.content.settings.bisq_connect;

import bisq.common.network.Address;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.RequiredFieldValidator;
import bisq.desktop.components.controls.validator.TextMinLengthValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.http_api.web_socket.BisqConnectClientInfo;
import bisq.i18n.Res;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Getter
public class BisqConnectModel implements Model {
    private final BooleanProperty enabled = new SimpleBooleanProperty();
    private final ObjectProperty<BisqConnectExposureMode> selectedMode = new SimpleObjectProperty<>(BisqConnectExposureMode.TOR);
    private final IntegerProperty port = new SimpleIntegerProperty();

    private final StringProperty apiUrl = new SimpleStringProperty("");
    private final BooleanProperty isChangeDetected = new SimpleBooleanProperty();
    private final StringProperty password = new SimpleStringProperty("");
    private final BooleanProperty passwordIsValid = new SimpleBooleanProperty();
    private final ValidatorBase passwordRequiredValidator = new RequiredFieldValidator(Res.get("validation.empty"));
    private final ValidatorBase passwordMinimumLengthValidator = new TextMinLengthValidator(Res.get("validation.password.tooShort"), 8);

    private final ObjectProperty<Image> qrCodeImage = new SimpleObjectProperty<>();
    private final StringProperty qrPlaceholder = new SimpleStringProperty("");
    private final SimpleIntegerProperty qrCodeSize;
    private final BooleanProperty websocketRunning = new SimpleBooleanProperty(false);
    private final StringProperty websocketInitError = new SimpleStringProperty("");
    private final ObjectProperty<Optional<Address>> websocketAddress = new SimpleObjectProperty<>(Optional.empty());
    private final ObservableList<BisqConnectClientInfo> connectedClients = FXCollections.observableArrayList();
    private final ObservableList<BisqConnectExposureMode> exposureModes = FXCollections.observableArrayList(BisqConnectExposureMode.values());

    public BisqConnectModel(int qrCodeSize) {
        this.qrCodeSize = new SimpleIntegerProperty(qrCodeSize);
    }
}
