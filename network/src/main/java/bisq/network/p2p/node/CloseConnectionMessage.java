package bisq.network.p2p.node;

import bisq.network.p2p.message.NetworkMessage;

record CloseConnectionMessage(CloseReason closeReason) implements NetworkMessage {
}
