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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.desktop.common.view.View;
import bisq.desktop.components.table.BisqTableView;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProfileCardOffersView extends View<VBox, ProfileCardOffersModel, ProfileCardOffersController> {
    private final BisqTableView<ListItem> tableView;

    public ProfileCardOffersView(ProfileCardOffersModel model,
                                 ProfileCardOffersController controller) {
        super(new VBox(), model, controller);

        VBox vBox = new VBox();
        vBox.setFillWidth(true);
        vBox.getStyleClass().add("header");
        tableView = new BisqTableView<>(model.getListItems());
        tableView.getStyleClass().addAll("reputation-table", "rich-table-view");
        tableView.allowVerticalScrollbar();
        configTableView();
        root.getChildren().addAll(vBox, tableView);
        root.setPadding(new Insets(20, 0, 0, 0));
        root.getStyleClass().add("reputation");
    }

    @Override
    protected void onViewAttached() {
        tableView.initialize();
    }

    @Override
    protected void onViewDetached() {
        tableView.dispose();
    }

    private void configTableView() {

    }

    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    @Getter
    static class ListItem {
        public ListItem() {

        }
    }
}
