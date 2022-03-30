package bisq.social.intent;


import bisq.network.p2p.services.data.storage.DistributedData;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.social.user.ChatUser;
import com.google.protobuf.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
// Note: will get probably removed
public record TradeIntent(String id, ChatUser maker, String ask, String bid, long date) implements DistributedData {
    @Override
    public MetaData getMetaData() {
        return new MetaData(TimeUnit.MINUTES.toMillis(5), 100000, getClass().getSimpleName());
    }

    @Override
    public boolean isDataInvalid() {
        return false;
    }

    @Override
    public Message toProto() {
        log.error("Not impl yet");
        return null;
    }
}
