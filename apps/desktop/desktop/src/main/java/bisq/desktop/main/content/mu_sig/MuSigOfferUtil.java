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

package bisq.desktop.main.content.mu_sig;

import bisq.desktop.main.content.components.UserProfileDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class MuSigOfferUtil {
    public static Callback<TableColumn<MuSigOfferListItem, MuSigOfferListItem>,
            TableCell<MuSigOfferListItem, MuSigOfferListItem>> getUserProfileCellFactory() {
        return column -> new TableCell<>() {
            private UserProfileDisplay userProfileDisplay;

            @Override
            protected void updateItem(MuSigOfferListItem item, boolean empty) {
                super.updateItem(item, empty);

                if (item != null && !empty) {
                    userProfileDisplay = new UserProfileDisplay(item.getMakerUserProfile(), true, true);
                    userProfileDisplay.setReputationScore(item.getReputationScore());
                    setGraphic(userProfileDisplay);
                } else {
                    if (userProfileDisplay != null) {
                        userProfileDisplay.dispose();
                        userProfileDisplay = null;
                    }
                    setGraphic(null);
                }
            }
        };
    }
}
