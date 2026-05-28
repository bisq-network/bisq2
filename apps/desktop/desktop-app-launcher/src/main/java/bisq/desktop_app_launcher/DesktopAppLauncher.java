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

package bisq.desktop_app_launcher;

import bisq.common.application.BuildVersion;
import bisq.common.util.ExceptionUtil;
import bisq.desktop_app.DesktopApp;
import lombok.extern.slf4j.Slf4j;

/**
 * We ship the binary with the current version of the DesktopApp and with the JRE.
 * The launcher calls the main method on the bundled DesktopApp.
 * <p>
 * The `java.home` system property is pointing to the provided JRE from the binary.
 * <p>
 * All JVM options and program arguments are forwarded to the Desktop application. As we cannot pass JVM arguments
 * to an executable, we add all program arguments starting with `-Dapplication` with `System.setProperty` as JVM options.
 */
@Slf4j
public class DesktopAppLauncher {
    public static void main(String[] args) {
        Thread.currentThread().setName("DesktopAppLauncher.main");
        try {
            System.setProperty("javafx.sg.warn", "false");
            new DesktopAppLauncher(args);
        } catch (Exception e) {
            log.error("Error at launch", e);
            System.err.println("Error at launch: " + ExceptionUtil.getStackTraceAsString(e));
        }
    }

    private DesktopAppLauncher(String[] args) {
        new Options(args);
        log.info("Run bundled Bisq application with version {}", BuildVersion.VERSION);
        DesktopApp.main(args);
    }
}
