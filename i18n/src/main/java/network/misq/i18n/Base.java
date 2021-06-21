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

package network.misq.i18n;

import lombok.extern.slf4j.Slf4j;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Slf4j
abstract class Base {
    private final ResourceBundle resourceBundle;

    protected Base(Locale locale) {
        if ("en".equalsIgnoreCase(locale.getLanguage())) {
            locale = Locale.ROOT;
        }
        resourceBundle = ResourceBundle.getBundle(getClass().getSimpleName().toLowerCase(), locale, new UTF8Control());
    }

    protected String getValue(String key, Object... arguments) {
        return MessageFormat.format(Res.Default.get(key), arguments);
    }

    protected String getValue(String key) {
        try {
            return resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            log.warn("Missing resource for key: {}", key);
            e.printStackTrace();
            return key;
        }
    }
}
