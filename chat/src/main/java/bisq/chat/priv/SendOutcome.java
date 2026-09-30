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

package bisq.chat.priv;

import bisq.network.SendMessageResult;
import lombok.Getter;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * What a private chat send decided locally, before it stored anything, together with the delivery future
 * when it went ahead.
 * <p>
 * The two are separated because they answer different questions and become known at different times. The
 * rejection is settled by the time the send returns and says whether the message exists at all; the
 * future resolves later and says only whether it reached the peer. A caller that has to answer
 * synchronously — an HTTP endpoint — can read the first without waiting for the second, which for a DM
 * over Tor may take seconds or go to a mailbox.
 * <p>
 * The future alone cannot carry both: a rejection and a delivery failure both surface as an exceptionally
 * completed future, and telling them apart is the difference between "nothing was stored, say so" and
 * "it was stored and is on its way, saying otherwise makes the client retry into a duplicate".
 * <p>
 * Not a record: accepted and rejected are the only two shapes, and only the factories below can build
 * them. A public constructor would also admit a rejection paired with a live delivery, or an acceptance
 * paired with a failed one.
 */
@Getter
public final class SendOutcome {
    public static SendOutcome rejected(SendRejection rejection) {
        // Kept as an exceptionally completed future so callers that only want the future - and there are
        // several, in desktop and the trade endpoints - see exactly what they saw before this type existed.
        return new SendOutcome(Optional.of(rejection),
                CompletableFuture.failedFuture(new RuntimeException(rejection.name())));
    }

    public static SendOutcome accepted(CompletableFuture<SendMessageResult> delivery) {
        return new SendOutcome(Optional.empty(), delivery);
    }

    private final Optional<SendRejection> rejection;
    private final CompletableFuture<SendMessageResult> delivery;

    private SendOutcome(Optional<SendRejection> rejection, CompletableFuture<SendMessageResult> delivery) {
        this.rejection = rejection;
        this.delivery = delivery;
    }

    public boolean isAccepted() {
        return rejection.isEmpty();
    }
}
