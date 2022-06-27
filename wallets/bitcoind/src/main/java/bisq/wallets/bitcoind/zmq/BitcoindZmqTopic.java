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

package bisq.wallets.bitcoind.zmq;

import lombok.Getter;

@Getter
public enum BitcoindZmqTopic {
    TOPIC_HASHBLOCK("hashblock"),
    TOPIC_RAWTX("rawtx");

    private static final String HASHBLOCK_TOPIC_NAME = "hashblock";
    private static final String RAWTX_TOPIC_NAME = "rawtx";

    private final String topicName;

    BitcoindZmqTopic(String topicName) {
        this.topicName = topicName;
    }

    public static BitcoindZmqTopic parse(String topicName) {
        switch (topicName) {
            case BitcoindZmqTopic.HASHBLOCK_TOPIC_NAME: {
                return TOPIC_HASHBLOCK;
            }
            case BitcoindZmqTopic.RAWTX_TOPIC_NAME: {
                return TOPIC_RAWTX;
            }
            default: {
                throw new IllegalStateException("Unknown ZMQ topic: " + topicName);
            }
        }
    }
}
