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

package bisq.desktop.main.content.trade_apps;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.UnorderedList;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolRoadmapView extends View<VBox, ProtocolRoadmapModel, ProtocolRoadmapController> {
    protected final Hyperlink learnMore;

    public ProtocolRoadmapView(ProtocolRoadmapModel model, ProtocolRoadmapController controller) {
        super(new VBox(10), model, controller);

        String name = model.getTradeProtocolType().name();
        Label headline = new Label(Res.get("tradeApps." + name));
        headline.setGraphic(ImageUtil.getImageViewById(model.getIconId()));
        headline.getStyleClass().add("trade-protocols-roadmap-headline");
        headline.setGraphicTextGap(10);

        Label subHeadline = new Label(Res.get("tradeApps." + name + ".subHeadline"));
        subHeadline.setWrapText(true);
        subHeadline.getStyleClass().add("trade-protocols-roadmap-sub-headline");

        Label overviewHeadline = new Label(Res.get("tradeApps.overview"));
        overviewHeadline.getStyleClass().add("trade-protocols-roadmap-content-headline");

        UnorderedList overview = new UnorderedList(Res.get("tradeApps." + name + ".overview"), "trade-protocols-roadmap-text");

        Label releaseHeadline = new Label(Res.get("tradeApps.release"));
        releaseHeadline.getStyleClass().addAll("font-size-16", "font-light");

        Label release = new Label(Res.get("tradeApps." + name + ".release"));
        release.setWrapText(true);
        release.getStyleClass().addAll("font-size-12", "font-light");

        Label tradeOffsHeadline = new Label(Res.get("tradeApps.tradeOffs"));
        tradeOffsHeadline.getStyleClass().addAll("font-size-16", "font-light");

        UnorderedList pro = new UnorderedList(Res.get("tradeApps." + name + ".pro"), "trade-protocols-roadmap-text", "\\+ ", "+ ");
        UnorderedList con = new UnorderedList(Res.get("tradeApps." + name + ".con"), "trade-protocols-roadmap-text", "- ", "- ");

        learnMore = new Hyperlink(Res.get("action.learnMore"));
        learnMore.getStyleClass().addAll("font-size-12", "text-fill-green");

        VBox.setMargin(headline, new Insets(0, 0, 0, 0));
        VBox.setMargin(overviewHeadline, new Insets(25, 0, 0, 0));
        VBox.setMargin(releaseHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeOffsHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(con, new Insets(0, 0, 15, 0));
        root.getChildren().addAll(headline, subHeadline,
                overviewHeadline, overview,
                releaseHeadline, release,
                tradeOffsHeadline, pro, con,
                learnMore);
    }

    @Override
    protected void onViewAttached() {
        learnMore.setOnAction(e -> controller.onLearnMore());
    }

    @Override
    protected void onViewDetached() {
        learnMore.setOnAction(null);
    }
}
