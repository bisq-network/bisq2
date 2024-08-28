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

package bisq.desktop.main.content.settings.utils;

import bisq.desktop.common.Layout;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.DirectoryPathValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UtilsView extends View<VBox, UtilsModel, UtilsController> {

    private static final ValidatorBase DIRECTORY_PATH_VALIDATOR = new DirectoryPathValidator(Res.get("settings.utils.backup.location.invalid"));

    private final Button setBackupLocationButton, backupButton;
    private final MaterialTextField backupLocation;
    private final Hyperlink webpage, dao, sourceCode, community, contribute,
            openLogFileButton, openTorLogFileButton, openDataDirButton, chatRules, tradeGuide, walletGuide, license, tac;

    public UtilsView(UtilsModel model, UtilsController controller) {
        super(new VBox(50), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        Label backupHeadline = new Label(Res.get("settings.utils.backup.headline"));
        backupHeadline.getStyleClass().add("large-thin-headline");
        backupLocation = new MaterialTextField(Res.get("settings.utils.backup.location"),
                Res.get("settings.utils.backup.location.prompt"),
                Res.get("settings.utils.backup.location.help"));
        backupLocation.setValidators(DIRECTORY_PATH_VALIDATOR);
        setBackupLocationButton = new Button(Res.get("settings.utils.backup.setLocationButton"));
        backupButton = new Button(Res.get("settings.utils.backup.backupButton"));
        HBox backupButtons = new HBox(10, setBackupLocationButton, backupButton);
        VBox backupBox = new VBox(20, backupLocation, backupButtons);

        Label localDataHeadline = new Label(Res.get("settings.utils.localData.headline"));
        localDataHeadline.getStyleClass().add("large-thin-headline");
        openDataDirButton = new Hyperlink(Res.get("settings.utils.localData.openDataDir"));
        openLogFileButton = new Hyperlink(Res.get("settings.utils.localData.openLogFile"));
        openTorLogFileButton = new Hyperlink(Res.get("settings.utils.localData.openTorLogFile"));

        VBox localDataBox = new VBox(5, openDataDirButton, openLogFileButton, openTorLogFileButton);

        Label rulesHeadline = new Label(Res.get("settings.utils.rules.headline"));
        rulesHeadline.getStyleClass().add("large-thin-headline");
        chatRules = new Hyperlink(Res.get("settings.utils.rules.chatRules"));
        tradeGuide = new Hyperlink(Res.get("settings.utils.rules.tradeGuide"));
        walletGuide = new Hyperlink(Res.get("settings.utils.rules.walletGuide"));
        VBox rulesAndGuidesBox = new VBox(5, chatRules, tradeGuide, walletGuide);

        Label legalHeadline = new Label(Res.get("settings.utils.legal.headline"));
        legalHeadline.getStyleClass().add("large-thin-headline");
        tac = new Hyperlink(Res.get("settings.utils.legal.tac"));
        license = new Hyperlink(Res.get("settings.utils.legal.license"));
        VBox legalBox = new VBox(5, tac, license);

        Label resourcesHeadline = new Label(Res.get("settings.utils.resources.headline"));
        resourcesHeadline.getStyleClass().add("large-thin-headline");
        webpage = new Hyperlink(Res.get("settings.utils.resources.webpage"));
        dao = new Hyperlink(Res.get("settings.utils.resources.dao"));
        sourceCode = new Hyperlink(Res.get("settings.utils.resources.sourceCode"));
        community = new Hyperlink(Res.get("settings.utils.resources.community"));
        contribute = new Hyperlink(Res.get("settings.utils.resources.contribute"));

        VBox resourcesBox = new VBox(5, webpage, dao, sourceCode, community, contribute);

        Insets value = new Insets(0, 5, 0, 5);
        VBox.setMargin(backupBox, value);
        VBox.setMargin(localDataBox, value);
        VBox.setMargin(rulesAndGuidesBox, value);
        VBox.setMargin(legalBox, value);
        VBox.setMargin(resourcesBox, value);
        root.getChildren().addAll(backupHeadline, getLine(), backupBox,
                localDataHeadline, getLine(), localDataBox,
                rulesHeadline, getLine(), rulesAndGuidesBox,
                legalHeadline, getLine(), legalBox,
                resourcesHeadline, getLine(), resourcesBox);
    }

    @Override
    protected void onViewAttached() {
        backupLocation.resetValidation();
        backupLocation.textProperty().bindBidirectional(model.getBackupLocation());
        backupLocation.validate();
        setBackupLocationButton.defaultButtonProperty().bind(model.getBackupButtonDefault().not());
        backupButton.defaultButtonProperty().bind(model.getBackupButtonDefault());
        backupButton.disableProperty().bind(model.getBackupButtonDisabled());

        openLogFileButton.setOnAction(e -> controller.onOpenLogFile());
        openTorLogFileButton.setOnAction(e -> controller.onOpenTorLogFile());
        openDataDirButton.setOnAction(e -> controller.onOpenDataDir());
        setBackupLocationButton.setOnAction(e -> controller.onSetBackupLocation());
        backupButton.setOnAction(e -> onBackupButtonPressed());
        chatRules.setOnAction(e -> controller.onOpenChatRules());
        tradeGuide.setOnAction(e -> controller.onOpenTradeGuide());
        walletGuide.setOnAction(e -> controller.onOpenWalletGuide());
        tac.setOnAction(e -> controller.onTac());
        license.setOnAction(e -> controller.onOpenLicense());
        webpage.setOnAction(e -> controller.onOpenWebpage());
        dao.setOnAction(e -> controller.onOpenDao());
        sourceCode.setOnAction(e -> controller.onOpenSourceCode());
        community.setOnAction(e -> controller.onOpenCommunity());
        contribute.setOnAction(e -> controller.onOpenContribute());
    }

    private void onBackupButtonPressed() {
        if(backupLocation.validate()) {
            controller.onBackup();
        }
    }

    @Override
    protected void onViewDetached() {
        backupLocation.setText("");
        backupLocation.resetValidation();
        backupLocation.textProperty().unbindBidirectional(model.getBackupLocation());
        setBackupLocationButton.defaultButtonProperty().unbind();
        backupButton.defaultButtonProperty().unbind();
        backupButton.disableProperty().unbind();

        openLogFileButton.setOnAction(null);
        openTorLogFileButton.setOnAction(null);
        openDataDirButton.setOnAction(null);
        setBackupLocationButton.setOnAction(null);
        backupButton.setOnAction(null);
        chatRules.setOnAction(null);
        tradeGuide.setOnAction(null);
        walletGuide.setOnAction(null);
        tac.setOnAction(null);
        license.setOnAction(null);
        webpage.setOnAction(null);
        dao.setOnAction(null);
        sourceCode.setOnAction(null);
        community.setOnAction(null);
        contribute.setOnAction(null);
    }

    private Region getLine() {
        Region line = Layout.hLine();
        VBox.setMargin(line, new Insets(-42.5, 0, -30, 0));
        return line;
    }
}
