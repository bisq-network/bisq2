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

import bisq.i18n.Res;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.WeakReference;

@Slf4j
@Getter
public class SearchBox extends HBox {
    private final TextField searchField;
    private final ImageView searchIcon;
    private String defaultStyle = "search-box";
    private String activeStyle = "search-box-active";
    private String iconId = "search-dimmed";
    private String activeIconId = "search-white";

    public SearchBox() {
        setAlignment(Pos.CENTER_LEFT);
        setMaxHeight(30);
        getStyleClass().add(defaultStyle);

        searchIcon = new ImageView();
        searchField = new TextField();
        searchField.setPromptText(Res.get("action.search"));
        searchField.getStyleClass().add("search-text-field");

        HBox.setMargin(searchIcon, new Insets(0, -3, 0, 7));
        getChildren().addAll(searchIcon, searchField);

        searchField.focusedProperty().addListener(new WeakReference<>((ChangeListener<Boolean>) (observable, oldValue, newValue) -> applyStyle(newValue)).get());
        applyStyle(searchField.isFocused());
    }

    public void setDefaultStyle(String defaultStyle) {
        this.defaultStyle = defaultStyle;
        applyStyle(searchField.isFocused());

    }

    public void setActiveStyle(String activeStyle) {
        this.activeStyle = activeStyle;
        applyStyle(searchField.isFocused());
    }

    public void setIconId(String iconId) {
        this.iconId = iconId;
        applyStyle(searchField.isFocused());
    }

    public void setActiveIconId(String iconId) {
        this.activeIconId = iconId;
        applyStyle(searchField.isFocused());
    }

    public final StringProperty textProperty() {
        return searchField.textProperty();
    }

    public String getText() {
        return searchField.getText();
    }

    private void applyStyle(boolean isFocused) {
        if (isFocused) {
            searchIcon.setId(activeIconId);
            getStyleClass().remove(defaultStyle);
            getStyleClass().add(activeStyle);
        } else {
            searchIcon.setId(iconId);
            getStyleClass().add(defaultStyle);
            getStyleClass().remove(activeStyle);
        }
    }
}