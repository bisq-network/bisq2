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

package bisq.network.tor.local_network;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamWaiter {
    private final InputStream inputStream;

    public InputStreamWaiter(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void waitForString(String expected) throws IOException {
        int expectedStringLength = expected.length();
        byte[] buffer = new byte[expectedStringLength];

        var stringBuilder = new StringBuilder();
        do {
            int numberOfReadBytes = inputStream.read(buffer, 0, expectedStringLength);
            String readString = new String(buffer, 0, numberOfReadBytes);
            stringBuilder.append(readString);
        } while (!isExpectedString(stringBuilder, expected));
    }

    private boolean isExpectedString(StringBuilder stringBuilder, String expected) {
        return stringBuilder.length() == expected.length() &&
                stringBuilder.toString().equals(expected);
    }
}
