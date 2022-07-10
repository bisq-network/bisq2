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

public class RoboIconWithId extends VBox {
    private final ImageView imageView;
    private final Label nym;
    private final StringProperty textProperty = new SimpleStringProperty();

    public RoboIconWithId(double size) {
        imageView = new ImageView();
        imageView.setCursor(Cursor.HAND);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        setAlignment(Pos.CENTER);
        nym = new Label();
        nym.setMaxWidth(size);
        nym.setMinWidth(size);
        nym.setTextAlignment(TextAlignment.CENTER);
        nym.setPadding(new Insets(7, 7, 7, 7));
        nym.getStyleClass().add("bisq-large-profile-id-label");

        getChildren().addAll(imageView, nym);

        EasyBind.subscribe(textProperty, this::setNym);
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

    public void setNym(String id) {
        nym.setText(Res.get("roboIconWithId.id", id));
    }
}