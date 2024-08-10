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

package bisq.network.p2p.node;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * We do not use a protobuf enum for CloseReason as it might easily change its entries and with protobuf it becomes
 * more restrictive to handle updates.
 */
@Slf4j
@Getter
public enum CloseReason {
    SHUTDOWN(true),
    CLOSE_MSG_RECEIVED(true),
    CLOSE_MSG_SENT(true),
    ADDRESS_VALIDATION_COMPLETED(true),
    ADDRESS_VALIDATION_FAILED(false),
    DUPLICATE_CONNECTION(true),
    TOO_MANY_CONNECTIONS_TO_SEEDS(true),
    AGED_CONNECTION(true),
    TOO_MANY_INBOUND_CONNECTIONS(true),
    TOO_MANY_CONNECTIONS(true),
    BANNED(false),
    ORPHANED_CONNECTION(false),
    EXCEPTION(false);

    private final boolean isGraceful;
    private Optional<String> details = Optional.empty();
    private Optional<Throwable> exception = Optional.empty();

    CloseReason(boolean isGraceful) {
        this.isGraceful = isGraceful;
    }

    public CloseReason details(String details) {
        this.details = Optional.of(details);
        return this;
    }

    public CloseReason exception(Throwable exception) {
        this.exception = Optional.of(exception);
        return this;
    }

    @Override
    public String toString() {
        String graceful = isGraceful ? " [graceful" : " [forced";
        return "Reason: " + name() + graceful +
                details.map(e -> ", details: " + e).orElse("") +
                exception.map(e -> ", exception: " + e).orElse("") + "]";
    }
}