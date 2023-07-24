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

package bisq.update;

import bisq.common.observable.Observable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import static bisq.update.Utils.*;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public class Descriptor {
    static List<Descriptor> createDescriptorList(String version, String destinationDirectory, String sourceFileName) throws IOException {
        String baseUrl = GITHUB_DOWNLOAD_URL + version + "/";
        String key_4A133008_fileName = KEY_4A133008 + EXTENSION;
        String key_E222AA02_fileName = KEY_E222AA02 + EXTENSION;
        return List.of(
                create(SIGNING_KEY_FILE, baseUrl, destinationDirectory),
                create(key_4A133008_fileName, FROM_BISQ_WEBPAGE_PREFIX + key_4A133008_fileName, PUB_KEYS_URL, destinationDirectory),
                create(key_E222AA02_fileName, FROM_BISQ_WEBPAGE_PREFIX + key_E222AA02_fileName, PUB_KEYS_URL, destinationDirectory),
                create(key_4A133008_fileName, baseUrl, destinationDirectory),
                create(key_E222AA02_fileName, baseUrl, destinationDirectory),
                create(sourceFileName + EXTENSION, FILE_NAME + EXTENSION, baseUrl, destinationDirectory),
                create(sourceFileName, FILE_NAME, baseUrl, destinationDirectory)
        );
    }

    public static Descriptor create(String fileName, String baseUrl, String destinationDirectory) throws MalformedURLException {
        return create(fileName, fileName, baseUrl, destinationDirectory);
    }

    public static Descriptor create(String sourceFileName, String destinationFileName, String baseUrl, String destinationDirectory) throws MalformedURLException {
        File destination = Path.of(destinationDirectory, destinationFileName).toFile();
        String urlPath = baseUrl + sourceFileName;
        return new Descriptor(new URL(urlPath), destination, sourceFileName);
    }

    private final URL url;
    private final File destination;
    private final String sourceFileName;
    private final Observable<Double> progress = new Observable<>(-1d);

    private Descriptor(URL url, File destination, String sourceFileName) {
        this.url = url;
        this.destination = destination;
        this.sourceFileName = sourceFileName;
    }
}