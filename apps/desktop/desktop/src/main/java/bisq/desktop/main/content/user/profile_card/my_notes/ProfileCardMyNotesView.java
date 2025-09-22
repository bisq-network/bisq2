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

package bisq.desktop.main.content.user.profile_card.my_notes;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.TransparentTextField;
import bisq.desktop.main.content.user.profile_card.ProfileCardView;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileCardMyNotesView extends View<VBox, ProfileCardMyNotesModel, ProfileCardMyNotesController> {
    private final Label disclaimerLabel;
    private final TransparentTextField contactReasonTextField;

    public ProfileCardMyNotesView(ProfileCardMyNotesModel model,
                                  ProfileCardMyNotesController controller) {
        super(new VBox(), model, controller);

        contactReasonTextField = new TransparentTextField(Res.get("user.profileCard.myNotes.contactReason"));
        contactReasonTextField.setEditable(false);
        contactReasonTextField.setPrefWidth(250);
        VBox vBox = new VBox(contactReasonTextField);

        HBox myNotesDataHBox = new HBox(10, vBox);

        disclaimerLabel = new Label();
        disclaimerLabel.setGraphicTextGap(7);
        disclaimerLabel.setGraphic(ImageUtil.getImageViewById("icon-info-grey"));
        disclaimerLabel.getStyleClass().addAll("text-fill-grey-dimmed", "compact-text");

        VBox contentBox = new VBox(10, myNotesDataHBox, Spacer.fillVBox(), disclaimerLabel);
        contentBox.getStyleClass().add("bisq-common-bg");
        contentBox.setAlignment(Pos.TOP_LEFT);
        contentBox.setMinHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);
        contentBox.setPrefHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);
        contentBox.setMaxHeight(ProfileCardView.SUB_VIEWS_CONTENT_HEIGHT);

        root.getChildren().add(contentBox);
        root.setPadding(new Insets(20, 0, 20, 0));
        root.getStyleClass().add("my-notes");
    }

    @Override
    protected void onViewAttached() {
        contactReasonTextField.setText(model.getContactReason());
        disclaimerLabel.setText(model.getDisclaimerText());
    }

    @Override
    protected void onViewDetached() {
    }
}
