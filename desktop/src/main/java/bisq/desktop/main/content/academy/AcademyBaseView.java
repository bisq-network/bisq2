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

package bisq.desktop.main.content.academy;

import bisq.desktop.common.Browser;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AcademyBaseView<M extends Model, C extends Controller> extends View<VBox, M, C> {
    protected final Label headline, subHeadline;
    protected final Hyperlink learnMore;

    public AcademyBaseView(M model, C controller) {
        super(new VBox(10), model, controller);

        String key = getKey();
        headline = new Label(Res.get("academy.overview." + key));
        headline.setGraphic(ImageUtil.getImageViewById(getIconId()));
        headline.getStyleClass().addAll("font-size-20", "font-light");
        headline.setGraphicTextGap(10);
        headline.setWrapText(true);

        subHeadline = new Label(Res.get("academy." + key + ".subHeadline"));
        subHeadline.getStyleClass().addAll("font-size-14", "font-light", "text-fill-grey-dimmed");
        subHeadline.setWrapText(true);

        learnMore = new Hyperlink(Res.get("action.learnMore"));
        learnMore.getStyleClass().addAll("font-size-12", "text-fill-green");
        VBox.setMargin(learnMore, new Insets(25, 0, 0,0));

        VBox.setMargin(headline, new Insets(0, 0, 0, 0));
        root.getChildren().addAll(headline, subHeadline);
        root.setPadding(new Insets(0, 40, 40, 40));
    }

    protected Label addHeadlineLabel(String headlineKey) {
        Label label = new Label(Res.get("academy." + getKey() + "." + headlineKey));
        label.getStyleClass().addAll("font-size-16", "font-light");
        label.setWrapText(true);
        root.getChildren().add(label);
        return label;
    }

    protected Label addContentLabel(String contentKey) {
        Label label = new Label(Res.get("academy." + getKey() + "." + contentKey));
        label.getStyleClass().addAll("font-size-12", "font-light", "bisq-line-spacing-01");
        label.setWrapText(true);
        VBox.setMargin(label, new Insets(25, 0, 0,0));
        root.getChildren().add(label);
        return label;
    }

    protected void addLearnMoreHyperlink() {
        checkArgument(!root.getChildren().contains(learnMore));
        root.getChildren().add(learnMore);
    }

    protected void setHeadlineMargin(Label headlineLabel) {
        VBox.setMargin(headlineLabel, new Insets(35, 0, 0, 0));
    }

    protected void setLastLabelMargin(Label lastLabel) {
        VBox.setMargin(lastLabel, new Insets(0, 0, 15, 0));
    }

    protected abstract String getIconId();

    protected abstract String getKey();

    protected abstract String getUrl();

    @Override
    protected void onViewAttached() {
        learnMore.setOnAction(e -> Browser.open(getUrl()));
    }

    @Override
    protected void onViewDetached() {
        learnMore.setOnAction(null);
    }
}