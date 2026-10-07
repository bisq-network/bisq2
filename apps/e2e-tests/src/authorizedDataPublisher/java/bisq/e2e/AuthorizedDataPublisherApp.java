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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * An api-app node of this checkout which publishes and removes authorized data with a dev key, as a security manager or
 * a root oracle node does. No API of the nodes publishes such data, so e2e tests start this program instead of the
 * api-app. A test sends one command per line to the standard input of the program. {@link AuthorizedDataPublisher}
 * lists the commands. The program logs {@code E2E done: <command>} when the broadcast of a command is done, and
 * {@code E2E failed: <command>} with the cause otherwise.
 */
public class AuthorizedDataPublisherApp extends ApiApp {
    private static final Logger log = LoggerFactory.getLogger(AuthorizedDataPublisherApp.class);

    public static void main(String[] args) {
        Thread.currentThread().setName("AuthorizedDataPublisherApp.main");
        new AuthorizedDataPublisherApp(args);
    }

    // The constructor of Executable blocks the main thread until the shutdown, so this class has no fields: their
    // initializers would never run. The state of the commands lives in AuthorizedDataPublisher.
    public AuthorizedDataPublisherApp(String[] args) {
        super(args);
    }

    @Override
    protected void onApplicationServiceInitialized(Boolean result, Throwable throwable) {
        super.onApplicationServiceInitialized(result, throwable);
        if (throwable == null && Boolean.TRUE.equals(result)) {
            AuthorizedDataPublisher publisher = new AuthorizedDataPublisher(applicationService);
            Thread commandReader = new Thread(() -> readCommands(publisher), "AuthorizedDataPublisherApp.commands");
            commandReader.setDaemon(true);
            commandReader.start();
        } else {
            log.error("The authorized data publisher did not start", throwable);
        }
    }

    private static void readCommands(AuthorizedDataPublisher publisher) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String command = line.trim();
                try {
                    publisher.execute(command);
                    log.info("E2E done: {}", command);
                } catch (RuntimeException e) {
                    log.error("E2E failed: {}", command, e);
                }
            }
        } catch (IOException e) {
            log.error("The authorized data publisher cannot read its commands", e);
        }
    }
}
