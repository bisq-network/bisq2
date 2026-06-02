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

package bisq.bisq_easy;

import bisq.notifications.Notification;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Lightweight {@link Notification} carrying a per-trade mobile push payload built by
 * {@link BisqEasyMobileTradeNotificationService}. Not persisted — only used as a
 * transient envelope between the trade-state observer and the mobile relay path.
 */
@Getter
@ToString
@EqualsAndHashCode
public final class MobileTradeNotification implements Notification {
    private final String id;
    private final String title;
    private final String message;

    public MobileTradeNotification(String id, String title, String message) {
        this.id = id;
        this.title = title;
        this.message = message;
    }

    @Override
    public Category getCategory() {
        return Category.TRADE_UPDATE;
    }
}
