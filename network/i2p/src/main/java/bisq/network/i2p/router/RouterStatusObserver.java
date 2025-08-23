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

package bisq.network.i2p.router;

import bisq.common.observable.Observable;
import bisq.common.timer.Scheduler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.RouterContext;

import java.util.concurrent.TimeUnit;

@Slf4j
class RouterStatusObserver {
    @Getter
    private final Observable<CommSystemFacade.Status> routerStatus = new Observable<>();
    private RouterContext routerContext;

    RouterStatusObserver() {
    }

    void start(RouterContext routerContext) {
        this.routerContext = routerContext;
        Scheduler.run(this::checkRouterStatus)
                .host(this)
                .runnableName("checkRouterStatus")
                .periodically(30, TimeUnit.SECONDS);
    }

    private void checkRouterStatus() {
        CommSystemFacade.Status status = routerContext.commSystem().getStatus();
        logStatus(status);
        routerStatus.set(status);
    }

    private static void logStatus(CommSystemFacade.Status status) {
        switch (status) {
            case OK,
                 IPV4_OK_IPV6_UNKNOWN,
                 IPV4_OK_IPV6_FIREWALLED,
                 IPV4_UNKNOWN_IPV6_OK,
                 IPV4_FIREWALLED_IPV6_OK,
                 IPV4_DISABLED_IPV6_OK,
                 IPV4_SNAT_IPV6_OK,
                 IPV4_SNAT_IPV6_UNKNOWN,
                 IPV4_FIREWALLED_IPV6_UNKNOWN,
                 REJECT_UNSOLICITED,
                 IPV4_UNKNOWN_IPV6_FIREWALLED,
                 IPV4_DISABLED_IPV6_UNKNOWN,
                 IPV4_DISABLED_IPV6_FIREWALLED -> log.info("I2P Router status - {}", status.toStatusString());

            case DIFFERENT,
                 DISCONNECTED,
                 HOSED,
                 UNKNOWN -> log.warn("I2P Router status - {}", status.toStatusString());
        }
    }
}
