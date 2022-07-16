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

package bisq.desktop.primary.main.content.settings.reputation.earnReputation.bond;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.primary.main.content.settings.reputation.burn.ReputationSourceListItem;
import bisq.i18n.Res;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BsqBondView extends View<VBox, BsqBondModel, BsqBondController> {
    private final ComboBox<ReputationSourceListItem> reputationSourcesComboBox;
    private final ChangeListener<ReputationSourceListItem> reputationSourceListener;
    private Subscription selectedSourcePin;

    public BsqBondView(BsqBondModel model, BsqBondController controller, Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.CENTER_LEFT);

        Label headLine = new Label(Res.get("reputation.burnedBsq.howToHeadline"));
        headLine.getStyleClass().add("bisq-text-15");

        Label info = new Label(Res.get("reputation.burnedBsq.info"));
        info.getStyleClass().add("bisq-text-13");

        Label userProfileSelectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);

        reputationSourcesComboBox = new ComboBox<>(model.getSources());


        Label pubKeyHashLabel = new Label(Res.get("reputation.pubKeyHash"));
        pubKeyHashLabel.getStyleClass().addAll("bisq-text-3");

        MaterialTextField txIdInputBox = new MaterialTextField(Res.get("reputation.txId"), Res.get("reputation.txId.prompt"));

        VBox.setMargin(headLine, new Insets(25, 0, 0, 0));
        VBox.setMargin(userProfileSelectLabel, new Insets(30, 0, 0, 30));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, 0, 30));
      /*  root.getChildren().addAll(headLine, info,
                userProfileSelectLabel, userProfileSelection,
                reputationSourcesComboBox,
                pubKeyHashLabel,
                txIdInputBox
        );*/
        reputationSourceListener = (observable, oldValue, newValue) -> controller.onSelectSource(newValue);
    }

    @Override
    protected void onViewAttached() {
        reputationSourcesComboBox.getSelectionModel().selectedItemProperty().addListener(reputationSourceListener);
        selectedSourcePin = EasyBind.subscribe(model.getSelectedSource(), selectedSource -> {
            if (selectedSource != null) {

            }
        });
    }

    @Override
    protected void onViewDetached() {
        reputationSourcesComboBox.getSelectionModel().selectedItemProperty().removeListener(reputationSourceListener);
        selectedSourcePin.unsubscribe();
    }

}
