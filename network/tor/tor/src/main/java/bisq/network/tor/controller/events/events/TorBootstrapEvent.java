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

package bisq.network.tor.controller.events.events;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public class TorBootstrapEvent {
    private static final String DONE_TAG = "done";
    public static final TorBootstrapEvent CONNECT_TO_EXTERNAL_TOR = new TorBootstrapEvent(0, "", "Connect to external Tor");
    public static final TorBootstrapEvent CONNECTION_TO_EXTERNAL_TOR_COMPLETED = new TorBootstrapEvent(20, "", "Connection to external Tor completed");

    private final int progress;
    private final String tag;
    private final String summary;

    private final Instant timestamp = Instant.now();

    public TorBootstrapEvent(int progress, String tag, String summary) {
        if (progress < 0 || summary.isEmpty()) {
            throw new IllegalArgumentException("Invalid bootstrap event: " + progress + " " + tag + " " + summary);
        }

        this.progress = progress;
        this.tag = tag;
        this.summary = summary;
    }

    public boolean isDoneEvent() {
        return tag.equals(DONE_TAG) || this.equals(CONNECTION_TO_EXTERNAL_TOR_COMPLETED);
    }

    public static boolean isBootstrapMessage(String type, String message) {
        return type.equals(EventType.STATUS_CLIENT.name()) && message.contains("BOOTSTRAP");
    }

    public static TorBootstrapEvent fromEventMessage(String message) {
        String[] keyValuePairs = message.split(" ");

        int progress = -1;
        String tag = "";
        String summary = "";
        for (String item : keyValuePairs) {
            if (!item.contains("=")) {
                continue;
            }

            String[] parts = item.split("=");
            if (parts.length != 2) {
                throw new IllegalStateException("Tor event key value pair has more than two '=' signs.");
            }

            String key = parts[0];
            String value = parts[1];

            switch (key) {
                case "PROGRESS":
                    progress = Integer.parseInt(value);
                    break;
                case "TAG":
                    tag = value;
                    break;
                case "SUMMARY":
                    summary = value;
                    break;
            }
        }

        return new TorBootstrapEvent(progress, tag, summary);
    }
}
