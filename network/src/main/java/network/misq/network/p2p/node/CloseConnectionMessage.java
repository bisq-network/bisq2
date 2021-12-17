package network.misq.network.p2p.node;

import network.misq.network.p2p.message.Message;

public record CloseConnectionMessage(Reason reason) implements Message {
    public enum Reason {
        DUPLICATE_CONNECTION,
        TOO_MANY_CONNECTIONS_TO_SEEDS,
        TOO_MANY_CONNECTIONS,
        ADDRESS_VALIDATION_COMPLETED
    }
}
