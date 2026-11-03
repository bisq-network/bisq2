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

import bisq.chat.ChatChannelDomain;
import bisq.chat.notifications.ChatNotification;
import bisq.chat.notifications.ChatNotificationService;
import bisq.common.observable.Pin;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.TabButton;
import bisq.desktop.main.content.ContentTabController;
import bisq.desktop.main.content.mu_sig.trade.history.MuSigHistoryController;
import bisq.desktop.main.content.mu_sig.offer.my_offers.MuSigMyOffersController;
import bisq.desktop.main.content.mu_sig.offer.listing.MuSigOfferbookController;
import bisq.desktop.main.content.mu_sig.onboarding.MuSigDashboardController;
import bisq.desktop.main.content.mu_sig.trade.pending.MuSigPendingTradesController;
import bisq.desktop.navigation.NavigationTarget;
import bisq.mu_sig.MusigNotificationService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class MuSigController extends ContentTabController<MuSigModel> {
    @Getter
    private final MuSigView view;
    private final ChatNotificationService chatNotificationService;
    private final MusigNotificationService musigNotificationService;
    private Pin changedNotificationPin;

    public MuSigController(ServiceProvider serviceProvider) {
        super(new MuSigModel(), NavigationTarget.MU_SIG, serviceProvider);

        view = new MuSigView(model, this);

        chatNotificationService = serviceProvider.getChatService().getChatNotificationService();
        musigNotificationService = serviceProvider.getMuSigService().getMusigNotificationService();
    }

    @Override
    public void onActivate() {
        super.onActivate();

        chatNotificationService.getNotConsumedNotifications().forEach(this::handleNotification);
        changedNotificationPin = chatNotificationService.getChangedNotification().addObserver(this::handleNotification);
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();

        if (changedNotificationPin != null) {
            changedNotificationPin.unbind();
            changedNotificationPin = null;
        }
    }

    private void handleNotification(ChatNotification chatNotification) {
        if (chatNotification == null) {
            return;
        }

        UIThread.run(() -> {
            if (chatNotification.getChatChannelDomain() == ChatChannelDomain.MU_SIG_OFFERBOOK) {
                findTabButton(NavigationTarget.MU_SIG_OFFERBOOK).ifPresent(button ->
                        button.setNumNotifications(getNumNotifications(ChatChannelDomain.MU_SIG_OFFERBOOK)));
            }

            if (chatNotification.getChatChannelDomain() == ChatChannelDomain.MU_SIG_OPEN_TRADES) {
                findTabButton(NavigationTarget.MU_SIG_OPEN_TRADES).ifPresent(button ->
                        button.setNumNotifications(getNumNotifications(ChatChannelDomain.MU_SIG_OPEN_TRADES)));
            }
        });
    }

    private Optional<TabButton> findTabButton(NavigationTarget navigationTarget) {
        return model.getTabButtons().stream()
                .filter(tabButton ->
                        navigationTarget == tabButton.getNavigationTarget()).findAny();
    }

    private long getNumNotifications(ChatChannelDomain domain) {
        return musigNotificationService.getMusigNotConsumedNotifications()
                .filter(notification -> notification.getChatChannelDomain() == domain)
                .count();
        }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        return switch (navigationTarget) {
            case MU_SIG_DASHBOARD -> Optional.of(new MuSigDashboardController(serviceProvider));
            case MU_SIG_OFFERBOOK -> Optional.of(new MuSigOfferbookController(serviceProvider));
            case MU_SIG_MY_OFFERS -> Optional.of(new MuSigMyOffersController(serviceProvider));
            case MU_SIG_OPEN_TRADES -> Optional.of(new MuSigPendingTradesController(serviceProvider));
            case MU_SIG_HISTORY -> Optional.of(new MuSigHistoryController(serviceProvider));
            default -> Optional.empty();
        };
    }
}
