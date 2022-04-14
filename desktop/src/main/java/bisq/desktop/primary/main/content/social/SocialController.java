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

package bisq.desktop.primary.main.content.social;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.social.chat.ChatController;
import bisq.desktop.primary.main.content.social.education.EducationController;
import bisq.desktop.primary.main.content.social.events.EventsController;
import bisq.desktop.primary.main.content.social.exchange.ExchangeController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SocialController extends TabController<SocialModel> {
    private final DefaultApplicationService applicationService;
    @Getter
    private final SocialView view;
    private Pin selectedUserProfilePin;

    public SocialController(DefaultApplicationService applicationService) {
        super(new SocialModel(applicationService.getUserProfileService()), NavigationTarget.SOCIAL);

        this.applicationService = applicationService;

        view = new SocialView(model, this);
    }

    @Override
    public void onActivate() {
        selectedUserProfilePin = FxBindings.bind(model.getSelectedUserProfile())
                .to(applicationService.getUserProfileService().getPersistableStore().getSelectedUserProfile());
    }

    @Override
    public void onDeactivate() {
        selectedUserProfilePin.unbind();
    }


    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case EXCHANGE -> {
                return Optional.of(new ExchangeController(applicationService));
            }
            case CHAT -> {
                return Optional.of(new ChatController(applicationService));
            }
            case EDUCATION -> {
                return Optional.of(new EducationController(applicationService));
            }
            case EVENTS -> {
                return Optional.of(new EventsController(applicationService));
            }

            default -> {
                return Optional.empty();
            }
        }
    }
}
