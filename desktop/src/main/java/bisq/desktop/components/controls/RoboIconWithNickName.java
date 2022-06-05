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

import bisq.desktop.components.robohash.RoboHash;
import bisq.social.user.ChatUserIdentity;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.text.TextAlignment;

import javax.annotation.Nullable;

public class RoboIconWithNickName extends HBox {
    public enum Size {
        SMALL(75),
        MEDIUM(150),
        LARGE(300);
        private final int size;

        Size(int size) {
            this.size = size;
        }
    }

    private final ImageView imageView;
    private final Label nickNameLabel;

    public RoboIconWithNickName() {
        this(null, Size.MEDIUM);
    }

    public RoboIconWithNickName(@Nullable ChatUserIdentity chatUserIdentity, Size size) {
        setAlignment(Pos.CENTER);
        setSpacing(10);

        imageView = new ImageView();
        imageView.setCursor(Cursor.HAND);
        imageView.setFitWidth(size.size);
        imageView.setFitHeight(size.size);

        nickNameLabel = new Label();
        nickNameLabel.setMaxWidth(size.size);
        nickNameLabel.setMinWidth(size.size);
        nickNameLabel.setTextAlignment(TextAlignment.CENTER);
        nickNameLabel.setPadding(new Insets(7, 7, 7, 7));
        // nickNameLabel.getStyleClass().add("bisq-large-profile-id-label");

        getChildren().addAll(imageView, nickNameLabel);
        if (chatUserIdentity != null) {
            setUserProfile(chatUserIdentity);
        }
    }

    public void setOnAction(Runnable handler) {
        imageView.setOnMousePressed(e -> handler.run());
    }

    public void setRoboHashImage(Image roboIconImage) {
        imageView.setImage(roboIconImage);
    }

    public void setUserProfile(ChatUserIdentity chatUserIdentity) {
        imageView.setImage(RoboHash.getImage(chatUserIdentity.getIdentity().proofOfWork()));
        nickNameLabel.setText(chatUserIdentity.getNickName());
        Tooltip.install(this, new Tooltip(chatUserIdentity.getTooltipString()));
    }
}