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

package bisq.desktop.primary.main.content.trade;

import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MultiLineLabel;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ProtocolRoadmapView<M extends Model, C extends Controller> extends View<VBox, M, C> {
    protected final MultiLineLabel headline, subHeadline, overviewHeadline, overview, releaseHeadline, release, tradeOffsHeadline, tradeOffs;
    protected final Hyperlink learnMore;

    public ProtocolRoadmapView(M model, C controller) {
        super(new VBox(10), model, controller);

        String key = getKey();
        headline = new MultiLineLabel(Res.get("trade.protocols." + key));
        headline.setGraphic(ImageUtil.getImageViewById(getIconId()));
        headline.getStyleClass().addAll("font-size-20", "font-light");
        headline.setGraphicTextGap(10);

        subHeadline = new MultiLineLabel(Res.get("trade.protocols." + key + ".subHeadline"));
        subHeadline.getStyleClass().addAll("font-size-14", "font-light", "text-fill-grey-dimmed");

        overviewHeadline = new MultiLineLabel(Res.get("trade.protocols.overview"));
        overviewHeadline.getStyleClass().addAll("font-size-16", "font-light");

        overview = new MultiLineLabel(Res.get("trade.protocols." + key + ".overview"));
        overview.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");

        releaseHeadline = new MultiLineLabel(Res.get("trade.protocols.release"));
        releaseHeadline.getStyleClass().addAll("font-size-16", "font-light");

        release = new MultiLineLabel(Res.get("trade.protocols." + key + ".release"));
        release.getStyleClass().addAll("font-size-12", "font-light");

        tradeOffsHeadline = new MultiLineLabel(Res.get("trade.protocols.tradeOffs"));
        tradeOffsHeadline.getStyleClass().addAll("font-size-16", "font-light");

        tradeOffs = new MultiLineLabel(Res.get("trade.protocols." + key + ".tradeOffs"));
        tradeOffs.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");

        learnMore = new Hyperlink(Res.get("learnMore"));
        learnMore.getStyleClass().addAll("font-size-12", "text-fill-green");

        VBox.setMargin(headline, new Insets(0, 0, 0, 0));
        VBox.setMargin(overviewHeadline, new Insets(25, 0, 0, 0));
        VBox.setMargin(releaseHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeOffsHeadline, new Insets(35, 0, 0, 0));
        VBox.setMargin(tradeOffs, new Insets(0, 0, 15, 0));
        root.getChildren().addAll(headline, subHeadline,
                overviewHeadline, overview,
                releaseHeadline, release,
                tradeOffsHeadline, tradeOffs,
                learnMore);

    }

    abstract protected String getIconId();

    abstract protected String getKey();

    abstract protected String getUrl();

    @Override
    protected void onViewAttached() {
        learnMore.setOnAction(e -> Browser.open(getUrl()));
    }

    @Override
    protected void onViewDetached() {
        learnMore.setOnAction(null);
    }
}
