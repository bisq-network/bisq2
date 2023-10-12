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

package bisq.desktop.main.content.bisq_easy.wallet_guide.create_wallet;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WalletGuideCreateWalletView extends View<HBox, WalletGuideCreateWalletModel, WalletGuideCreateWalletController> {
    private final Button backButton, nextButton;

    public WalletGuideCreateWalletView(WalletGuideCreateWalletModel model, WalletGuideCreateWalletController controller) {
        super(new HBox(20), model, controller);

        VBox vBox = new VBox(20);
        vBox.setFillWidth(true);
        vBox.setAlignment(Pos.TOP_LEFT);


        Label headline = new Label(Res.get("bisqEasy.walletGuide.createWallet.headline"));
        headline.getStyleClass().add("bisq-easy-trade-guide-headline");

        Text text = new Text(Res.get("bisqEasy.walletGuide.createWallet.content"));
        text.getStyleClass().add("bisq-easy-trade-guide-content");
        TextFlow content = new TextFlow(text);

        backButton = new Button(Res.get("action.back"));

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        HBox buttons = new HBox(20, backButton, nextButton);

        VBox.setMargin(headline, new Insets(0, 0, -5, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        vBox.getChildren().addAll(headline, content, buttons);

        ImageView image = ImageUtil.getImageViewById("blue-wallet-create");
        root.getChildren().addAll(vBox, image);

    }

    @Override
    protected void onViewAttached() {
        backButton.setOnAction(e -> controller.onBack());
        nextButton.setOnAction(e -> controller.onNext());
    }

    @Override
    protected void onViewDetached() {
        backButton.setOnAction(null);
        nextButton.setOnAction(null);
    }
}
