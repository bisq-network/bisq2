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

package bisq.evolution.updater;

import bisq.common.observable.Observable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static bisq.evolution.updater.UpdaterUtils.*;

@Slf4j
@ToString
@Getter
@EqualsAndHashCode
public class DownloadItem {

    static List<DownloadItem> createDescriptorList(String version,
                                                   String destinationDirectory,
                                                   String fileName,
                                                   List<String> keys) {
        String baseUrl = GITHUB_DOWNLOAD_URL + version + "/";
        List<DownloadItem> downloadItems = new ArrayList<>();
        downloadItems.add(create(SIGNING_KEY_FILE, baseUrl, destinationDirectory));
        for (String key : keys) {
            String keyFileName = key + ASC_EXTENSION;
            downloadItems.add(create(keyFileName, baseUrl, destinationDirectory));
            downloadItems.add(create(keyFileName, FROM_BISQ_WEBPAGE_PREFIX + keyFileName, PUB_KEYS_URL, destinationDirectory));
        }
        downloadItems.add(create(fileName + ASC_EXTENSION, baseUrl, destinationDirectory));
        downloadItems.add(create(fileName, baseUrl, destinationDirectory));
        return downloadItems;
    }

    public static DownloadItem create(String fileName, String baseUrl, String destinationDirectory) {
        return create(fileName, fileName, baseUrl, destinationDirectory);
    }

    private static DownloadItem create(String sourceFileName, String destinationFileName, String baseUrl, String destinationDirectory) {
        File destination = Path.of(destinationDirectory, destinationFileName).toFile();
        String urlPath = baseUrl + sourceFileName;
        return new DownloadItem(urlPath, destination, sourceFileName);
    }

    private final String urlPath;
    private final File destinationFile;
    private final String sourceFileName;
    @ToString.Exclude
    private final Observable<Double> progress = new Observable<>(-1d);

    private DownloadItem(String urlPath, File destinationFile, String sourceFileName) {
        this.urlPath = urlPath;
        this.destinationFile = destinationFile;
        this.sourceFileName = sourceFileName;
    }
}