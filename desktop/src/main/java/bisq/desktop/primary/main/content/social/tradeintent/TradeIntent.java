package bisq.desktop.primary.main.content.social.tradeintent;


import bisq.network.p2p.message.Proto;
import bisq.social.chat.ChatUser;

//todo move to offer domain
public record TradeIntent(String id, ChatUser maker, String ask, String bid, long date) implements Proto {
}
