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

package bisq.i2p_router;


import bisq.common.file.FileUtils;
import bisq.common.logging.LogSetup;
import bisq.common.platform.OS;
import bisq.common.platform.PlatformUtils;
import bisq.i2p_router.common.utils.ImageUtil;
import bisq.i2p_router.common.utils.KeyHandlerUtil;
import bisq.i18n.Res;
import bisq.i2p_router.gui.Controller;
import bisq.i2p_router.gui.View;
import bisq.i2p_router.service.I2pRouterService;
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
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

@Slf4j
public class I2PRouterApp extends Application {
    private static final double WIDTH = 800;
    private static final double HEIGHT = 600;
    private final I2pRouterService service;
    private Controller controller;

    public I2PRouterApp() {
        service = new I2pRouterService();

        // Taskbar is only supported on mac
        if (OS.isMacOs()) {
            ImageIcon image = new ImageIcon(Objects.requireNonNull(I2PRouterApp.class.getResource("/images/app-icon.png")));
            Taskbar.getTaskbar().setIconImage(image.getImage());
        }
    }

    @Override
    public void init() {
        Parameters parameters = getParameters();
        setupLogging(parameters);
        setupRes(parameters);
        controller = new Controller(WIDTH, HEIGHT, service, this::shutdown);
        service.onApplicationReady(parameters);
        controller.onApplicationReady(parameters);
    }

    @Override
    public void start(Stage primaryStage) {
        // Prevent JavaFX app from exiting when window closes
        Platform.setImplicitExit(false);

        setupStage(primaryStage, controller.getView());
        service.initialize()
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        log.error("Initializing service failed", throwable);
                        shutdown();
                    }
                });
        controller.onActivate();
    }

    private CompletableFuture<Boolean> shutdown() {
        return service.shutdown()
                .whenComplete((result, throwable) -> {
                    if (controller != null) {
                        controller.onDeactivate();
                    }
                    try {
                        Platform.exit();
                    } catch (Exception e) {
                        System.exit(0);
                    }
                });
    }

    private void setupStage(Stage stage, View view) {
        stage.setTitle("Bisq I2P Router");
        Scene scene = new Scene(view, WIDTH, HEIGHT);
        scene.setFill(Paint.valueOf("#1c1c1c"));
        scene.getStylesheets().addAll(requireNonNull(this.getClass().getResource("/css/base.css")).toExternalForm(),
                requireNonNull(this.getClass().getResource("/css/i2p_router_app.css")).toExternalForm());
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

            SystemTray systemTray = SystemTray.getSystemTray();
            URL iconUrl = getClass().getResource("/images/tray-icon.png");
            java.awt.Image icon = Toolkit.getDefaultToolkit().getImage(iconUrl);
            TrayIcon trayIcon = new TrayIcon(icon, "Bisq I2P Router", popupMenu);
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

    private void setupLogging(Parameters parameters) {
        String logFile = PlatformUtils.getUserDataDir().resolve("Bisq-i2p-router-app").toAbsolutePath() + FileUtils.FILE_SEP + "i2p-router-app";
        String logFileParam = parameters.getNamed().get("logFile");
        if (logFileParam != null) {
            logFile = URLDecoder.decode(logFileParam, StandardCharsets.UTF_8);
        }
        LogSetup.setup(logFile);
        log.info("I2P router app logging to {}", logFile);
    }

    private void setupRes(Parameters parameters) {
        String language = "en";
        String languageParam = parameters.getNamed().get("language");
        if (languageParam != null) {
            language = URLDecoder.decode(languageParam, StandardCharsets.UTF_8);
        }
        Res.setLanguage(language);
    }
}

