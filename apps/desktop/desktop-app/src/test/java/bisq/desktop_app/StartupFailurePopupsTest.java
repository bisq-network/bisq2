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

package bisq.desktop_app;

import bisq.application.ApplicationService;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StartupFailurePopupsTest {
    @TempDir
    Path appDataDirPath;

    // The start shows the error popup with the bug report for every other failure
    @Test
    void aFailureWhichNoProviderRecognizesGetsNoPopupContent() {
        Throwable startupFailure = new CompletionException(new IllegalStateException("Could not read the wallet"));

        assertThat(StartupFailurePopups.findContent(startupFailure, "Could not read the wallet", createConfig()))
                .isEmpty();
    }

    // The popup has two buttons for choices, so a third choice would not be shown
    @Test
    void aPopupOffersAtMostTwoChoices() {
        StartupFailurePopupContent.Choice choice =
                new StartupFailurePopupContent.Choice("Apply and shut down", Map.of("application.example", true));

        assertThat(new StartupFailurePopupContent("Headline", "Message", List.of(choice, choice)).choices())
                .hasSize(2);
        assertThatThrownBy(() -> new StartupFailurePopupContent("Headline", "Message", List.of(choice, choice, choice)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private ApplicationService.Config createConfig() {
        return new ApplicationService.Config(ConfigFactory.empty(), ConfigFactory.empty(), appDataDirPath, "Bisq2",
                false, 0, false, "", false, false, 0, false, false);
    }
}
