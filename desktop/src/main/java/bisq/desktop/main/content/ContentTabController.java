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

package bisq.desktop.main.content;

import bisq.bisq_easy.BisqEasyNotificationsService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class ContentTabController<M extends ContentTabModel> extends TabController<M> {
    protected final ServiceProvider serviceProvider;
    private final BisqEasyNotificationsService bisqEasyNotificationsService;
    private Pin isNotificationVisiblePin;

    public ContentTabController(M model, NavigationTarget host, ServiceProvider serviceProvider) {
        super(model, host);

        this.serviceProvider = serviceProvider;
        bisqEasyNotificationsService = serviceProvider.getBisqEasyService().getBisqEasyNotificationsService();
    }

    @Override
    public void onActivate() {
        isNotificationVisiblePin = FxBindings.bind(model.getIsNotificationVisible())
                .to(bisqEasyNotificationsService.getIsNotificationPanelVisible());
    }

    @Override
    public void onDeactivate() {
        isNotificationVisiblePin.unbind();
    }
}
