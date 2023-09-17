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

import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UtilsView extends View<VBox, UtilsModel, UtilsController> {

    private final Button openLogFileButton, openDataDirButton, setBackupLocationButton, backupButton,
            chatRules, tradeGuide, license, tac;
    private final MaterialTextField backupLocation;
    private final Hyperlink webpage, dao, sourceCode, community, contribute;

    public UtilsView(UtilsModel model, UtilsController controller) {
        super(new VBox(20), model, controller);

        root.setPadding(new Insets(30, 0, 0, 0));
        root.setAlignment(Pos.TOP_LEFT);

        Label backupHeadline = new Label(Res.get("settings.utils.backup.headline"));
        backupHeadline.getStyleClass().addAll("settings-headline");
        backupLocation = new MaterialTextField(Res.get("settings.utils.backup.location"),
                Res.get("settings.utils.backup.location.prompt"),
                Res.get("settings.utils.backup.location.help"));
        setBackupLocationButton = new Button(Res.get("settings.utils.backup.setLocationButton"));
        backupButton = new Button(Res.get("settings.utils.backup.backupButton"));
        HBox backupButtons = new HBox(10, setBackupLocationButton, backupButton);
        VBox backupBox = new VBox(20, backupLocation, backupButtons);
        backupBox.getStyleClass().add("settings-box-bg");

        Label localDataHeadline = new Label(Res.get("settings.utils.localData.headline"));
        localDataHeadline.getStyleClass().addAll("settings-headline");
        openDataDirButton = new Button(Res.get("settings.utils.localData.openDataDir"));
        openDataDirButton.getStyleClass().add("grey-transparent-outlined-button");
        openLogFileButton = new Button(Res.get("settings.utils.localData.openLogFile"));
        openLogFileButton.getStyleClass().add("grey-transparent-outlined-button");
        HBox localDataBox = new HBox(20, openDataDirButton, openLogFileButton);
        localDataBox.getStyleClass().add("settings-box-bg");

        Label rulesHeadline = new Label(Res.get("settings.utils.rules.headline"));
        rulesHeadline.getStyleClass().addAll("settings-headline");
        chatRules = new Button(Res.get("settings.utils.rules.chatRules"));
        chatRules.getStyleClass().add("grey-transparent-outlined-button");
        tradeGuide = new Button(Res.get("settings.utils.rules.tradeGuide"));
        tradeGuide.getStyleClass().add("grey-transparent-outlined-button");
        HBox rulesBox = new HBox(20, chatRules, tradeGuide);
        rulesBox.getStyleClass().add("settings-box-bg");

        Label legalHeadline = new Label(Res.get("settings.utils.legal.headline"));
        legalHeadline.getStyleClass().addAll("settings-headline");
        tac = new Button(Res.get("settings.utils.legal.tac"));
        tac.getStyleClass().add("grey-transparent-outlined-button");
        license = new Button(Res.get("settings.utils.legal.license"));
        license.getStyleClass().add("grey-transparent-outlined-button");
        HBox legalBox = new HBox(20, tac, license);
        legalBox.getStyleClass().add("settings-box-bg");

        Label resourcesHeadline = new Label(Res.get("settings.utils.resources.headline"));
        resourcesHeadline.getStyleClass().addAll("settings-headline");
        webpage = new Hyperlink(Res.get("settings.utils.resources.webpage"));
        dao = new Hyperlink(Res.get("settings.utils.resources.dao"));
        sourceCode = new Hyperlink(Res.get("settings.utils.resources.sourceCode"));
        community = new Hyperlink(Res.get("settings.utils.resources.community"));
        contribute = new Hyperlink(Res.get("settings.utils.resources.contribute"));

        VBox resourcesBox = new VBox(5, webpage, dao, sourceCode, community, contribute);
        resourcesBox.getStyleClass().add("settings-box-bg");

        VBox.setMargin(backupHeadline, new Insets(-8, 0, -10, 0));
        VBox.setMargin(localDataHeadline, new Insets(0, 0, -10, 0));
        VBox.setMargin(rulesHeadline, new Insets(0, 0, -10, 0));
        VBox.setMargin(legalHeadline, new Insets(0, 0, -10, 0));
        VBox.setMargin(resourcesHeadline, new Insets(0, 0, -10, 0));
        root.getChildren().addAll(backupHeadline, backupBox,
                localDataHeadline, localDataBox,
                rulesHeadline, rulesBox,
                legalHeadline, legalBox,
                resourcesHeadline, resourcesBox);
    }

    @Override
    protected void onViewAttached() {
        backupLocation.textProperty().bindBidirectional(model.getBackupLocation());
        setBackupLocationButton.defaultButtonProperty().bind(model.getBackupButtonDefault().not());
        backupButton.disableProperty().bind(model.getBackupButtonDisabled());
        backupButton.defaultButtonProperty().bind(model.getBackupButtonDefault());

        openLogFileButton.setOnAction(e -> controller.onOpenLogFile());
        openDataDirButton.setOnAction(e -> controller.onOpenDataDir());
        setBackupLocationButton.setOnAction(e -> controller.onSetBackupLocation());
        backupButton.setOnAction(e -> controller.onBackup());
        chatRules.setOnAction(e -> controller.onChatRules());
        tradeGuide.setOnAction(e -> controller.onTradeGuide());
        tac.setOnAction(e -> controller.onTac());
        license.setOnAction(e -> controller.onOpenLicense());
        webpage.setOnAction(e -> controller.onOpenWebpage());
        dao.setOnAction(e -> controller.onOpenDao());
        sourceCode.setOnAction(e -> controller.onOpenSourceCode());
        community.setOnAction(e -> controller.onOpenCommunity());
        contribute.setOnAction(e -> controller.onOpenContribute());
    }

    @Override
    protected void onViewDetached() {
        backupLocation.textProperty().unbindBidirectional(model.getBackupLocation());
        setBackupLocationButton.defaultButtonProperty().unbind();
        backupButton.disableProperty().unbind();
        backupButton.defaultButtonProperty().unbind();

        openLogFileButton.setOnAction(null);
        openDataDirButton.setOnAction(null);
        setBackupLocationButton.setOnAction(null);
        backupButton.setOnAction(null);
        chatRules.setOnAction(null);
        tradeGuide.setOnAction(null);
        tac.setOnAction(null);
        license.setOnAction(null);
        webpage.setOnAction(null);
        dao.setOnAction(null);
        sourceCode.setOnAction(null);
        community.setOnAction(null);
        contribute.setOnAction(null);
    }
}
