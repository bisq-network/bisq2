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

package bisq.desktop.primary.main.content.academy;

import bisq.desktop.common.Browser;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public abstract class AcademyView<M extends Model, C extends Controller> extends View<VBox, M, C> {
    protected final Hyperlink learnMore;


    public AcademyView(M model, C controller) {
        super(new VBox(15), model, controller);

        String key = getKey();
        Label headline = new Label(Res.get("academy.overview." + key));
        headline.getStyleClass().addAll("font-size-17", "font-light");
        headline.setWrapText(true);

        Label subHeadline = new Label(Res.get("academy." + key + ".subHeadline"));
        subHeadline.getStyleClass().addAll("font-size-14", "font-light");
        subHeadline.setWrapText(true);

        Region separator = new Region();
        separator.getStyleClass().add("separator-medium-grey");

        Label content = new Label(Res.get("academy." + key + ".content"));
        content.getStyleClass().addAll("font-size-12", "font-light");
        content.setWrapText(true);

        learnMore = new Hyperlink(Res.get("learnMore"));
        learnMore.getStyleClass().addAll("font-size-12", "text-fill-green");

        VBox.setMargin(separator, new Insets(15, 0, 40, 0));
        VBox.setMargin(content, new Insets(0, 0, 30, 0));
        root.getChildren().addAll(headline, subHeadline, separator, content, learnMore);
    }

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
