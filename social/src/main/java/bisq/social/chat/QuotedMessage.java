package bisq.social.chat;

import java.io.Serializable;

public record QuotedMessage(String userName, byte[] pubKeyHash, String message) implements Serializable {
}
