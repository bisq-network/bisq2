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

package bisq.desktop.main.content.bisq_easy.trade_guide.rules;

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.UnorderedList;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class BisqEasyGuideRulesView extends View<VBox, BisqEasyGuideRulesModel, BisqEasyGuideRulesController> {
    private final Button backButton, closeButton;
    private final Hyperlink learnMore;
    private final CheckBox confirmCheckBox;
    private Subscription tradeRulesConfirmedPin;

    public BisqEasyGuideRulesView(BisqEasyGuideRulesModel model, BisqEasyGuideRulesController controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("bisqEasy.tradeGuide.rules.headline"));
        headline.getStyleClass().add("bisq-easy-trade-guide-headline");

        UnorderedList content = new UnorderedList(Res.get("bisqEasy.tradeGuide.rules.content"), "bisq-easy-trade-guide-content");

        learnMore = new Hyperlink(Res.get("action.learnMore"));

        backButton = new Button(Res.get("action.back"));
        closeButton = new Button(Res.get("action.close"));
        confirmCheckBox = new CheckBox(Res.get("bisqEasy.tradeGuide.rules.confirm"));

        HBox buttons = new HBox(20, backButton, closeButton);
        VBox.setMargin(headline, new Insets(10, 0, -5, 0));
        VBox.setMargin(confirmCheckBox, new Insets(0, 0, 10, 0));
        root.getChildren().addAll(headline, content, learnMore, confirmCheckBox, buttons);
    }

    @Override
    protected void onViewAttached() {
        confirmCheckBox.setSelected(model.getTradeRulesConfirmed().get());

        confirmCheckBox.setVisible(!model.getTradeRulesConfirmed().get());
        confirmCheckBox.setManaged(!model.getTradeRulesConfirmed().get());

        tradeRulesConfirmedPin = EasyBind.subscribe(model.getTradeRulesConfirmed(), tradeRulesConfirmed -> {
            closeButton.setDefaultButton(tradeRulesConfirmed);
            if (tradeRulesConfirmed) {
                closeButton.getStyleClass().remove("outlined-button");
            } else {
                closeButton.getStyleClass().add("outlined-button");
            }
        });

        confirmCheckBox.setOnAction(e -> controller.onConfirm(confirmCheckBox.isSelected()));
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        tradeRulesConfirmedPin.unsubscribe();

        confirmCheckBox.setOnAction(null);
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
