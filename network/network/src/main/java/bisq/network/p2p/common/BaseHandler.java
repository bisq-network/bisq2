package bisq.network.p2p.common;

import java.util.concurrent.ThreadLocalRandom;

abstract class BaseHandler {
    protected String createRequestId() {
        return String.valueOf(createNonce());
    }

    protected int createNonce() {
        return ThreadLocalRandom.current().nextInt();
    }
}
