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

package bisq.desktop.main.content.authorized_role.security_manager;

import bisq.bonded_roles.security_manager.difficulty_adjustment.AuthorizedDifficultyAdjustmentData;
import bisq.desktop.common.converters.Converters;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.desktop.components.controls.validator.NumberValidator;
import bisq.desktop.components.controls.validator.ValidatorBase;
import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.components.table.DateColumnUtil;
import bisq.desktop.components.table.DateTableItem;
import bisq.desktop.components.table.RichTableView;
import bisq.i18n.Res;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.presentation.formatters.DateFormatter;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class DifficultyAdjustmentView extends View<VBox, DifficultyAdjustmentModel, DifficultyAdjustmentController> {
    private static final ValidatorBase DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR =
            new NumberValidator(Res.get("authorizedRole.securityManager.difficultyAdjustment.invalid", NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT),
                    0, NetworkLoad.MAX_DIFFICULTY_ADJUSTMENT, false);

    private final Button difficultyAdjustmentButton;
    private final MaterialTextField difficultyAdjustmentFactor;
    private final RichTableView<DifficultyAdjustmentListItem> difficultyAdjustmentTableView;

    public DifficultyAdjustmentView(DifficultyAdjustmentModel model, DifficultyAdjustmentController controller) {
        super(new VBox(10), model, controller);

        root.setAlignment(Pos.TOP_LEFT);

        Label difficultyAdjustmentHeadline = new Label(Res.get("authorizedRole.securityManager.difficultyAdjustment.headline"));
        difficultyAdjustmentHeadline.getStyleClass().add("large-thin-headline");

        difficultyAdjustmentFactor = new MaterialTextField(Res.get("authorizedRole.securityManager.difficultyAdjustment.description"));
        difficultyAdjustmentFactor.setMaxWidth(400);
        difficultyAdjustmentFactor.setValidators(DIFFICULTY_ADJUSTMENT_FACTOR_VALIDATOR);

        difficultyAdjustmentButton = new Button(Res.get("authorizedRole.securityManager.difficultyAdjustment.button"));
        difficultyAdjustmentButton.setDefaultButton(true);

        difficultyAdjustmentTableView = new RichTableView<>(model.getDifficultyAdjustmentListItems(),
                Res.get("authorizedRole.securityManager.difficultyAdjustment.table.headline"));
        configDifficultyAdjustmentTableView();
        difficultyAdjustmentTableView.setMaxHeight(50);


        VBox.setMargin(difficultyAdjustmentButton, new Insets(0, 0, 10, 0));
        VBox.setVgrow(difficultyAdjustmentTableView, Priority.NEVER);
        this.root.getChildren().addAll(difficultyAdjustmentHeadline, difficultyAdjustmentFactor,
                difficultyAdjustmentButton, difficultyAdjustmentTableView);
    }

    @Override
    protected void onViewAttached() {
        difficultyAdjustmentTableView.initialize();
        Bindings.bindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor(),
                Converters.DOUBLE_STRING_CONVERTER);
        difficultyAdjustmentButton.disableProperty().bind(model.getDifficultyAdjustmentFactorButtonDisabled());
        difficultyAdjustmentButton.setOnAction(e -> controller.onPublishDifficultyAdjustmentFactor());
    }

    @Override
    protected void onViewDetached() {
        difficultyAdjustmentTableView.dispose();
        Bindings.unbindBidirectional(difficultyAdjustmentFactor.textProperty(), model.getDifficultyAdjustmentFactor());
        difficultyAdjustmentButton.disableProperty().unbind();
        difficultyAdjustmentButton.setOnAction(null);
    }

    private void configDifficultyAdjustmentTableView() {
        difficultyAdjustmentTableView.getColumns().add(DateColumnUtil.getDateColumn(difficultyAdjustmentTableView.getSortOrder()));
        difficultyAdjustmentTableView.getColumns().add(new BisqTableColumn.Builder<DifficultyAdjustmentListItem>()
                .title(Res.get("authorizedRole.securityManager.difficultyAdjustment.table.value"))
                .minWidth(150)
                .comparator(Comparator.comparing(DifficultyAdjustmentListItem::getDifficultyAdjustmentFactor))
                .valueSupplier(DifficultyAdjustmentListItem::getDifficultyAdjustmentFactorString)
                .build());
        difficultyAdjustmentTableView.getColumns().add(new BisqTableColumn.Builder<DifficultyAdjustmentListItem>()
                .isSortable(false)
                .minWidth(200)
                .right()
                .setCellFactory(getRemoveDifficultyAdjustmentCellFactory())
                .build());
    }

    private Callback<TableColumn<DifficultyAdjustmentView.DifficultyAdjustmentListItem, DifficultyAdjustmentView.DifficultyAdjustmentListItem>,
            TableCell<DifficultyAdjustmentView.DifficultyAdjustmentListItem, DifficultyAdjustmentView.DifficultyAdjustmentListItem>> getRemoveDifficultyAdjustmentCellFactory() {
        return column -> new TableCell<>() {
            private final Button button = new Button(Res.get("data.remove"));

            @Override
            protected void updateItem(DifficultyAdjustmentView.DifficultyAdjustmentListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty && controller.isRemoveDifficultyAdjustmentButtonVisible(item.getData())) {
                    button.setOnAction(e -> controller.onRemoveDifficultyAdjustmentListItem(item));
                    setGraphic(button);
                } else {
                    button.setOnAction(null);
                    setGraphic(null);
                }
            }
        };
    }


    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    @ToString
    public static class DifficultyAdjustmentListItem implements DateTableItem {
        @EqualsAndHashCode.Include
        private final AuthorizedDifficultyAdjustmentData data;

        private final long date;
        private final String dateString, timeString, difficultyAdjustmentFactorString;
        private final double difficultyAdjustmentFactor;

        public DifficultyAdjustmentListItem(AuthorizedDifficultyAdjustmentData data) {
            this.data = data;
            date = data.getDate();
            dateString = DateFormatter.formatDate(date);
            timeString = DateFormatter.formatTime(date);
            difficultyAdjustmentFactor = data.getDifficultyAdjustmentFactor();
            difficultyAdjustmentFactorString = String.valueOf(difficultyAdjustmentFactor);
        }
    }
}
