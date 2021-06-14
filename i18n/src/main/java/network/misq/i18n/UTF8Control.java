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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Adds UTF8 support for property files
 */
class UTF8Control extends ResourceBundle.Control {
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload)
            throws IOException {
        // Below is a copy following the default implementation.
        String bundleName = toBundleName(baseName, locale);
        try {
            String resourceName = checkNotNull(toResourceName(bundleName, "properties"));
            if (reload) {
                URL url = checkNotNull(loader.getResource(resourceName));
                URLConnection connection = checkNotNull(url.openConnection());
                connection.setUseCaches(false);
                try (InputStream stream = connection.getInputStream()) {
                    return getBundle(stream);
                }
            } else {
                try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                    return getBundle(stream);
                }
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private ResourceBundle getBundle(InputStream stream) throws IOException {
        // Only this line is changed to make it read properties files as UTF-8.
        return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
