package bisq.tor.controller.events.events;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@ToString
public abstract class HsDescEvent {
    @Getter
    public enum Action {
        CREATED(7),
        FAILED(8),
        RECEIVED(7),
        UPLOAD(8),
        UPLOADED(6);

        private final int numberOfArgs;

        Action(int numberOfArgs) {
            this.numberOfArgs = numberOfArgs;
        }

        public boolean isAction(String[] parts) {
            // Example: 650 HS_DESC CREATED <onion_address> UNKNOWN UNKNOWN <descriptor_id>
            return parts[2].equals(this.toString()) && parts.length == numberOfArgs;
        }
    }

    protected final Action action;
    protected final String hsAddress;
    protected final String authType;
    protected final String hsDir;
}
