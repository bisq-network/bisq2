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

package bisq.bi2p.ui;

import bisq.bi2p.common.threading.UIClock;
import bisq.bi2p.common.threading.UIThread;
import bisq.bi2p.service.I2PRouterService;
import bisq.common.file.FileReaderUtils;
import bisq.common.observable.Pin;
import bisq.common.util.MathUtils;
import bisq.i18n.Res;
import bisq.network.i2p.router.state.NetworkState;
import bisq.network.i2p.router.state.ProcessState;
import bisq.network.i2p.router.state.RouterState;
import bisq.network.i2p.router.state.TunnelInfo;
import javafx.application.Application;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static bisq.network.i2p.router.state.RouterState.RUNNING_OK;


@Slf4j
public class Bi2pController {
    @Getter
    private final Bi2pView view;
    private final Bi2pModel model;
    private final I2PRouterService service;
    private final Runnable shutdownHandler;
    private Pin statePin, throwablePin, tunnelInfoPin, processStatePin, networkStatePin;
    private Runnable secondTickListener;

    public Bi2pController(double stageWidth, double stageHeight, I2PRouterService service, Runnable shutdownHandler) {
        this.service = service;
        this.shutdownHandler = shutdownHandler;

        model = new Bi2pModel(stageWidth, stageHeight, getVersion());
        view = new Bi2pView(this, model);

        UIClock.initialize();
    }

    public void onApplicationReady(Application.Parameters parameters) {
    }

    public void onActivate() {
        model.getRouterSetupDetails().set(Res.get("bi2p.routerSetupDetails",
                model.getVersion(),
                service.getI2cpHost(),
                String.valueOf(service.getI2cpPort())));
        model.getDateDirPath().set(service.getI2pDirPath().toAbsolutePath().toString());

        model.getButtonLabel().set(Res.get("bi2p.stop"));
        model.getButtonDisabled().set(false);
        model.getRouterStateString().set(Res.get("bi2p.routerState." + RouterState.STARTING.name()));

        processStatePin = service.getProcessState().addObserver(value -> UIThread.run(this::updateRouterStateDetails));
        networkStatePin = service.getNetworkState().addObserver(value -> UIThread.run(this::updateRouterStateDetails));
        tunnelInfoPin = service.getTunnelInfo().addObserver(value -> UIThread.run(this::updateRouterStateDetails));

        statePin = service.getState().addObserver(state -> {
            UIThread.run(() -> {
                if (state != null) {
                    model.getRouterState().set(state);
                    model.getRouterStateString().set(Res.get("bi2p.routerState." + state.name()));
                    switch (state) {
                        case NEW, STARTING, RUNNING_TESTING -> {
                            model.getButtonLabel().set(Res.get("bi2p.stop"));
                            model.getButtonDisabled().set(false);
                        }
                        case RUNNING_OK, RUNNING_FIREWALLED -> {
                            model.getButtonLabel().set(Res.get("bi2p.stop"));
                            model.getButtonDisabled().set(false);
                            stopDurationUpdate();
                        }
                        case STOPPING -> {
                            model.getButtonDisabled().set(true);
                            stopDurationUpdate();
                        }
                        case RUNNING_DISCONNECTED, STOPPED, FAILED -> {
                            model.getButtonLabel().set(Res.get("bi2p.shutdown"));
                            model.getButtonDisabled().set(false);
                            stopDurationUpdate();
                        }
                    }
                }
            });
        });
        throwablePin = service.getThrowable().addObserver(throwable -> {
            UIThread.run(() -> {
                if (throwable != null) {
                    model.getErrorMessage().set(throwable.getLocalizedMessage());
                }
            });
        });

        long startTime = System.currentTimeMillis();
        secondTickListener = () -> {
            double maxExpectedStartupTime = TimeUnit.SECONDS.toMillis(model.getMaxExpectedStartupTime());
            double passed = System.currentTimeMillis() - startTime;
            if (passed > maxExpectedStartupTime) {
                model.getStartupExpectedTimePassed().set(true);
            }
            double progress = Math.min(1, passed / maxExpectedStartupTime);
            model.getProgress().set(progress);
            updateStartupDuration(MathUtils.roundDoubleToInt(passed / 1000d));
        };
        UIClock.addOnSecondTickListener(secondTickListener);

        model.getErrorHeadline().set(Res.get("bi2p.errorHeadline"));

        view.onViewAttached();
    }

    private void updateRouterStateDetails() {
        int outboundTunnelCount = 0;
        int outboundClientTunnelCount = 0;
        int inboundClientTunnelCount = 0;
        String processStateString = "N/A";
        String networkStateString = "N/A";

        TunnelInfo tunnelInfo = service.getTunnelInfo().get();
        if (tunnelInfo != null) {
            outboundTunnelCount = tunnelInfo.getOutboundTunnelCount();
            outboundClientTunnelCount = tunnelInfo.getOutboundClientTunnelCount();
            inboundClientTunnelCount = tunnelInfo.getInboundClientTunnelCount();

        }
        ProcessState processState = service.getProcessState().get();
        if (processState != null) {
            processStateString = Res.get("bi2p.processState." + processState.name());
        }
        NetworkState networkState = service.getNetworkState().get();
        if (networkState != null) {
            networkStateString = Res.get("bi2p.networkState." + networkState.name());
        }

        model.getRouterStateDetails().set(Res.get("bi2p.routerStateDetails",
                processStateString,
                networkStateString,
                outboundTunnelCount,
                outboundClientTunnelCount,
                inboundClientTunnelCount));
    }

    public void onDeactivate() {
        statePin.unbind();
        throwablePin.unbind();
        tunnelInfoPin.unbind();
        processStatePin.unbind();
        networkStatePin.unbind();
        UIClock.removeOnSecondTickListener(secondTickListener);

        view.onViewDetached();
    }

    void onButtonClicked() {
        RouterState routerState = service.getState().get();
        if (routerState == null) {
            log.warn("RouterState is expected to be not null");
            return;
        }
        if (routerState.ordinal() <= RUNNING_OK.ordinal()) {
            service.shutdown();
        } else {
            shutdownHandler.run();
        }
    }

    private String getVersion() {
        try {
            return FileReaderUtils.readStringFromResource("bi2p/version.txt");
        } catch (IOException e) {
            log.error("Could not read version. Run gradle task `copyI2pRouterAppVersionToResources`.", e);
            return "n/a";
        }
    }

    private void updateStartupDuration(long duration) {
        model.setStartupDuration(duration);
        model.getStartupDurationString().set(Res.get("bi2p.startupDuration", duration));
    }

    private void stopDurationUpdate() {
        UIClock.removeOnSecondTickListener(secondTickListener);
        model.getStartupDurationString().set(Res.get("bi2p.finalStartupDuration", model.getStartupDuration()));
    }

}
