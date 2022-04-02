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

package bisq.desktop.primary.main.content.social.onboarding.selectUserType;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqButton;
import bisq.desktop.layout.Layout;
import bisq.i18n.Res;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;

public class SelectUserTypeView extends View<VBox, SelectUserTypeModel, SelectUserTypeController> {
    private final BisqButton newTrader, proTrader, skip;

    public SelectUserTypeView(SelectUserTypeModel model, SelectUserTypeController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(50);
        root.setAlignment(Pos.CENTER);

        Label headline = new Label(Res.get("satoshisquareapp.selectTraderType.headline"));
        headline.getStyleClass().add("headline-label");
        headline.setTextAlignment(TextAlignment.CENTER);

        newTrader = getButton(Res.get("satoshisquareapp.selectTraderType.newbie"));
        proTrader = getButton(Res.get("satoshisquareapp.selectTraderType.proTrader"));
        skip = new BisqButton(Res.get("satoshisquareapp.selectTraderType.skip"));
       
        HBox hBox = Layout.hBoxWith(newTrader, proTrader);
        hBox.setSpacing(25);
        hBox.setAlignment(Pos.CENTER);
        root.getChildren().addAll(Spacer.height(100), headline, hBox,  skip, Spacer.fillVBox());
    }

    private BisqButton getButton(String text) {
        BisqButton button = new BisqButton(text);
        button.setDefaultButton(true);
        button.setStyle("-fx-pref-height: 135px; -fx-pref-width: 325px; -fx-background-color: -fx-selection-bar");
        return button;
    }

    @Override
    protected void onViewAttached() {
        newTrader.setOnAction(e -> controller.onNewTrader());
        proTrader.setOnAction(e -> controller.onProTrader());
        skip.setOnAction(e -> controller.onSkip());
    }

    @Override
    protected void onViewDetached() {
    }
}
