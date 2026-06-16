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

package bisq.desktop_ui_harness_app;

import bisq.desktop.common.view.View;
import bisq.desktop.common.view.ViewLifecycleObserver;
import bisq.desktop.main.content.chat.message_container.ChatMessageContainerAutomationBinder;
import bisq.desktop.main.left.LeftNavAutomationBinder;
import bisq.desktop.overlay.onboarding.create_profile.CreateProfileAutomationBinder;
import bisq.desktop.overlay.onboarding.welcome.WelcomeAutomationBinder;
import bisq.desktop.overlay.tac.TacAutomationBinder;
import bisq.desktop.overlay.tac.legal_terms.TacLegalTermsAutomationBinder;
import bisq.desktop.overlay.tac.risk_ack.TacRiskAckAutomationBinder;
import bisq.desktop.splash.SplashAutomationBinder;

import java.util.List;

final class DesktopAutomationViewObserver implements ViewLifecycleObserver {
    private final List<DesktopAutomationViewBinder<?>> binders;

    DesktopAutomationViewObserver() {
        this(List.of(
                new ChatMessageContainerAutomationBinder(),
                new LeftNavAutomationBinder(),
                new SplashAutomationBinder(),
                new TacAutomationBinder(),
                new TacRiskAckAutomationBinder(),
                new TacLegalTermsAutomationBinder(),
                new WelcomeAutomationBinder(),
                new CreateProfileAutomationBinder()
        ));
    }

    DesktopAutomationViewObserver(List<DesktopAutomationViewBinder<?>> binders) {
        this.binders = List.copyOf(binders);
    }

    @Override
    public void onViewAttached(View<?, ?, ?> view) {
        bind(view);
    }

    private void bind(View<?, ?, ?> view) {
        for (DesktopAutomationViewBinder<?> binder : binders) {
            if (binder.tryBind(view)) {
                return;
            }
        }
    }
}
