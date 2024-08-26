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

import bisq.desktop.common.converters.Converters;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class NetworkSettingsView extends View<VBox, NetworkSettingsModel, NetworkSettingsController> {
    private static final ValidatorBase DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR =
            new NumberValidator(Res.get("settings.network.difficultyAdjustmentFactor.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    0, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT);
    private static final double TEXT_FIELD_WIDTH = 500;

    private final Switch ignoreDiffAdjustFromSecManagerSwitch;
    private final MaterialTextField difficultyAdjustmentFactor;
    private Subscription ignoreDiffAdjustFromSecManagerSwitchPin;

    public NetworkSettingsView(NetworkSettingsModel model, NetworkSettingsController controller) {
        super(new VBox(50), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        Label networkHeadline = new Label(Res.get("settings.network.headline"));
        networkHeadline.getStyleClass().add("large-thin-headline");

        difficultyAdjustmentFactor = new MaterialTextField();
        difficultyAdjustmentFactor.setMaxWidth(TEXT_FIELD_WIDTH);
        difficultyAdjustmentFactor.setValidators(DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR);
        difficultyAdjustmentFactor.setStringConverter(Converters.DOUBLE_STRING_CONVERTER);
        ignoreDiffAdjustFromSecManagerSwitch = new Switch(Res.get("settings.network.difficultyAdjustmentFactor.ignoreValueFromSecManager"));

        VBox networkVBox = new VBox(10, difficultyAdjustmentFactor, ignoreDiffAdjustFromSecManagerSwitch);
        Insets insets = new Insets(0, 5, 0, 5);
        root.getChildren().addAll(networkHeadline, SettingsViewUtils.getLineAfterHeadline(root.getSpacing()), networkVBox);
    }

    @Override
    protected void onViewAttached() {
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().bindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());
        Bindings.bindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor(),
                Converters.DOUBLE_STRING_CONVERTER);
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().bind(model.getDifficultyAdjustmentFactorEditable());
        difficultyAdjustmentFactor.descriptionProperty().bind(model.getDifficultyAdjustmentFactorDescriptionText());
        ignoreDiffAdjustFromSecManagerSwitchPin = EasyBind.subscribe(
                ignoreDiffAdjustFromSecManagerSwitch.selectedProperty(), s -> difficultyAdjustmentFactor.validate());
    }

    @Override
    protected void onViewDetached() {
        ignoreDiffAdjustFromSecManagerSwitch.selectedProperty().unbindBidirectional(model.getIgnoreDiffAdjustmentFromSecManager());
        Bindings.unbindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor());
        difficultyAdjustmentFactor.getTextInputControl().editableProperty().unbind();
        difficultyAdjustmentFactor.descriptionProperty().unbind();
        ignoreDiffAdjustFromSecManagerSwitchPin.unsubscribe();
        difficultyAdjustmentFactor.resetValidation();
    }

}
