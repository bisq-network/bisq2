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

package bisq.desktop.main.content.settings.misc;

import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.Switch;
import bisq.desktop.main.content.settings.SettingsViewUtils;
import bisq.i18n.Res;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MiscSettingsView extends View<VBox, MiscSettingsModel, MiscSettingsController> {
    private static final double TEXT_FIELD_WIDTH = 500;

    private final Button resetDontShowAgain;
    private final Switch useAnimations, preventStandbyMode, addContactsAutomatically;
    private final MaterialTextField totalMaxBackupSizeInMB;

    public MiscSettingsView(MiscSettingsModel model, MiscSettingsController controller) {
        super(new VBox(), model, controller);

        // Display settings
        Label displayHeadline = SettingsViewUtils.getHeadline(Res.get("settings.display.headline"));

        useAnimations = new Switch(Res.get("settings.display.useAnimations"));
        preventStandbyMode = new Switch(Res.get("settings.display.preventStandbyMode"));
        resetDontShowAgain = new Button(Res.get("settings.display.resetDontShowAgain"));
        resetDontShowAgain.getStyleClass().add("grey-transparent-outlined-button");

        VBox.setMargin(resetDontShowAgain, new Insets(10, 0, 0, 0));
        VBox displayVBox = new VBox(10, useAnimations, preventStandbyMode, resetDontShowAgain);

        // Backup settings
        Label backupHeadline = SettingsViewUtils.getHeadline(Res.get("settings.backup.headline"));
        ImageView infoIcon = ImageUtil.getImageViewById("info");
        infoIcon.setOpacity(0.6);
        Tooltip.install(infoIcon, new BisqTooltip(Res.get("settings.backup.totalMaxBackupSizeInMB.info.tooltip")));
        HBox.setMargin(infoIcon, new Insets(5, 0, 0, 0));
        HBox backupHeadlineHBox = new HBox(10, backupHeadline, infoIcon);

        totalMaxBackupSizeInMB = new MaterialTextField(Res.get("settings.backup.totalMaxBackupSizeInMB.description"));
        totalMaxBackupSizeInMB.setMaxWidth(TEXT_FIELD_WIDTH);
        totalMaxBackupSizeInMB.setValidators(model.getTotalMaxBackupSizeValidator());

        // Contacts settings
        Label contactsHeadline = SettingsViewUtils.getHeadline(Res.get("settings.contacts.headline"));
        Label contactsDescription = new Label(Res.get("settings.contacts.addContactsAutomatically.description"));
        contactsDescription.getStyleClass().addAll("normal-text", "wrap-text", "text-fill-grey-dimmed");
        addContactsAutomatically = new Switch(Res.get("settings.contacts.addContactsAutomatically.switch"));
        VBox contactsVBox = new VBox(20, contactsDescription, addContactsAutomatically);

        VBox.setMargin(displayVBox, new Insets(0, 5, 0, 5));
        VBox contentBox = new VBox(50,
                displayHeadline, separator(), displayVBox,
                backupHeadlineHBox, separator(), totalMaxBackupSizeInMB,
                contactsHeadline, separator(), contactsVBox);
        contentBox.getStyleClass().add("bisq-common-bg");
        root.setAlignment(Pos.TOP_LEFT);
        root.getChildren().add(contentBox);
        root.setPadding(new Insets(0, 40, 20, 40));
    }

    @Override
    protected void onViewAttached() {
        useAnimations.selectedProperty().bindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().bindBidirectional(model.getPreventStandbyMode());
        resetDontShowAgain.setOnAction(e -> controller.onResetDontShowAgain());

        Bindings.bindBidirectional(totalMaxBackupSizeInMB.textProperty(), model.getTotalMaxBackupSizeInMB(),
                model.getTotalMaxBackupSizeConverter());
        totalMaxBackupSizeInMB.validate();

        addContactsAutomatically.selectedProperty().bindBidirectional(model.getAddContactsAutomatically());
    }

    @Override
    protected void onViewDetached() {
        useAnimations.selectedProperty().unbindBidirectional(model.getUseAnimations());
        preventStandbyMode.selectedProperty().unbindBidirectional(model.getPreventStandbyMode());
        resetDontShowAgain.setOnAction(null);

        Bindings.unbindBidirectional(totalMaxBackupSizeInMB.textProperty(), model.getTotalMaxBackupSizeInMB());
        totalMaxBackupSizeInMB.resetValidation();

        addContactsAutomatically.selectedProperty().unbindBidirectional(model.getAddContactsAutomatically());
    }

    private static Region separator() {
        return SettingsViewUtils.getLineAfterHeadline(50);
    }
}
