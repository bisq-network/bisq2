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

package bisq.desktop.main.banner;

import bisq.desktop.common.view.Model;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
public class BannerNotificationModel implements Model {
    @Setter
    private String pendingInventoryRequests;
    @Setter
    private String inventoryRequestsInfo;
    @Setter
    private String maxInventoryRequests;
    private final BooleanProperty inventoryDataChangeFlag = new SimpleBooleanProperty();
    private final BooleanProperty initialInventoryRequestsCompleted = new SimpleBooleanProperty();

    public BannerNotificationModel() {
    }
}
