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
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import org.fxmisc.easybind.EasyBind;

public class LargeRoboIconWithId extends VBox {
    private final ImageView imageView;
    private final Label profileId;
    private final StringProperty textProperty = new SimpleStringProperty();

    public LargeRoboIconWithId() {
        imageView = new ImageView();
        imageView.setCursor(Cursor.HAND);
        setAlignment(Pos.CENTER);
        profileId = new Label();
        profileId.setMaxWidth(300);
        profileId.setMinWidth(300);
        profileId.setTextAlignment(TextAlignment.CENTER);
        profileId.setPadding(new Insets(7, 7, 7, 7));
        profileId.getStyleClass().add("bisq-large-profile-id-label");

        getChildren().addAll(imageView, profileId);

        EasyBind.subscribe(textProperty, this::setProfileId);
    }

    public void setOnAction(Runnable handler) {
        imageView.setOnMousePressed(e -> handler.run());
    }

    public void setRoboHashImage(Image roboIconImage) {
        imageView.setImage(roboIconImage);
    }

    public final StringProperty textProperty() {
        return textProperty;
    }

    public void setProfileId(String id) {
        profileId.setText(Res.get("largeRoboIconWithId.id", id));
    }
}