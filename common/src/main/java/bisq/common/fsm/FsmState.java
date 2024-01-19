package bisq.common.fsm;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public enum FsmState implements State {
    ANY(),
    ERROR(true);

    private final boolean isFinalState;

    FsmState() {
        this.isFinalState = false;
    }

    FsmState(boolean isFinalState) {
        this.isFinalState = isFinalState;
    }
}
