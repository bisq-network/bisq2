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

package bisq.desktop.main.content.settings.bisq_connect.api_config;

import bisq.common.network.Address;
import bisq.desktop.common.converters.LongStringConverter;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.HostValidator;
import bisq.desktop.components.controls.validator.PortValidator;
import bisq.api.ApiConfig;
import bisq.api.access.transport.ApiAccessTransportType;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j

@Getter
public class ApiConfigModel implements Model {
    private final ApiConfig apiConfig;

    // --- accessTransportType ---
    private final ObjectProperty<ApiAccessTransportType> apiAccessTransportType = new SimpleObjectProperty<>();

    // --- server ---
    private final BooleanProperty restEnabled = new SimpleBooleanProperty();
    private final BooleanProperty websocketEnabled = new SimpleBooleanProperty();

    // --- bind ---
    private final StringProperty bindHost = new SimpleStringProperty();
    private final IntegerProperty bindPort = new SimpleIntegerProperty();

    // --- security ---
    private final BooleanProperty tlsRequired = new SimpleBooleanProperty();
    private final BooleanProperty torClientAuthRequired = new SimpleBooleanProperty();

    // --- security.rest ---
    private final ObjectProperty<Optional<List<String>>> restAllowEndpoints = new SimpleObjectProperty<>(Optional.empty());
    private final ListProperty<String> restDenyEndpoints = new SimpleListProperty<>(FXCollections.observableArrayList());

    // --- security.websocket ---
    private final ObjectProperty<Optional<List<String>>> websocketAllowEndpoints = new SimpleObjectProperty<>(Optional.empty());
    private final ListProperty<String> websocketDenyEndpoints = new SimpleListProperty<>(FXCollections.observableArrayList());


    // --- UI specific ---
    private final ObservableList<ApiAccessTransportType> apiTransportTypes = FXCollections.observableArrayList();
    private final StringProperty protocol = new SimpleStringProperty();
    private final StringProperty serverUrl = new SimpleStringProperty();
    private final ObjectProperty<Address> onionServiceAddress = new SimpleObjectProperty<>();
    private final StringProperty onionServiceUrl = new SimpleStringProperty();
    private final StringProperty onionServiceUrlPrompt = new SimpleStringProperty();
    private final StringProperty onionServiceUrlHelp = new SimpleStringProperty();
    private final StringProperty detectedLanHost = new SimpleStringProperty();
    private final BooleanProperty detectedLanHostApplied = new SimpleBooleanProperty();
    private final BooleanProperty applyButtonDisabled = new SimpleBooleanProperty();

    // Converters
    private final LongStringConverter bindPortConverter = new LongStringConverter(bindPort.get(), false);

    // validators
    private final PortValidator bindPortValidator = new PortValidator();
    private final HostValidator bindHostValidator = new HostValidator();

    // --- constructor from ApiConfig ---
    public ApiConfigModel(ApiConfig apiConfig) {
        this.apiConfig = apiConfig;
        this.apiAccessTransportType.set(apiConfig.getApiAccessTransportType());
        this.restEnabled.set(apiConfig.isRestEnabled());
        this.websocketEnabled.set(apiConfig.isWebsocketEnabled());
        this.bindHost.set(apiConfig.getBindHost());
        this.bindPort.set(apiConfig.getBindPort());
        this.tlsRequired.set(apiConfig.isTlsRequired());
        this.torClientAuthRequired.set(apiConfig.isTorClientAuthRequired());
        this.restAllowEndpoints.set(apiConfig.getRestAllowEndpoints().map(ArrayList::new));
        this.restDenyEndpoints.set(FXCollections.observableArrayList(apiConfig.getRestDenyEndpoints()));
        this.websocketAllowEndpoints.set(apiConfig.getWebsocketAllowEndpoints().map(ArrayList::new));
        this.websocketDenyEndpoints.set(FXCollections.observableArrayList(apiConfig.getWebsocketDenyEndpoints()));
    }
}