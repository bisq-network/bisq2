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

import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.TransparentTextField;
import bisq.desktop.components.controls.validator.TextMaxLengthValidator;
import bisq.desktop.main.content.user.profile_card.ProfileCardView;
import bisq.i18n.Res;
import bisq.user.contact_list.ContactListService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ProfileCardMyNotesView extends View<VBox, ProfileCardMyNotesModel, ProfileCardMyNotesController> {
    private static final TextMaxLengthValidator TAG_MAX_LENGTH_VALIDATOR =
            new TextMaxLengthValidator(Res.get("user.profileCard.myNotes.transparentTextField.tag.maxLength",
                    ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH),
                    ContactListService.CONTACT_LIST_ENTRY_MAX_TAG_LENGTH);

    private final TransparentTextField contactReasonTextField, tagTextField;
    private final Label disclaimerLabel;

    public ProfileCardMyNotesView(ProfileCardMyNotesModel model,
                                  ProfileCardMyNotesController controller) {
        super(new VBox(), model, controller);

        tagTextField = new TransparentTextField(Res.get("user.profileCard.myNotes.tag"), true,
                controller::onSaveTag, this::cancelEditingTag);
        tagTextField.setValidator(TAG_MAX_LENGTH_VALIDATOR);
        contactReasonTextField = new TransparentTextField(Res.get("user.profileCard.myNotes.contactReason"), false);
        VBox vBox = new VBox(20, tagTextField, contactReasonTextField);

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
        tagTextField.setText(model.getTag().get());
        disclaimerLabel.setText(model.getDisclaimerText());

        contactReasonTextField.initialize();
        tagTextField.initialize();
    }

    @Override
    protected void onViewDetached() {
        contactReasonTextField.dispose();
        tagTextField.dispose();
    }

    private void cancelEditingTag() {
        UIThread.run(() -> tagTextField.setText(model.getTag().get()));
    }
}
