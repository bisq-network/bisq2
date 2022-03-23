package bisq.network.p2p.message;

import bisq.common.encoding.Proto;

public record TextData(String text) implements Proto {
}
