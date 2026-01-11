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

import bisq.desktop.common.view.Model;
import bisq.api.web_socket.WebsocketClient1;
import bisq.api.web_socket.WebSocketService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class BisqConnectModel implements Model {
    private final String webSocketServerUrl;
    private final int qrCodeSize;

    private final ObjectProperty<WebSocketService.State> webSocketServiceState = new SimpleObjectProperty<>(WebSocketService.State.NEW);
    private final BooleanProperty isPairingVisible = new SimpleBooleanProperty();
    private final ObjectProperty<Image> qrCodeImage = new SimpleObjectProperty<>();
    private final StringProperty qrCode = new SimpleStringProperty();

    private final ObservableList<WebsocketClient1> connectedClients = FXCollections.observableArrayList();

    BisqConnectModel(String webSocketServerUrl, int qrCodeSize) {
        this.webSocketServerUrl = webSocketServerUrl;
        this.qrCodeSize = qrCodeSize;
    }
}
