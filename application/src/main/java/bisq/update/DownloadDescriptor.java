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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;

@ToString
@Getter
@EqualsAndHashCode
public class DownloadDescriptor {
    public static DownloadDescriptor create(String fileName, String baseUrl, String destinationDirectory) throws MalformedURLException {
        return create(fileName, fileName, baseUrl, destinationDirectory);
    }

    public static DownloadDescriptor create(String sourceFileName, String destinationFileName, String baseUrl, String destinationDirectory) throws MalformedURLException {
        File destination = Path.of(destinationDirectory, destinationFileName).toFile();
        String urlPath = baseUrl + sourceFileName;
        return new DownloadDescriptor(new URL(urlPath), destination, sourceFileName);
    }

    private final URL url;
    private final File destination;
    private final String sourceFileName;
    private final Observable<Double> progress = new Observable<>(-1d);

    private DownloadDescriptor(URL url, File destination, String sourceFileName) {
        this.url = url;
        this.destination = destination;
        this.sourceFileName = sourceFileName;
    }
}