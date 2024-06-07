package bisq.tor.controller.events.events;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


@SuperBuilder
@Getter
@ToString(callSuper = true)
public class HsDescCreatedEvent extends HsDescEvent {
    private final String descriptorId;
}
