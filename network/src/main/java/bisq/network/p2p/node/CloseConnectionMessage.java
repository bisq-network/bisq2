package bisq.network.p2p.node;

import bisq.network.p2p.message.Message;

record CloseConnectionMessage(CloseReason closeReason) implements Message {
}
