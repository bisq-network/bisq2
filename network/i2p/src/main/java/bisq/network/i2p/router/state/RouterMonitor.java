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

package bisq.network.i2p.router.state;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.timer.Scheduler;
import lombok.extern.slf4j.Slf4j;
import net.i2p.router.CommSystemFacade;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;
import net.i2p.router.TunnelManagerFacade;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class RouterMonitor implements RouterObserver {
    private final Runnable shutdownTask;
    private final AtomicReference<NetworkState> networkState = new AtomicReference<>(NetworkState.NEW);
    private final AtomicReference<ProcessState> processState = new AtomicReference<>(ProcessState.NEW);
    private final AtomicReference<RouterState> routerState = new AtomicReference<>(RouterState.NEW);
    private final AtomicReference<CommSystemFacade.Status> rawNetworkState = new AtomicReference<>(null);

    private final Observable<NetworkState> networkStateObservable = new Observable<>(networkState.get());
    private final Observable<ProcessState> processStateObservable = new Observable<>(processState.get());
    private final Observable<RouterState> routerStateObservable = new Observable<>(routerState.get());
    private final Observable<TunnelInfo> tunnelInfo = new Observable<>(new TunnelInfo());

    private final Router router;
    private final RouterContext routerContext;
    private volatile Scheduler scheduler;
    private volatile boolean isShutdownInProgress;

    public RouterMonitor(Router router) {
        this.router = router;
        routerContext = router.getContext();

        shutdownTask = () -> {
            if (processState.get() != ProcessState.STOPPED && processState.get() != ProcessState.FAILED) {
                setProcessState(ProcessState.STOPPING);
            }
            updateRouterState();
        };
        routerContext.addShutdownTask(shutdownTask);
        routerContext.addFinalShutdownTask(() -> {
            if (processState.get() != ProcessState.STOPPED && processState.get() != ProcessState.FAILED) {
                setProcessState(ProcessState.STOPPED);
            }
            setNetworkState(NetworkState.DISCONNECTED);
            updateRouterState();
        });
    }

    public void startPolling() {
        scheduler = Scheduler.run(this::updateStates)
                .host(this)
                .runnableName("updateState")
                .periodically(1, TimeUnit.SECONDS);
    }

    public void startShutdown() {
        setProcessState(ProcessState.STOPPING);
        updateStates();
    }

    public void shutdown() {
        if (router == null || isShutdownInProgress) {
            return;
        }
        updateStates();

        isShutdownInProgress = true;

        if (scheduler != null) {
            scheduler.stop();
        }

        routerContext.removeShutdownTask(shutdownTask);
        tunnelInfo.set(new TunnelInfo());
    }

    @Override
    public ReadOnlyObservable<ProcessState> getProcessState() {
        return processStateObservable;
    }

    @Override
    public ReadOnlyObservable<NetworkState> getNetworkState() {
        return networkStateObservable;
    }

    @Override
    public ReadOnlyObservable<RouterState> getRouterState() {
        return routerStateObservable;
    }

    @Override
    public ReadOnlyObservable<TunnelInfo> getTunnelInfo() {
        return tunnelInfo;
    }

    public void handleRouterException(Exception exception) {
        log.error("Router exception", exception);
        setProcessState(ProcessState.FAILED);
        updateRouterState();
    }

    private void updateStates() {
        if (isShutdownInProgress) {
            return;
        }
        updateNetworkState();
        updateProcessState();
        updateRouterState();
        updateTunnelInfo(routerContext);
    }

    private void updateNetworkState() {
        CommSystemFacade commSystemFacade = routerContext.commSystem();
        if (commSystemFacade == null) return;
        CommSystemFacade.Status value = commSystemFacade.getStatus();
        if (value == null) return;
        setRawNetworkState(value);
        NetworkState networkState = toNetworkState(value);
        setNetworkState(networkState);
    }

    private void updateProcessState() {
        // Avoid regressions: only move forward in state transitions
        ProcessState current = processState.get();
        if (current == ProcessState.FAILED) {
            return;
        }

        if (!router.isAlive()) {
            // Router is not alive yet → either still starting or stopped
            if (current == ProcessState.NEW || current == ProcessState.STARTING) {
                setProcessState(ProcessState.STARTING);
            } else if (current == ProcessState.STOPPING || current == ProcessState.STOPPED) {
                setProcessState(ProcessState.STOPPED);
            }
            return;
        }

        // Router thread is alive → initialization in progress
        if (current == ProcessState.NEW || current == ProcessState.STARTING) {
            setProcessState(ProcessState.INITIALIZING);
        }

        // isRunning: if router is RUNNING, i. e NetDB and Expl. tunnels are ready.
        if (!router.isRunning()) {
            return;
        }

        // RouterInfo becomes available → fully running
        if (router.getRouterInfo() != null) {
            setProcessState(ProcessState.RUNNING);
        }
    }

    private void updateRouterState() {
        RouterState value = switch (processState.get()) {
            case NEW -> RouterState.NEW;
            case STARTING, INITIALIZING -> RouterState.STARTING;
            case RUNNING -> switch (networkState.get()) {
                case NEW -> RouterState.RUNNING_DISCONNECTED; // Not expected
                case OK -> RouterState.RUNNING_OK;
                case TESTING -> RouterState.RUNNING_TESTING;
                case FIREWALLED -> RouterState.RUNNING_FIREWALLED;
                case UNKNOWN_SIGNAL, DISCONNECTED -> RouterState.RUNNING_DISCONNECTED;
            };
            case STOPPING -> RouterState.STOPPING;
            case STOPPED -> RouterState.STOPPED;
            case FAILED -> RouterState.FAILED;
        };
        setRouterState(value);
    }

    private void updateTunnelInfo(RouterContext routerContext) {
        TunnelManagerFacade tunnelManager = routerContext.tunnelManager();
        if (tunnelManager != null) {
            TunnelInfo value = new TunnelInfo(tunnelManager.getInboundClientTunnelCount(),
                    tunnelManager.getOutboundTunnelCount(),
                    tunnelManager.getOutboundClientTunnelCount());
            if (!tunnelInfo.get().equals(value)) {
                log.info("Update {}", value);
            }
            tunnelInfo.set(value);
        }
    }

    private static NetworkState toNetworkState(CommSystemFacade.Status status) {
        if (status == null) return NetworkState.UNKNOWN_SIGNAL;

        return switch (status) {
            // Fully usable (at least one family OK)
            case OK,
                 IPV4_OK_IPV6_UNKNOWN,
                 IPV4_OK_IPV6_FIREWALLED,
                 IPV4_UNKNOWN_IPV6_OK,
                 IPV4_FIREWALLED_IPV6_OK,
                 IPV4_DISABLED_IPV6_OK,
                 IPV4_SNAT_IPV6_OK -> NetworkState.OK;

            // Still probing / mixed with "Testing"
            case IPV4_SNAT_IPV6_UNKNOWN,
                 IPV4_FIREWALLED_IPV6_UNKNOWN,
                 IPV4_UNKNOWN_IPV6_FIREWALLED,
                 IPV4_DISABLED_IPV6_UNKNOWN,
                 UNKNOWN -> // label is "Testing"
                    NetworkState.TESTING;

            // Clearly limited inbound / no confirmed OK
            // "Firewalled"
            // "Symmetric NAT"
            case REJECT_UNSOLICITED,
                 DIFFERENT,
                 IPV4_DISABLED_IPV6_FIREWALLED ->  // v4 disabled, v6 firewalled
                    NetworkState.FIREWALLED;
            case DISCONNECTED -> NetworkState.DISCONNECTED;
            case HOSED -> // "Port Conflict" (treat as effectively firewalled/limited)
                    NetworkState.FIREWALLED;
        };
    }

    private void setRawNetworkState(CommSystemFacade.Status value) {
        if (value != rawNetworkState.get()) {
            log.info("Update rawNetworkState: {}", value);
        }
        rawNetworkState.set(value);
    }

    private void setNetworkState(NetworkState value) {
        if (networkState.get() == NetworkState.DISCONNECTED) {
            return;
        }
        if (value != networkState.get()) {
            log.info("Update networkState: {}", value);
        }
        networkState.set(value);
        networkStateObservable.set(value);
    }

    private void setProcessState(ProcessState value) {
        if (processState.get() == ProcessState.FAILED || processState.get() == ProcessState.STOPPED) {
            return;
        }
        if (value != processState.get()) {
            log.info("Update processState: {}", value);
        }
        processState.set(value);
        processStateObservable.set(value);
    }

    private void setRouterState(RouterState value) {
        if (routerState.get() == RouterState.FAILED || routerState.get() == RouterState.STOPPED) {
            return;
        }
        if (value != routerState.get()) {
            log.info("Update routerState: {}", value);
        }
        routerState.set(value);
        routerStateObservable.set(value);
    }
}