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

package bisq.e2e;

import bisq.api_app.ApiApp;
import bisq.bonded_roles.release.AppType;
import bisq.bonded_roles.security_manager.alert.AlertType;
import bisq.bonded_roles.security_manager.alert.AuthorizedAlertData;
import bisq.common.encoding.Hex;
import bisq.network.NetworkService;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyGeneration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Optional;
import java.util.UUID;

/**
 * An api-app node of this checkout which publishes and removes authorized data with a dev key, as a security manager
 * does. No API of the nodes publishes such data, so e2e tests start this program instead of the api-app. A test sends
 * one command per line to the standard input of the program:
 * <ul>
 *     <li>{@code require <version>}: publishes an alert for the desktop app which requires this version for trading,
 *     with the fields which the security manager tab of the desktop app sets</li>
 *     <li>{@code remove}: removes this alert</li>
 * </ul>
 * The first dev key of AuthorizedPubKeys signs the alert, and the alert states that a static key authorizes it. Nodes
 * in dev mode accept the dev keys as static keys. The same key owns the entry in the network storage, as for all
 * authorized data (docs/specifications/network/storage-ownership.md). The program logs {@link #PUBLISHED} and
 * {@link #REMOVED} when the broadcast of a command is done.
 */
public class AuthorizedDataPublisherApp extends ApiApp {
    private static final String PUBLISHED = "E2E alert published";
    private static final String REMOVED = "E2E alert removed";

    private static final Logger log = LoggerFactory.getLogger(AuthorizedDataPublisherApp.class);
    // privatekey1 and publicKey1 of the Javadoc of AuthorizedPubKeys
    private static final String DEV_PRIVATE_KEY = "30818d020100301006072a8648ce3d020106052b8104000a047630740201010420" +
            "5b4479d165652fe5410419b1d03c937956be0e1c4f46e9fbe86c66776529d81ca00706052b8104000aa144034200043dd1f2" +
            "f56593e62670282c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e14697055" +
            "67124a02ae2e8afa8e8091";
    private static final String DEV_PUBLIC_KEY = "3056301006072a8648ce3d020106052b8104000a034200043dd1f2f56593e626702" +
            "82c245cb71d50b43985b308dd1c977632c3cde155427e4fad0899d7e7af110584182f7e55547d6e1469705567124a02ae2e8" +
            "afa8e8091";

    // The constructor of Executable blocks the main thread until the shutdown, so the fields of this class must not
    // have initializers: they would never run.
    private volatile AuthorizedAlertData publishedAlert;

    public static void main(String[] args) {
        Thread.currentThread().setName("AuthorizedDataPublisherApp.main");
        new AuthorizedDataPublisherApp(args);
    }

    public AuthorizedDataPublisherApp(String[] args) {
        super(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        super.onApplicationServiceInitialized(result, throwable);
        if (throwable == null && Boolean.TRUE.equals(result)) {
            Thread commandReader = new Thread(this::readCommands, "AuthorizedDataPublisherApp.commands");
            commandReader.setDaemon(true);
            commandReader.start();
        } else {
            log.error("The authorized data publisher did not start", throwable);
        }
    }

    private void readCommands() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] command = line.trim().split(" ");
                if (command[0].equals("require") && command.length == 2) {
                    publishAlertWhichRequiresVersion(command[1]);
                } else if (command[0].equals("remove") && command.length == 1) {
                    removeAlert();
                } else {
                    log.error("Unknown command of the authorized data publisher: {}", line);
                }
            }
        } catch (IOException e) {
            log.error("The authorized data publisher cannot read its commands", e);
        }
    }

    private void publishAlertWhichRequiresVersion(String minVersion) {
        KeyPair devKeyPair = devKeyPair();
        AuthorizedAlertData alert = new AuthorizedAlertData(UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                AlertType.EMERGENCY,
                Optional.of("E2E: update required"),
                Optional.of("Version " + minVersion + " is required for trading"),
                false,
                true,
                Optional.of(minVersion),
                Optional.empty(),
                Hex.encode(DigestUtil.hash(devKeyPair.getPublic().getEncoded())),
                true,
                Optional.empty(),
                AppType.DESKTOP);
        networkService().publishAuthorizedData(alert, devKeyPair).join();
        publishedAlert = alert;
        log.info("{}: {}", PUBLISHED, alert);
    }

    private void removeAlert() {
        AuthorizedAlertData alert = publishedAlert;
        if (alert == null) {
            log.error("The authorized data publisher has no alert to remove");
            return;
        }
        networkService().removeAuthorizedData(alert, devKeyPair()).join();
        publishedAlert = null;
        log.info("{}: {}", REMOVED, alert.getId());
    }

    private NetworkService networkService() {
        return applicationService.getNetworkService();
    }

    private static KeyPair devKeyPair() {
        try {
            return new KeyPair(KeyGeneration.generatePublic(Hex.decode(DEV_PUBLIC_KEY)),
                    KeyGeneration.generatePrivate(Hex.decode(DEV_PRIVATE_KEY)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
