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

import bisq.network.tor.local_network.InputStreamWaiter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class InputStreamWaiterTests {

    @Test
    @Timeout(value = 30)
    public void waitForPasswordPromptFullString() throws IOException, InterruptedException {
        var pipedOutputStream = new PipedOutputStream();
        var pipedInputStream = new PipedInputStream(pipedOutputStream);

        String testString = "Enter PEM pass phrase:";
        var thread = new Thread(() -> {
            try {
                pipedOutputStream.write(testString.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();
        thread.join();

        InputStreamWaiter inputStreamWaiter = new InputStreamWaiter(pipedInputStream);
        inputStreamWaiter.waitForString(testString);
    }

    @Test
    @Timeout(value = 30)
    public void waitForPasswordPromptPartialReads() throws IOException {
        var pipedOutputStream = new PipedOutputStream();
        var pipedInputStream = new PipedInputStream(pipedOutputStream);

        var thread = new Thread(() -> {
            String testString = "Enter PEM pass ";
            try {
                pipedOutputStream.write(testString.getBytes(StandardCharsets.UTF_8));
                pipedOutputStream.flush();

                while (pipedInputStream.available() > 0) {
                    //noinspection BusyWait
                    Thread.sleep(100);
                }

                String remainingString = "phrase:";
                pipedOutputStream.write(remainingString.getBytes(StandardCharsets.UTF_8));
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
        thread.start();

        InputStreamWaiter inputStreamWaiter = new InputStreamWaiter(pipedInputStream);
        inputStreamWaiter.waitForString("Enter PEM pass phrase:");
    }
}
