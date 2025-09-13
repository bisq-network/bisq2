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

package bisq.bi2p;


import bisq.bi2p.common.standby.PreventStandbyModeService;
import bisq.bi2p.common.utils.ImageUtil;
import bisq.bi2p.common.utils.KeyHandlerUtil;
import bisq.bi2p.service.I2PRouterService;
import bisq.bi2p.ui.Bi2pController;
import bisq.bi2p.ui.Bi2pView;
import bisq.common.observable.Observable;
import bisq.common.platform.OS;
import bisq.common.platform.PlatformUtils;
import bisq.i18n.Res;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Paint;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

@Slf4j
public class Bi2pApp extends Application {
    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;
    private I2PRouterService i2pRouterService;
    private Bi2pController controller;
    private String i2pRouterDir;
    private PreventStandbyModeService preventStandbyModeService;
    // For now, we have it turned on always, but maybe we allow to turn off in UI or by options, thus we leave it
    // as Observable.
    private final Observable<Boolean> preventStandbyMode = new Observable<>(true);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private SystemTray systemTray;
    private TrayIcon trayIcon;

    public Bi2pApp() {
        // Taskbar is only supported on mac
        if (OS.isMacOs()) {
            ImageIcon image = new ImageIcon(Objects.requireNonNull(Bi2pApp.class.getResource("/images/app_icons/bi2p-app_512.png")));
            Taskbar.getTaskbar().setIconImage(image.getImage());
        }
    }

    @Override
    public void init() {
        Parameters parameters = getParameters();
        i2pRouterDir = Optional.ofNullable(parameters.getNamed().get("i2pRouterDir"))
                .orElseGet(() -> PlatformUtils.getUserDataDir().resolve("Bisq2_I2P_router").toString());
        setupRes(parameters);

        preventStandbyModeService = new PreventStandbyModeService(i2pRouterDir, preventStandbyMode);
        preventStandbyModeService.initialize();

        i2pRouterService = new I2PRouterService(parameters, i2pRouterDir);

        //noinspection Convert2MethodRef
        controller = new Bi2pController(WIDTH, HEIGHT, i2pRouterService, () -> shutdown());
        controller.onApplicationReady(parameters);
    }

    @Override
    public void start(Stage primaryStage) {
        // Prevent JavaFX app from exiting when window closes
        Platform.setImplicitExit(false);

        setupStage(primaryStage, controller.getView());
        i2pRouterService.initialize()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Initializing service failed", throwable);
                        shutdown();
                    }
                });
        controller.onActivate();
    }

    private CompletableFuture<Boolean> shutdown() {
        if (shuttingDown.getAndSet(true)) {
            return CompletableFuture.completedFuture(false);
        }
        if (i2pRouterService == null) {
            doShutdown();
            return CompletableFuture.completedFuture(false);
        }
        if (systemTray != null && trayIcon != null) {
            try {
                systemTray.remove(trayIcon);
            } catch (Exception ignore) {
            }
        }
        return i2pRouterService.shutdown()
                .whenComplete((result, throwable) -> doShutdown());
    }

    private void doShutdown() {
        log.error("doShutdown");
        preventStandbyModeService.shutdown();
        if (controller != null) {
            controller.onDeactivate();
        }
        // Platform.exit(); does not work here, probably because of the system tray
        System.exit(PlatformUtils.EXIT_SUCCESS);
    }

    private void setupStage(Stage stage, Bi2pView view) {
        stage.setTitle("Bisq I2P Router");
        Scene scene = new Scene(view, WIDTH, HEIGHT);
        scene.setFill(Paint.valueOf("#1c1c1c"));
        scene.getStylesheets().addAll(requireNonNull(this.getClass().getResource("/css/base.css")).toExternalForm(),
                requireNonNull(this.getClass().getResource("/css/bi2p_app.css")).toExternalForm());
        stage.setScene(scene);
        stage.sizeToScene();

        if (SystemTray.isSupported()) {
            stage.setOnCloseRequest(event -> {
                event.consume();
                stage.hide();
            });

            scene.addEventHandler(KeyEvent.KEY_PRESSED,
                    event -> KeyHandlerUtil.handleCloseKeyEvent(event, () -> {
                        event.consume();
                        stage.hide();
                    }));
            PopupMenu popupMenu = new PopupMenu();

            systemTray = SystemTray.getSystemTray();
            URL iconUrl = getClass().getResource("/images/tray_icon/bi2p-tray_32@2x.png");
            java.awt.Image icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
            trayIcon = new TrayIcon(icon, "Bisq I2P Router", popupMenu);
            trayIcon.setImageAutoSize(true);
            try {
                systemTray.add(trayIcon);
            } catch (AWTException e) {
                log.error("Error at adding trayIcon", e);
            }

            MenuItem showItem = new MenuItem("Show Window");
            showItem.addActionListener(e -> {
                Platform.runLater(() -> {
                    if (stage.isShowing()) {
                        stage.hide();
                        showItem.setLabel("Show Window");
                    } else {
                        stage.show();
                        showItem.setLabel("Hide Window");
                    }
                });
            });
            popupMenu.add(showItem);

            MenuItem exitItem = new MenuItem("Exit");
            exitItem.addActionListener(e -> {
                systemTray.remove(trayIcon);
                shutdown();
            });
            popupMenu.add(exitItem);
        } else {
            stage.setOnCloseRequest(event -> {
                event.consume();
                shutdown();
            });

            scene.addEventHandler(KeyEvent.KEY_PRESSED,
                    event -> KeyHandlerUtil.handleCloseKeyEvent(event, () -> {
                        event.consume();
                        shutdown();
                    }));
        }

        //stage.getIcons().add(I2PRouterApp.getImageByPath("images/tray-icon.png"));
        stage.setX(0);
        stage.setY(0);
        stage.show();
    }

    public static Image getImageByPath(String path) {
        try (InputStream resourceAsStream = ImageUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (resourceAsStream == null) {
                return null;
            }
            return new Image(Objects.requireNonNull(resourceAsStream));
        } catch (Exception e) {
            log.error("Loading image failed: path={}", path, e);
            throw new RuntimeException(e);
        }
    }

    private void setupRes(Parameters parameters) {
        String language = Optional.ofNullable(parameters.getNamed().get("language"))
                .map(languageParam -> URLDecoder.decode(languageParam, StandardCharsets.UTF_8))
                .orElse("en");
        Res.setAndApplyLanguage(language);
    }
}

