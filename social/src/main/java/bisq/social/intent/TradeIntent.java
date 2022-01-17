package bisq.social.intent;


import bisq.network.p2p.message.Proto;
import bisq.social.chat.ChatPeer;

public record TradeIntent(String id, ChatPeer maker, String ask, String bid, long date) implements Proto {
}
