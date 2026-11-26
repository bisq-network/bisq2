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
import bisq.common.platform.OS;
import bisq.common.util.StringUtils;
import bisq.i18n.Res;
import bisq.network.p2p.node.transport.TorTransportService;
import com.google.common.annotations.VisibleForTesting;

import java.nio.file.Path;
import java.util.ArrayList;
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
    private static final Map<String, Object> ALLOW_PLUGGABLE_TRANSPORTS =
            Map.of(TorTransportService.ALLOW_PLUGGABLE_TRANSPORTS_CONFIG_PATH, true);
    // Both, so that no bridges remain, whichever of them the user configured
    private static final Map<String, Object> NO_BRIDGES = Map.of(TorTransportService.BUILT_IN_BRIDGES_CONFIG_PATH, "",
            TorTransportService.BRIDGES_CONFIG_PATH, List.of());

    // The provider which StartupFailurePopups lists
    static Optional<StartupFailurePopupContent> findContent(Throwable startupFailure,
                                                            String errorMessage,
                                                            ApplicationService.Config config) {
        return TorStartupFailure.find(startupFailure)
                .map(failure -> create(failure, errorMessage, OS.isWindows(), config::isSetOnCommandLine,
                        config.getCustomConfigFilePath()));
    }

    /**
     * @param errorMessage       the startup error message, which states the reason of a sandbox which cannot run Tor
     * @param isSetOnCommandLine whether a program argument or a JVM option sets a configuration path
     */
    @VisibleForTesting
    static StartupFailurePopupContent create(TorStartupFailure failure,
                                             String errorMessage,
                                             boolean isWindows,
                                             Predicate<String> isSetOnCommandLine,
                                             Path configFilePath) {
        return switch (failure) {
            case TorStartupFailure.SandboxUnavailable ignored ->
                    forSandboxUnavailable(errorMessage, isSetOnCommandLine, configFilePath);
            case TorStartupFailure.PluggableTransportsNotAllowed notAllowed ->
                    forPluggableTransportsNotAllowed(notAllowed.transportNames(), isWindows, isSetOnCommandLine,
                            configFilePath);
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

    // The message says what each choice costs, so the choices need no confirmation of their own. Turning the sandbox
    // off is offered only on Windows, where Tor cannot start the transports in its sandbox.
    private static StartupFailurePopupContent forPluggableTransportsNotAllowed(List<String> transportNames,
                                                                               boolean isWindows,
                                                                               Predicate<String> isSetOnCommandLine,
                                                                               Path configFilePath) {
        List<String> paragraphs = new ArrayList<>();
        paragraphs.add(Res.get("popup.startup.error.torPluggableTransportsNotAllowed.intro",
                String.join(", ", transportNames)));
        paragraphs.add(Res.get(isWindows ?
                "popup.startup.error.torPluggableTransportsNotAllowed.windowsInfo" :
                "popup.startup.error.torPluggableTransportsNotAllowed.allowInfo"));
        paragraphs.add(Res.get("popup.startup.error.torPluggableTransportsNotAllowed.noBridgesInfo"));

        StartupFailurePopupContent.Choice allow = new StartupFailurePopupContent.Choice(
                Res.get("popup.startup.error.torPluggableTransportsNotAllowed.allow"), ALLOW_PLUGGABLE_TRANSPORTS);
        StartupFailurePopupContent.Choice noBridges = new StartupFailurePopupContent.Choice(
                Res.get("popup.startup.error.torPluggableTransportsNotAllowed.noBridges"), NO_BRIDGES);
        List<StartupFailurePopupContent.Choice> candidates = isWindows ?
                List.of(noBridges, turnOffSandboxChoice()) :
                List.of(allow, noBridges);
        List<StartupFailurePopupContent.Choice> choices = new ArrayList<>();
        List<String> pathsSetOnCommandLine = new ArrayList<>();
        for (StartupFailurePopupContent.Choice candidate : candidates) {
            List<String> overridden = candidate.configEntries().keySet().stream()
                    .filter(isSetOnCommandLine)
                    .sorted()
                    .toList();
            if (overridden.isEmpty()) {
                choices.add(candidate);
            } else {
                pathsSetOnCommandLine.addAll(overridden);
            }
        }
        if (!pathsSetOnCommandLine.isEmpty()) {
            paragraphs.add(commandLineInfo(pathsSetOnCommandLine));
        }
        if (!choices.isEmpty()) {
            paragraphs.add(Res.get("popup.startup.error.choice.info", configFilePath.toAbsolutePath().toString()));
        }
        return new StartupFailurePopupContent(Res.get("popup.startup.error.torPluggableTransportsNotAllowed.headline"),
                String.join("\n\n", paragraphs),
                choices);
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
