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

package bisq.desktop.main.content.user.profile_card.messages;

import bisq.desktop.common.view.View;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class ProfileCardMessagesView extends View<VBox, ProfileCardMessagesModel, ProfileCardMessagesController> {
    private final VBox channelMessagesVBox;
    private final ScrollPane scrollPane;
    private final Label noMessagesLabel;

    public ProfileCardMessagesView(ProfileCardMessagesModel model,
                                   ProfileCardMessagesController controller) {
        super(new VBox(), model, controller);

        channelMessagesVBox = new VBox();

        noMessagesLabel = new Label(Res.get("user.profileCard.messages.noMessages"));
        noMessagesLabel.getStyleClass().add("no-messages-placeholder-label");

        VBox contentVBox = new VBox(channelMessagesVBox, noMessagesLabel);
        contentVBox.setFillWidth(true);
        contentVBox.setAlignment(Pos.TOP_CENTER);

        scrollPane = new ScrollPane(contentVBox);
        scrollPane.getStyleClass().add("message-list-bg");
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(307);
        scrollPane.setMinHeight(307);

        root.setPadding(new Insets(20, 0, 0, 0));
        root.getChildren().add(scrollPane);
    }

    @Override
    protected void onViewAttached() {
        scrollPane.setVvalue(0);

        channelMessagesVBox.visibleProperty().bind(model.getShouldShowMessages());
        channelMessagesVBox.managedProperty().bind(model.getShouldShowMessages());
        noMessagesLabel.visibleProperty().bind(model.getShouldShowMessages().not());
        noMessagesLabel.managedProperty().bind(model.getShouldShowMessages().not());
    }

    @Override
    protected void onViewDetached() {
        channelMessagesVBox.visibleProperty().unbind();
        channelMessagesVBox.managedProperty().unbind();
        noMessagesLabel.visibleProperty().unbind();
        noMessagesLabel.managedProperty().unbind();
    }

    void updateProfileCardMessages(List<VBox> channelMessages) {
        channelMessagesVBox.getChildren().setAll(channelMessages);
    }
}
