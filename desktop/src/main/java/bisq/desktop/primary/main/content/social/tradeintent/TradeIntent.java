package bisq.desktop.primary.main.content.social.tradeintent;

import bisq.network.p2p.message.Proto;

//todo move to offer domain
public record TradeIntent(String ask, String bid, long date) implements Proto {
}
