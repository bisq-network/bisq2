package bisq.common.fsm;

import bisq.common.util.StringUtils;

/**
 * Wildcard state used for any state to be allowed as source state
 */
class AnySourceState implements State {
    @Override
    public boolean isFinalState() {
        return false;
    }

    @Override
    public String name() {
        return StringUtils.camelCaseToSnakeCase(getClass().getSimpleName());
    }
}
