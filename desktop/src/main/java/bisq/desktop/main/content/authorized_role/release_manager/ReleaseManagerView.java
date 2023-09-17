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

package bisq.desktop.main.content.authorized_role.release_manager;

import bisq.bonded_roles.release.ReleaseNotification;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextArea;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.BisqTableView;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.presentation.formatters.BooleanFormatter;
import bisq.presentation.formatters.DateFormatter;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class ReleaseManagerView extends View<VBox, ReleaseManagerModel, ReleaseManagerController> {

    private final Button sendButton;
    private final MaterialTextArea releaseNotes;
    private final MaterialTextField version;
    private final CheckBox isPreReleaseCheckBox, isLauncherUpdateCheckBox;
    private final BisqTableView<ReleaseNotificationListItem> tableView;

    public ReleaseManagerView(ReleaseManagerModel model, ReleaseManagerController controller, Pane roleInfo) {
        super(new VBox(10), model, controller);

        this.root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("authorizedRole.releaseManager.headline"));
        headline.getStyleClass().add("bisq-text-headline-2");

        releaseNotes = new MaterialTextArea(Res.get("authorizedRole.releaseManager.releaseNotes"));
        version = new MaterialTextField(Res.get("authorizedRole.releaseManager.version"));
        isPreReleaseCheckBox = new CheckBox(Res.get("authorizedRole.releaseManager.isPreRelease"));
        isLauncherUpdateCheckBox = new CheckBox(Res.get("authorizedRole.releaseManager.isLauncherUpdate"));

        sendButton = new Button(Res.get("authorizedRole.releaseManager.send"));
        sendButton.setDefaultButton(true);
        sendButton.setAlignment(Pos.BOTTOM_RIGHT);

        Label tableHeadline = new Label(Res.get("authorizedRole.releaseManager.table.headline"));
        tableHeadline.getStyleClass().add("bisq-text-headline-2");

        tableView = new BisqTableView<>(model.getSortedListItems());
        tableView.setMinHeight(200);
        tableView.getStyleClass().add("user-bonded-roles-table-view");
        configTableView();

        VBox.setMargin(headline, new Insets(30, 0, 10, 0));
        VBox.setMargin(sendButton, new Insets(10, 0, 0, 0));
        VBox.setMargin(isPreReleaseCheckBox, new Insets(10, 0, 0, 0));
        VBox.setMargin(tableHeadline, new Insets(30, 0, 10, 0));
        VBox.setMargin(roleInfo, new Insets(20, 0, 0, 0));
        this.root.getChildren().addAll(headline,
                releaseNotes,
                version,
                isPreReleaseCheckBox,
                isLauncherUpdateCheckBox,
                sendButton,
                tableHeadline, tableView,
                roleInfo);
    }

    @Override
    protected void onViewAttached() {
        version.textProperty().bindBidirectional(model.getVersion());
        releaseNotes.textProperty().bindBidirectional(model.getReleaseNotes());
        sendButton.disableProperty().bind(model.getActionButtonDisabled());
        isPreReleaseCheckBox.selectedProperty().bindBidirectional(model.getIsPreRelease());
        isLauncherUpdateCheckBox.selectedProperty().bindBidirectional(model.getIsLauncherUpdate());

        sendButton.setOnAction(e -> controller.onSendReleaseNotification());
    }

    @Override
    protected void onViewDetached() {
        version.textProperty().unbindBidirectional(model.getVersion());
        releaseNotes.textProperty().unbindBidirectional(model.getReleaseNotes());
        sendButton.disableProperty().unbind();
        isPreReleaseCheckBox.selectedProperty().unbindBidirectional(model.getIsPreRelease());
        isLauncherUpdateCheckBox.selectedProperty().unbindBidirectional(model.getIsLauncherUpdate());

        sendButton.setOnAction(null);
    }

    protected void configTableView() {
        BisqTableColumn<ReleaseNotificationListItem> date = new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.date"))
                .left()
                .minWidth(180)
                .comparator(Comparator.comparing(ReleaseNotificationListItem::getDate).reversed())
                .valueSupplier(ReleaseNotificationListItem::getDateString)
                .build();
        tableView.getColumns().add(date);
        tableView.getSortOrder().add(date);

        tableView.getColumns().add(new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.releaseNotes"))
                .minWidth(200)
                .comparator(Comparator.comparing(ReleaseNotificationListItem::getReleaseNotes))
                .valueSupplier(ReleaseNotificationListItem::getReleaseNotes)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.version"))
                .minWidth(120)
                .comparator(Comparator.comparing(ReleaseNotificationListItem::getVersion))
                .valueSupplier(ReleaseNotificationListItem::getVersion)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .isSortable(false)
                .title(Res.get("authorizedRole.releaseManager.table.isPreRelease"))
                .minWidth(120)
                .valueSupplier(ReleaseNotificationListItem::getIsPreRelease)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .isSortable(false)
                .title(Res.get("authorizedRole.releaseManager.table.isLauncherUpdate"))
                .minWidth(120)
                .valueSupplier(ReleaseNotificationListItem::getIsLauncherUpdate)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.profileId"))
                .minWidth(150)
                .comparator(Comparator.comparing(ReleaseNotificationListItem::getReleaseManagerProfileId))
                .valueSupplier(ReleaseNotificationListItem::getReleaseManagerProfileId)
                .build());
        tableView.getColumns().add(new BisqTableColumn.Builder<ReleaseNotificationListItem>()
                .isSortable(false)
                .minWidth(200)
                .right()
                .setCellFactory(getRemoveItemCellFactory())
                .build());
    }

    private Callback<TableColumn<ReleaseNotificationListItem, ReleaseNotificationListItem>, TableCell<ReleaseNotificationListItem, ReleaseNotificationListItem>> getRemoveItemCellFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button(Res.get("data.remove"));

            @Override
            public void updateItem(final ReleaseNotificationListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty && controller.isRemoveButtonVisible(item.getReleaseNotification())) {
                    button.setOnAction(e -> controller.onRemoveReleaseNotification(item.getReleaseNotification()));
                    setGraphic(button);
                } else {
                    button.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }

    @EqualsAndHashCode
    @Getter
    @ToString
    public static class ReleaseNotificationListItem implements TableItem {
        private final ReleaseNotification releaseNotification;
        private final String dateString, isLauncherUpdate, releaseNotes, version, isPreRelease, releaseManagerProfileId;
        private final long date;

        public ReleaseNotificationListItem(ReleaseNotification releaseNotification) {
            this.releaseNotification = releaseNotification;
            date = releaseNotification.getDate();
            dateString = DateFormatter.formatDateTime(date);
            isPreRelease = BooleanFormatter.toYesNo(releaseNotification.isPreRelease());
            isLauncherUpdate = BooleanFormatter.toYesNo(releaseNotification.isLauncherUpdate());
            releaseNotes = releaseNotification.getReleaseNotes();
            version = releaseNotification.getVersionString();
            releaseManagerProfileId = releaseNotification.getReleaseManagerProfileId();
        }
    }
}
