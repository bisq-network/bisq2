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

package bisq.tor.process.control_port;

import bisq.common.facades.FacadeProvider;

import java.io.IOException;
import java.nio.file.Path;

public class ControlPortFileParser {
    private static final String PORT_MARKER = "PORT=";

    public static int parse(Path controlPortFilePath) {
        try {
            String fileContent = FacadeProvider.getJdkFacade().readString(controlPortFilePath);
            if (isControlPortFileReady(fileContent)) {
                for (String line : fileContent.split("\n")) {
                    // Lines end on Windows with "\r\n". Previous String.split("\n") removed "\n" already.
                    line = line.replace("\r", "");

                    if (isPortLine(line)) {
                        return parsePort(line);
                    }
                }
            }

            throw new ControlPortFileParseFailureException("Parsed file is invalid or incomplete: " + fileContent);

        } catch (IOException e) {
            throw new ControlPortFileParseFailureException(e);
        }
    }

    private static boolean isControlPortFileReady(String fileContent) {
        return fileContent.contains("\n") && fileContent.contains(PORT_MARKER);
    }

    private static boolean isPortLine(String line) {
        return line.startsWith("PORT=");
    }

    private static int parsePort(String line) {
        // PORT=127.0.0.1:37611
        String lineWithoutPrefix = line.replace("PORT=", "");
        String[] ipAndPort = lineWithoutPrefix.split(":");

        if (ipAndPort.length == 2) {
            String port = ipAndPort[1];
            return Integer.parseInt(port);
        }

        throw new ControlPortFileParseFailureException("Couldn't parse control port line: " + line);
    }
}
