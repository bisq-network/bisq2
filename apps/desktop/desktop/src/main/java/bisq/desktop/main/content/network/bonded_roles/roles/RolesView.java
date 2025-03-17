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

package bisq.desktop.main.content.network.bonded_roles.roles;

import bisq.desktop.components.table.BisqTableColumn;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesListItem;
import bisq.desktop.main.content.network.bonded_roles.BondedRolesView;
import bisq.i18n.Res;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;

@Slf4j
public class RolesView extends BondedRolesView<RolesModel, RolesController> {
    public RolesView(RolesModel model, RolesController controller, VBox tabControllerRoot) {
        super(model, controller, tabControllerRoot);
    }

    @Override
    protected String getTableHeadline() {
        return Res.get("user.bondedRoles.table.headline.roles");
    }

    @Override
    protected String getVerificationHeadline() {
        return Res.get("user.bondedRoles.verification.howTo.roles");
    }

    @Override
    protected void configTableView() {
        richTableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.userProfile"))
                .left()
                .minWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getUserName))
                .valueSupplier(BondedRolesListItem::getUserName)
                .setCellFactory(getUserProfileCellFactory())
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.role"))
                .fixWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getRoleTypeString))
                .valueSupplier(BondedRolesListItem::getRoleTypeString)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.bondUserName"))
                .minWidth(200)
                .comparator(Comparator.comparing(BondedRolesListItem::getBondUserName))
                .valueSupplier(BondedRolesListItem::getBondUserName)
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.profileId"))
                .minWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getUserProfileId))
                .valueSupplier(BondedRolesListItem::getUserProfileId)
                .setCellFactory(getUserProfileIdCellFactory())
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.signature"))
                .minWidth(150)
                .comparator(Comparator.comparing(BondedRolesListItem::getSignature))
                .valueSupplier(BondedRolesListItem::getSignature)
                .setCellFactory(getSignatureCellFactory())
                .build());
        richTableView.getColumns().add(new BisqTableColumn.Builder<BondedRolesListItem>()
                .title(Res.get("user.bondedRoles.table.columns.isBanned"))
                .right()
                .fixWidth(90)
                .comparator(Comparator.comparing(BondedRolesListItem::getIsBanned))
                .valueSupplier(BondedRolesListItem::getIsBanned)
                .build());
    }
}
