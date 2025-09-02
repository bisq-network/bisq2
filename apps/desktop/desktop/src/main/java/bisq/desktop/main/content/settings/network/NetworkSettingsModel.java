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

import bisq.common.network.Address;
import bisq.desktop.common.converters.AddressStringConverter;
import bisq.desktop.common.converters.DoubleStringConverter;
import bisq.desktop.common.view.Model;
import bisq.desktop.components.controls.validator.NetworkAddressValidator;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.i2p.router.RouterSetup.DEFAULT_BI2P_GRPC_HOST;
import static bisq.network.i2p.router.RouterSetup.DEFAULT_BI2P_GRPC_PORT;
import static bisq.network.i2p.router.RouterSetup.DEFAULT_I2CP_HOST;
import static bisq.network.i2p.router.RouterSetup.DEFAULT_I2CP_PORT;

@Slf4j
@Getter
class NetworkSettingsModel implements Model {
    final static Address DEFAULT_I2CP_ADDRESS = new Address(DEFAULT_I2CP_HOST, DEFAULT_I2CP_PORT);
    final static Address DEFAULT_BI2P_GRPC_ADDRESS = new Address(DEFAULT_BI2P_GRPC_HOST, DEFAULT_BI2P_GRPC_PORT);

    private final ObjectProperty<TransportOption> selectedTransportOption = new SimpleObjectProperty<>();
    private final BooleanProperty useEmbeddedI2PRouter = new SimpleBooleanProperty();
    private final ObjectProperty<Address> i2cpAddress = new SimpleObjectProperty<>();
    private final ObjectProperty<Address> bi2pGrpcAddress = new SimpleObjectProperty<>();
    private final BooleanProperty shutdownButtonVisible = new SimpleBooleanProperty();
    private final BooleanProperty clearOnlyVisible = new SimpleBooleanProperty();

    private final AddressStringConverter i2cpAddressConverter = new AddressStringConverter(DEFAULT_I2CP_ADDRESS);
    private final AddressStringConverter bi2pGrpcAddressConverter = new AddressStringConverter(DEFAULT_BI2P_GRPC_ADDRESS);

    private final ValidatorBase i2cpAddressValidator = new NetworkAddressValidator();
    private final ValidatorBase bi2pGrpcAddressValidator = new NetworkAddressValidator();

    private final DoubleProperty difficultyAdjustmentFactor = new SimpleDoubleProperty();
    private final BooleanProperty difficultyAdjustmentFactorEditable = new SimpleBooleanProperty();
    private final StringProperty difficultyAdjustmentFactorDescriptionText = new SimpleStringProperty();
    private final BooleanProperty ignoreDiffAdjustmentFromSecManager = new SimpleBooleanProperty();
    private final DoubleProperty totalMaxBackupSizeInMB = new SimpleDoubleProperty();

    private final DoubleStringConverter difficultyAdjustmentFactorConverter = new DoubleStringConverter(NetworkLoad.DEFAULT_DIFFICULTY_ADJUSTMENT);
    private final ValidatorBase difficultyAdjustmentFactorValidator =
            new NumberValidator(Res.get("settings.network.difficultyAdjustmentFactor.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    NetworkLoad.MIN_DIFFICULTY_ADJUSTMENT, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT);

    NetworkSettingsModel() {
    }
}
