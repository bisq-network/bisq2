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

package bisq.desktop.main.content.components;

import bisq.bonded_roles.BondedRoleType;
import bisq.desktop.common.utils.ImageUtil;
import bisq.desktop.components.controls.BisqTooltip;
import bisq.i18n.Res;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;

import java.util.List;

public class BondedRoleBadge extends HBox {
    private final BisqTooltip tooltip = new BisqTooltip();

    public BondedRoleBadge(boolean useLargeIcon) {
        ImageView badgeIcon;
        if (useLargeIcon) {
            badgeIcon = ImageUtil.getImageViewById("moderator-badge-large");
            setAlignment(Pos.BOTTOM_LEFT);
            setPadding(new Insets(0, -5, 5, 0));
        } else {
            badgeIcon = ImageUtil.getImageViewById("moderator-badge");
            setPadding(new Insets(0.5, -6, 0, 0));
        }
        getChildren().add(badgeIcon);

        setVisible(false);
        setManaged(false);

        Tooltip.install(this, tooltip);
    }

    public void setUserProfileBondedRoles(List<BondedRoleType> userBondedRoles) {
        boolean hasBondedRole = !userBondedRoles.isEmpty();
        setVisible(hasBondedRole);
        setManaged(hasBondedRole);
        String bondedRoleBadgeTooltip =
                userBondedRoles.contains(BondedRoleType.MEDIATOR) && userBondedRoles.contains(BondedRoleType.MODERATOR)
                        ? Res.get("user.profileCard.bondedRoleBadge.MediatorAndModerator")
                        : userBondedRoles.contains(BondedRoleType.MODERATOR) ? Res.get("user.profileCard.bondedRoleBadge.Moderator")
                        : userBondedRoles.contains(BondedRoleType.MEDIATOR) ? Res.get("user.profileCard.bondedRoleBadge.Mediator")
                        : "";
        tooltip.setText(hasBondedRole ? bondedRoleBadgeTooltip : "");
    }

    public void dispose() {
        Tooltip.uninstall(this, tooltip);
    }
}
