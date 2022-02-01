package bisq.social.intent;


import bisq.network.p2p.services.data.NetworkPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.chat.ChatPeer;

import java.util.concurrent.TimeUnit;

public record TradeIntent(String id, ChatPeer maker, String ask, String bid, long date) implements NetworkPayload {
    @Override
    public MetaData getMetaData() {
        return new MetaData(TimeUnit.MINUTES.toMillis(5), 100000, getClass().getSimpleName());
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }
}
