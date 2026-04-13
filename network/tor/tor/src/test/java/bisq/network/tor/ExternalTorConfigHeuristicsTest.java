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

package bisq.network.tor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalTorConfigHeuristicsTest {

    @Test
    void applyUsesDetectedSystemTorPaths(@TempDir Path tempDir) throws Exception {
        Path controlSocketPath = tempDir.resolve("control");
        Path cookieAuthFilePath = tempDir.resolve("control.authcookie");
        Files.createFile(controlSocketPath);
        Files.createFile(cookieAuthFilePath);

        String template = "#UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n" +
                "## ControlSocket /path/to/tor/control.socket\n" +
                "ControlPort 127.0.0.1:9051\n";

        String result = ExternalTorConfigHeuristics.apply(
                template,
                controlSocketPath,
                List.of(cookieAuthFilePath),
                false);

        String expected = "UseExternalTor 1\n" +
                "CookieAuthentication 1\n" +
                "CookieAuthFile " + cookieAuthFilePath + "\n" +
                "ControlSocket " + controlSocketPath + "\n" +
                "ControlPort 127.0.0.1:9051\n";

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void applyLeavesTemplateUnchangedWithoutSystemTorSocket(@TempDir Path tempDir) {
        String template = "#UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n";

        String result = ExternalTorConfigHeuristics.apply(
                template,
                tempDir.resolve("missing-control"),
                List.of(tempDir.resolve("missing-cookie")),
                false);

        assertThat(result).isEqualTo(template);
    }

    @Test
    void applyLeavesCookieAuthenticationDisabledWhenCookieFileIsMissing(@TempDir Path tempDir) throws Exception {
        Path controlSocketPath = tempDir.resolve("control");
        Files.createFile(controlSocketPath);

        String template = "#UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n" +
                "## ControlSocket /path/to/tor/control.socket\n" +
                "ControlPort 127.0.0.1:9051\n";

        String result = ExternalTorConfigHeuristics.apply(
                template,
                controlSocketPath,
                List.of(tempDir.resolve("missing-control.authcookie")),
                false);

        String expected = "UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n" +
                "ControlSocket " + controlSocketPath + "\n" +
                "ControlPort 127.0.0.1:9051\n";

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void applyUsesOnionGraterOnTailsWhenSocketNotReadable(@TempDir Path tempDir) throws Exception {
        Path cookieAuthFilePath = tempDir.resolve("control.authcookie");
        Files.createFile(cookieAuthFilePath);

        String template = "#UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n" +
                "## ControlSocket /path/to/tor/control.socket\n" +
                "ControlPort 127.0.0.1:9051\n";

        String result = ExternalTorConfigHeuristics.apply(
                template,
                tempDir.resolve("missing-control"),
                List.of(cookieAuthFilePath),
                true);

        String expected = "UseExternalTor 1\n" +
                "CookieAuthentication 1\n" +
                "CookieAuthFile " + cookieAuthFilePath + "\n" +
                "## ControlSocket /path/to/tor/control.socket\n" +
                "ControlPort 127.0.0.1:951\n";

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void applyUsesOnionGraterOnTailsWithoutCookieAuth(@TempDir Path tempDir) {
        String template = "#UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n" +
                "## ControlSocket /path/to/tor/control.socket\n" +
                "ControlPort 127.0.0.1:9051\n";

        String result = ExternalTorConfigHeuristics.apply(
                template,
                tempDir.resolve("missing-control"),
                List.of(tempDir.resolve("missing-cookie")),
                true);

        String expected = "UseExternalTor 1\n" +
                "CookieAuthentication 0\n" +
                "CookieAuthFile /path/to/control_auth_cookie\n" +
                "## ControlSocket /path/to/tor/control.socket\n" +
                "ControlPort 127.0.0.1:951\n";

        assertThat(result).isEqualTo(expected);
    }
}
