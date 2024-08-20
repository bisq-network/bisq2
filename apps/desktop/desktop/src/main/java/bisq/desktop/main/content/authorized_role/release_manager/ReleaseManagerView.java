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
import bisq.desktop.components.table.BisqTableColumns;
import bisq.desktop.components.table.DateTableItem;
import bisq.desktop.components.table.RichTableView;
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
    private final RichTableView<ListItem> richTableView;

    public ReleaseManagerView(ReleaseManagerModel model, ReleaseManagerController controller, Pane roleInfo) {
        super(new VBox(10), model, controller);

        root.setPadding(new Insets(0, 40, 40, 40));
        root.setAlignment(Pos.TOP_LEFT);

        Label headline = new Label(Res.get("authorizedRole.releaseManager.headline"));
        headline.getStyleClass().add("large-thin-headline");

        releaseNotes = new MaterialTextArea(Res.get("authorizedRole.releaseManager.releaseNotes"));
        version = new MaterialTextField(Res.get("authorizedRole.releaseManager.version"));
        isPreReleaseCheckBox = new CheckBox(Res.get("authorizedRole.releaseManager.isPreRelease"));
        isLauncherUpdateCheckBox = new CheckBox(Res.get("authorizedRole.releaseManager.isLauncherUpdate"));

        sendButton = new Button(Res.get("authorizedRole.releaseManager.send"));
        sendButton.setDefaultButton(true);
        sendButton.setAlignment(Pos.BOTTOM_RIGHT);

        richTableView = new RichTableView<>(model.getListItems(),
                Res.get("authorizedRole.releaseManager.table.headline"));
        configTableView();

        roleInfo.setPadding(new Insets(0));

        VBox.setMargin(sendButton, new Insets(10, 0, 0, 0));
        VBox.setMargin(isPreReleaseCheckBox, new Insets(10, 0, 0, 0));
        VBox.setMargin(roleInfo, new Insets(20, 0, 0, 0));
        this.root.getChildren().addAll(headline,
                releaseNotes,
                version,
                isPreReleaseCheckBox,
                isLauncherUpdateCheckBox,
                sendButton,
                richTableView,
                roleInfo);
    }

    @Override
    protected void onViewAttached() {
        richTableView.initialize();
        version.textProperty().bindBidirectional(model.getVersion());
        releaseNotes.textProperty().bindBidirectional(model.getReleaseNotes());
        sendButton.disableProperty().bind(model.getActionButtonDisabled());
        isPreReleaseCheckBox.selectedProperty().bindBidirectional(model.getIsPreRelease());
        isLauncherUpdateCheckBox.selectedProperty().bindBidirectional(model.getIsLauncherUpdate());

        sendButton.setOnAction(e -> controller.onSendReleaseNotification());
    }

    @Override
    protected void onViewDetached() {
        richTableView.dispose();
        version.textProperty().unbindBidirectional(model.getVersion());
        releaseNotes.textProperty().unbindBidirectional(model.getReleaseNotes());
        sendButton.disableProperty().unbind();
        isPreReleaseCheckBox.selectedProperty().unbindBidirectional(model.getIsPreRelease());
        isLauncherUpdateCheckBox.selectedProperty().unbindBidirectional(model.getIsLauncherUpdate());

        sendButton.setOnAction(null);
    }

    protected void configTableView() {
        richTableView.getColumns().add(BisqTableColumns.getDateColumn(richTableView.getSortOrder()));

        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.releaseNotes"))
                .minWidth(200)
                .comparator(Comparator.comparing(ListItem::getReleaseNotes))
                .valueSupplier(ListItem::getReleaseNotes)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.version"))
                .minWidth(120)
                .comparator(Comparator.comparing(ListItem::getVersion))
                .valueSupplier(ListItem::getVersion)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .title(Res.get("authorizedRole.releaseManager.table.isPreRelease"))
                .minWidth(120)
                .valueSupplier(ListItem::getIsPreRelease)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .title(Res.get("authorizedRole.releaseManager.table.isLauncherUpdate"))
                .minWidth(120)
                .valueSupplier(ListItem::getIsLauncherUpdate)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .title(Res.get("authorizedRole.releaseManager.table.profileId"))
                .minWidth(150)
                .comparator(Comparator.comparing(ListItem::getReleaseManagerProfileId))
                .valueSupplier(ListItem::getReleaseManagerProfileId)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<ListItem>()
                .isSortable(false)
                .minWidth(200)
                .right()
                .setCellFactory(getRemoveItemCellFactory())
                .includeForCsv(false)
                .build());
    }

    private Callback<TableColumn<ListItem, ListItem>, TableCell<ListItem, ListItem>> getRemoveItemCellFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button(Res.get("data.remove"));

            @Override
            protected void updateItem(ListItem item, boolean empty) {
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

    @Getter
    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class ListItem implements DateTableItem {
        @EqualsAndHashCode.Include
        private final ReleaseNotification releaseNotification;

        private final String dateString, timeString, isLauncherUpdate, releaseNotes, version, isPreRelease, releaseManagerProfileId;
        private final long date;

        public ListItem(ReleaseNotification releaseNotification) {
            this.releaseNotification = releaseNotification;

            date = releaseNotification.getDate();
            dateString = DateFormatter.formatDate(date);
            timeString = DateFormatter.formatTime(date);
            isPreRelease = BooleanFormatter.toYesNo(releaseNotification.isPreRelease());
            isLauncherUpdate = BooleanFormatter.toYesNo(releaseNotification.isLauncherUpdate());
            releaseNotes = releaseNotification.getReleaseNotes();
            version = releaseNotification.getVersionString();
            releaseManagerProfileId = releaseNotification.getReleaseManagerProfileId();
        }
    }

}
