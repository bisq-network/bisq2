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
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.p2p.node.transport.TorTransportService;
import com.google.common.annotations.VisibleForTesting;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * What the popup of a failed start of Tor says and which choices it offers. A choice which a program argument or a
 * JVM option overrides is left out, because the command line wins over bisq.conf, and the message names the option.
 */
final class TorStartupFailurePopups {
    // Leaves room for the explanation of the choice within the 2200 characters which the popup shows
    private static final int MAX_ERROR_LENGTH = 1200;
    private static final Map<String, Object> TURN_OFF_SANDBOX =
            Map.of(TorTransportService.SANDBOX_ENABLED_CONFIG_PATH, false);

    // The provider which StartupFailurePopups lists
    static Optional<StartupFailurePopupContent> findContent(Throwable startupFailure,
                                                            String errorMessage,
                                                            ApplicationService.Config config) {
        return TorStartupFailure.find(startupFailure)
                .map(failure -> create(failure, errorMessage, config::isSetOnCommandLine,
                        config.getCustomConfigFilePath()));
    }

    /**
     * @param errorMessage       the startup error message, which states the reason of a sandbox which cannot run Tor
     * @param isSetOnCommandLine whether a program argument or a JVM option sets a configuration path
     */
    @VisibleForTesting
    static StartupFailurePopupContent create(TorStartupFailure failure,
                                             String errorMessage,
                                             Predicate<String> isSetOnCommandLine,
                                             Path configFilePath) {
        return switch (failure) {
            case TorStartupFailure.SandboxUnavailable ignored ->
                    forSandboxUnavailable(errorMessage, isSetOnCommandLine, configFilePath);
        };
    }

    // The message says what Tor without the sandbox risks, so the choice needs no confirmation of its own
    private static StartupFailurePopupContent forSandboxUnavailable(String errorMessage,
                                                                    Predicate<String> isSetOnCommandLine,
                                                                    Path configFilePath) {
        String headline = Res.get("popup.startup.error.torSandboxUnavailable.headline");
        String error = Res.get("popup.startup.error", shortenError(errorMessage));
        if (isSetOnCommandLine.test(TorTransportService.SANDBOX_ENABLED_CONFIG_PATH)) {
            return new StartupFailurePopupContent(headline,
                    error + "\n\n" + commandLineInfo(List.of(TorTransportService.SANDBOX_ENABLED_CONFIG_PATH)),
                    List.of());
        }
        String turnOffInfo = Res.get("popup.startup.error.torSandboxUnavailable.turnOff.info",
                TorTransportService.SANDBOX_ENABLED_CONFIG_PATH, configFilePath.toAbsolutePath().toString());
        return new StartupFailurePopupContent(headline,
                error + "\n\n" + turnOffInfo,
                List.of(turnOffSandboxChoice()));
    }

    private static StartupFailurePopupContent.Choice turnOffSandboxChoice() {
        return new StartupFailurePopupContent.Choice(Res.get("popup.startup.error.torSandboxUnavailable.turnOff"),
                TURN_OFF_SANDBOX);
    }

    // The popup cuts a longer message at its end, which would cut off the choice, its risk and how to undo it. The
    // error of a refusal of sandy can quote a long report and the standard error of sandy and Tor; bisq.log keeps all.
    @VisibleForTesting
    static String shortenError(String errorMessage) {
        if (errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }
        return StringUtils.truncate(errorMessage, MAX_ERROR_LENGTH) + "\n" +
                Res.get("popup.startup.error.choice.errorShortened");
    }

    private static String commandLineInfo(List<String> paths) {
        return Res.get("popup.startup.error.choice.commandLine", String.join(", ", paths));
    }
}
