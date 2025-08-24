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
import bisq.common.observable.Pin;
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
public class RouterStateObserver {
    public enum NetworkState {
        NEW,
        UNKNOWN,     // no reliable signal yet
        TESTING,     // bootstrapping / mixed "Testing"
        OK,          // at least one family (v4 or v6) confirmed OK
        FIREWALLED,  // reachable only in limited ways (no confirmed OK)
        DISCONNECTED // no connectivity
    }

    public enum ProcessState {
        NEW,
        STARTING,
        INITIALIZING,
        RUNNING,
        STOPPING,
        STOPPED,
        FAILED
    }

    // High level state combining process and network state
    public enum State {
        NEW,
        STARTING,
        RUNNING_TESTING,
        RUNNING_OK,
        RUNNING_FIREWALLED,
        RUNNING_DISCONNECTED,
        STOPPING,
        STOPPED,
        FAILED
    }

    private final Runnable shutdownTask;
    private final AtomicReference<NetworkState> networkState = new AtomicReference<>(NetworkState.NEW);
    private final AtomicReference<ProcessState> processState = new AtomicReference<>(ProcessState.NEW);
    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    private final Observable<State> stateObservable = new Observable<>(State.NEW);
    private final Observable<Integer> outboundTunnelCount = new Observable<>(0);

    private volatile Router router;
    private volatile Scheduler scheduler;
    private volatile Pin statusPin;

    RouterStateObserver() {
        shutdownTask = () -> processState.set(ProcessState.STOPPING);
    }

    void start(Router router) {
        this.router = router;
        RouterContext routerContext = router.getContext();

        routerContext.addShutdownTask(shutdownTask);
        routerContext.addFinalShutdownTask(() -> processState.set(ProcessState.STOPPED));

        scheduler = Scheduler.run(() -> {
                    updateNetworkState();
                    updateProcessState();
                    updateState();
                    TunnelManagerFacade tunnelManager = routerContext.tunnelManager();
                    if (tunnelManager != null) {
                        outboundTunnelCount.set(tunnelManager.getOutboundTunnelCount());
                    }
                })
                .host(this)
                .runnableName("updateState")
                .periodically(1, TimeUnit.SECONDS);
    }

    void shutdown() {
        if (statusPin != null) {
            statusPin.unbind();
        }
        if (scheduler != null) {
            scheduler.stop();
        }

        router.getContext().removeShutdownTask(shutdownTask);

        if (state.get() != State.STOPPED && state.get() != State.FAILED) {
            processState.set(ProcessState.STOPPED);
        }
    }

    ReadOnlyObservable<State> getState() {
        return stateObservable;
    }

    ReadOnlyObservable<Integer> getOutboundTunnelCount() {
        return outboundTunnelCount;
    }

    void handleRouterException(Exception exception) {
        processState.set(ProcessState.FAILED);
        updateState();
    }

    private void updateNetworkState() {
        CommSystemFacade.Status status = router.getContext().commSystem().getStatus();
        networkState.set(toNetworkState(status));
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
                processState.compareAndSet(current, ProcessState.STARTING);
            } else if (current == ProcessState.STOPPING || current == ProcessState.STOPPED) {
                processState.set(ProcessState.STOPPED);
            }
            return;
        }

        // Router thread is alive → initialization in progress
        if (current == ProcessState.NEW || current == ProcessState.STARTING) {
            processState.set(ProcessState.INITIALIZING);
        }

        // isRunning: if router is RUNNING, i. e NetDB and Expl. tunnels are ready.
        if (!router.isRunning()) {
            return;
        }

        // RouterInfo becomes available → fully running
        if (router.getRouterInfo() != null) {
            processState.set(ProcessState.RUNNING);
        }
    }

    private void updateState() {
        state.set(switch (processState.get()) {
            case NEW -> State.NEW;
            case STARTING, INITIALIZING -> State.STARTING;
            case RUNNING -> switch (networkState.get()) {
                case NEW -> State.RUNNING_DISCONNECTED; // Not expected
                case OK -> State.RUNNING_OK;
                case TESTING -> State.RUNNING_TESTING;
                case FIREWALLED -> State.RUNNING_FIREWALLED;
                case UNKNOWN, DISCONNECTED -> State.RUNNING_DISCONNECTED;
            };
            case STOPPING -> State.STOPPING;
            case STOPPED -> State.STOPPED;
            case FAILED -> State.FAILED;
        });
        stateObservable.set(state.get());
    }

    private static NetworkState toNetworkState(CommSystemFacade.Status status) {
        if (status == null) return NetworkState.UNKNOWN;

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
}
