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
        return tag.equals(DONE_TAG);
    }
}
