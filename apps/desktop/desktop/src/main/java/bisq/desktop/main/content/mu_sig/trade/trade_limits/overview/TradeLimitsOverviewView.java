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

package bisq.desktop.main.content.mu_sig.trade.trade_limits.overview;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.BisqHyperlink;
import bisq.desktop.components.controls.UnorderedList;
import bisq.desktop.main.content.mu_sig.trade.trade_limits.TradeLimitsViewUtils;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TradeLimitsOverviewView extends View<VBox, TradeLimitsOverviewModel, TradeLimitsOverviewController> {
    private final Button nextButton;
    private final Hyperlink learnMore;

    public TradeLimitsOverviewView(TradeLimitsOverviewModel model,
                                   TradeLimitsOverviewController controller) {
        super(new VBox(), model, controller);

        Label headline = TradeLimitsViewUtils.getHeadline(Res.get("muSig.trade.limits.overview.headline"));
        Label info = TradeLimitsViewUtils.getInfo(Res.get("muSig.trade.limits.overview.info"));

        Label fiatBuyerSubHeadline = TradeLimitsViewUtils.getSubHeadline(Res.get("muSig.trade.limits.overview.subHeadline.fiat.buyer"));
        UnorderedList fiatBuyerInfo = TradeLimitsViewUtils.getUnorderedList(Res.get("muSig.trade.limits.overview.info.fiat.buyer"));

        Label fiatSellerSubHeadline = TradeLimitsViewUtils.getSubHeadline(Res.get("muSig.trade.limits.overview.subHeadline.fiat.seller"));
        UnorderedList fiatSellerInfo = TradeLimitsViewUtils.getUnorderedList(Res.get("muSig.trade.limits.overview.info.fiat.seller"));

        Label cryptoSellerSubHeadline = TradeLimitsViewUtils.getSubHeadline(Res.get("muSig.trade.limits.overview.subHeadline.crypto"));
        UnorderedList cryptoInfo = TradeLimitsViewUtils.getUnorderedList(Res.get("muSig.trade.limits.overview.info.crypto"));

        nextButton = new Button(Res.get("action.next"));
        nextButton.setDefaultButton(true);

        learnMore = new BisqHyperlink(Res.get("action.learnMore"), "https://bisq.wiki/MuSig-tradelimits");

        HBox buttons = new HBox(20, nextButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setMargin(headline, new Insets(10, 0, 10, 0));
        VBox.setMargin(buttons, new Insets(25, 0, 0, 0));
        VBox contentBox = new VBox(5,
                headline,
                info,
                fiatBuyerSubHeadline, fiatBuyerInfo,
                fiatSellerSubHeadline, fiatSellerInfo,
                cryptoSellerSubHeadline, cryptoInfo,
                buttons);
        contentBox.getStyleClass().addAll("bisq-common-bg", "common-line-spacing");
        root.getChildren().addAll(contentBox);

        root.setPadding(new Insets(20, 0, 0, 0));
    }


    @Override
    protected void onViewAttached() {
        nextButton.setOnAction(e -> controller.onNext());
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        nextButton.setOnAction(null);
        learnMore.setOnAction(null);
    }
}
