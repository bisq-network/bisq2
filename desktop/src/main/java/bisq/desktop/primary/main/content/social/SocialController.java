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
import bisq.desktop.common.view.CachingController;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.common.view.TabController;
import bisq.desktop.primary.main.content.social.discussion.DiscussionsController;
import bisq.desktop.primary.main.content.social.education.EducationController;
import bisq.desktop.primary.main.content.social.events.EventsController;
import bisq.desktop.primary.main.content.social.gettingStarted.GettingStartedController;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public class SocialController extends TabController<SocialModel> implements CachingController {
    private final DefaultApplicationService applicationService;
    @Getter
    private final SocialView view;

    public SocialController(DefaultApplicationService applicationService) {
        super(new SocialModel(), NavigationTarget.SOCIAL);

        this.applicationService = applicationService;

        view = new SocialView(model, this);
    }

    @Override
    public void onActivate() {
    }

    @Override
    public void onDeactivate() {
    }

    @Override
    protected Optional<? extends Controller> createController(NavigationTarget navigationTarget) {
        switch (navigationTarget) {
            case GETTING_STARTED -> {
                return Optional.of(new GettingStartedController(applicationService));
            }
            case DISCUSS -> {
                return Optional.of(new DiscussionsController(applicationService));
            }
            case LEARN -> {
                return Optional.of(new EducationController(applicationService));
            }
            case CONNECT -> {
                return Optional.of(new EventsController(applicationService));
            }
            default -> {
                return Optional.empty();
            }
        }
    }
}
