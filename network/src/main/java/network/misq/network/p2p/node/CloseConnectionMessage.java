package network.misq.network.p2p.node;

import network.misq.network.p2p.message.Message;

record CloseConnectionMessage(CloseReason closeReason) implements Message {
}
