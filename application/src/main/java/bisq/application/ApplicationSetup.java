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

package bisq.application;

import bisq.common.application.DevMode;
import bisq.common.locale.LocaleRepository;
import bisq.common.logging.LogSetup;
import bisq.i18n.Res;

import java.nio.file.Paths;
import java.util.Locale;

public class ApplicationSetup {
    static void initialize(ApplicationConfig applicationConfig) {
        LogSetup.setup(Paths.get(applicationConfig.baseDir(), "bisq").toString());

        Locale locale = applicationConfig.getLocale();
        LocaleRepository.initialize(locale);
        Res.initialize(locale);

        DevMode.setDevMode(applicationConfig.devMode());
    }
}