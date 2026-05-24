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

package bisq.desktop.main.content.user.accounts.stable_coin_accounts.create;

import bisq.account.accounts.stable_coin.StableCoinAccountPayload;
import bisq.account.payment_method.stable_coin.StableCoinPaymentRail;
import bisq.common.util.StringUtils;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StableCoinAccountDataController implements Controller {
    private final CreateStableCoinAccountModel parentModel;
    @Getter
    private final DataView view;

    StableCoinAccountDataController(CreateStableCoinAccountModel parentModel) {
        this.parentModel = parentModel;
        this.view = new DataView(parentModel, this);
    }

    @Override
    public void onActivate() {
        updateNextButtonState();
    }

    @Override
    public void onDeactivate() {
    }

    private void updateNextButtonState() {
        StableCoinPaymentRail rail = parentModel.getSelectedRail().get();
        String address = parentModel.getAddress().get();
        boolean valid = rail != null && StringUtils.isNotEmpty(address) && StableCoinAccountPayload.isValidEvmAddress(address.trim());
        parentModel.getNextButtonDisabled().set(!valid);
    }

    static class DataView extends View<VBox, CreateStableCoinAccountModel, StableCoinAccountDataController> {
        private final ComboBox<StableCoinPaymentRail> networkComboBox;
        private final MaterialTextField addressField;
        private final Label validationLabel;
        private ChangeListener<StableCoinPaymentRail> railListener;
        private ChangeListener<String> addressListener;

        DataView(CreateStableCoinAccountModel model, StableCoinAccountDataController controller) {
            super(new VBox(20), model, controller);

            root.setAlignment(Pos.TOP_LEFT);
            root.setPadding(new Insets(20, 0, 0, 0));

            Label headline = new Label(Res.get("user.stableCoinAccounts.create.headline"));
            headline.getStyleClass().add("large-thin-headline");

            Label networkLabel = new Label(Res.get("user.stableCoinAccounts.create.network"));
            networkLabel.getStyleClass().add("bisq-text-14");

            networkComboBox = new ComboBox<>(FXCollections.observableArrayList(StableCoinPaymentRail.values()));
            networkComboBox.setPrefWidth(400);
            networkComboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(StableCoinPaymentRail rail) {
                    if (rail == null) return "";
                    return Res.get("user.stableCoinAccounts.create.networkComboFormat",
                            rail.getStableCoin().getCode(), rail.getStableCoin().getNetwork().getDisplayName());
                }

                @Override
                public StableCoinPaymentRail fromString(String string) {
                    return null;
                }
            });

            addressField = new MaterialTextField(
                    Res.get("user.stableCoinAccounts.create.address"),
                    Res.get("user.stableCoinAccounts.create.address.prompt"));
            addressField.setPrefWidth(400);

            validationLabel = new Label();
            validationLabel.getStyleClass().add("bisq-text-14");
            validationLabel.setStyle("-fx-text-fill: -bisq-red;");
            validationLabel.setVisible(false);
            validationLabel.setManaged(false);

            root.getChildren().addAll(headline, networkLabel, networkComboBox, addressField, validationLabel);
        }

        @Override
        protected void onViewAttached() {
            if (model.getSelectedRail().get() != null) {
                networkComboBox.getSelectionModel().select(model.getSelectedRail().get());
            }
            addressField.setText(model.getAddress().get());

            railListener = (obs, old, newVal) -> {
                model.getSelectedRail().set(newVal);
                if (controller != null) controller.updateNextButtonState();
            };
            networkComboBox.valueProperty().addListener(railListener);

            addressListener = (obs, old, newVal) -> {
                model.getAddress().set(newVal);
                boolean showError = StringUtils.isNotEmpty(newVal) && !StableCoinAccountPayload.isValidEvmAddress(newVal.trim());
                validationLabel.setText(showError ? Res.get("user.stableCoinAccounts.create.address.invalidEvm") : "");
                validationLabel.setVisible(showError);
                validationLabel.setManaged(showError);
                if (controller != null) controller.updateNextButtonState();
            };
            addressField.textProperty().addListener(addressListener);
        }

        @Override
        protected void onViewDetached() {
            networkComboBox.valueProperty().removeListener(railListener);
            addressField.textProperty().removeListener(addressListener);
        }
    }
}
