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

import java.util.List;
import java.util.Optional;

/**
 * The failed starts which a change of bisq.conf fixes. Their popup offers the change instead of a bug report.
 */
final class StartupFailurePopups {
    @FunctionalInterface
    interface ContentProvider {
        /**
         * @param errorMessage the startup error message of the failure
         * @return the content of the popup, if the provider recognizes the failure
         */
        Optional<StartupFailurePopupContent> find(Throwable startupFailure,
                                                  String errorMessage,
                                                  ApplicationService.Config config);
    }

    private static final List<ContentProvider> CONTENT_PROVIDERS = List.of();

    // The content of the first provider which recognizes the failure
    static Optional<StartupFailurePopupContent> findContent(Throwable startupFailure,
                                                            String errorMessage,
                                                            ApplicationService.Config config) {
        return CONTENT_PROVIDERS.stream()
                .flatMap(provider -> provider.find(startupFailure, errorMessage, config).stream())
                .findFirst();
    }
}
