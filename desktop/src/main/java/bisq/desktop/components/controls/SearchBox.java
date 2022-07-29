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

package bisq.desktop.components.controls;

import bisq.desktop.common.utils.ImageUtil;
import bisq.i18n.Res;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

public class SearchBox extends HBox {
    private final TextField searchField;

    public SearchBox() {
        setAlignment(Pos.CENTER_LEFT);
        setMaxHeight(30);
        getStyleClass().add("small-search-box");

        ImageView searchIcon = ImageUtil.getImageViewById("search-white");
        searchField = new TextField();
        searchField.setPromptText(Res.get("search"));
        searchField.getStyleClass().add("small-search-text");

        HBox.setMargin(searchIcon, new Insets(0, -3, 0, 7));
        getChildren().addAll(searchIcon, searchField);
    }

    public final StringProperty textProperty() {
        return searchField.textProperty();
    }

    public String getText() {
        return searchField.getText();
    }
}