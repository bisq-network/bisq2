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
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.List;

public class ProfileCardMessagesView extends View<VBox, ProfileCardMessagesModel, ProfileCardMessagesController> {
    private final VBox channelMessagesVBox = new VBox();

    public ProfileCardMessagesView(ProfileCardMessagesModel model,
                                   ProfileCardMessagesController controller) {
        super(new VBox(), model, controller);

        root.getChildren().add(channelMessagesVBox);
    }

    @Override
    protected void onViewAttached() {
    }

    @Override
    protected void onViewDetached() {
    }

    void updateProfileCardMessages(List<VBox> channelMessages) {
        channelMessagesVBox.getChildren().setAll(channelMessages);
    }
}
