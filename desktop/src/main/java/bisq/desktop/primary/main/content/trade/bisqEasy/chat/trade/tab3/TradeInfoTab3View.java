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

package bisq.desktop.primary.main.content.trade.bisqEasy.chat.trade.tab3;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import lombok.extern.slf4j.Slf4j;
import org.fxmisc.easybind.EasyBind;
import org.fxmisc.easybind.Subscription;

@Slf4j
public class TradeInfoTab3View extends View<VBox, TradeInfoTab3Model, TradeInfoTab3Controller> {
    private final Button backButton, collapseButton;
    private final Hyperlink learnMore;
    private final Text content;
    private final CheckBox confirm;
    private Subscription widthPin;

    public TradeInfoTab3View(TradeInfoTab3Model model,
                             TradeInfoTab3Controller controller) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("tradeInfo.tab3.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        content = new Text(Res.get("tradeInfo.tab3.content"));
        content.getStyleClass().addAll("bisq-text-13", "bisq-line-spacing-01");

        learnMore = new Hyperlink(Res.get("user.reputation.learnMore"));
        confirm = new CheckBox(Res.get("tradeInfo.tab3.confirm"));

        backButton = new Button(Res.get("back"));
        collapseButton = new Button(Res.get("tradeInfo.tab3.collapse"));
        HBox buttons = new HBox(20, backButton, collapseButton);

        VBox.setVgrow(content, Priority.ALWAYS);
        VBox.setMargin(headline, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headline, content, learnMore, confirm, buttons);
    }

    @Override
    protected void onViewAttached() {
        confirm.setManaged(!model.getTradeRulesConfirmed().get());
        confirm.setVisible(!model.getTradeRulesConfirmed().get());

        collapseButton.managedProperty().bind(model.getTradeRulesConfirmed());
        collapseButton.visibleProperty().bind(model.getTradeRulesConfirmed());

        confirm.setOnAction(e -> controller.onConfirm(confirm.isSelected()));
        backButton.setOnAction(e -> controller.onBack());
        collapseButton.setOnAction(e -> controller.onCollapse());
        learnMore.setOnAction(e -> controller.onLearnMore());

        widthPin = EasyBind.subscribe(root.widthProperty(),
                w -> content.setWrappingWidth(w.doubleValue() - 30));
    }

    @Override
    protected void onViewDetached() {
        collapseButton.managedProperty().unbind();
        collapseButton.visibleProperty().unbind();

        confirm.setOnAction(null);
        backButton.setOnAction(null);
        collapseButton.setOnAction(null);
        learnMore.setOnAction(null);

        widthPin.unsubscribe();
    }
}
