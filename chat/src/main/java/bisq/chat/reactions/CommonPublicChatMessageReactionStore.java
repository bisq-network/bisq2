package bisq.chat.reactions;

import bisq.common.proto.ProtoResolver;
import bisq.persistence.PersistableStore;
import com.google.protobuf.Message;

public class CommonPublicChatMessageReactionStore implements PersistableStore<CommonPublicChatMessageReactionStore> {
    //private final

    @Override
    public CommonPublicChatMessageReactionStore getClone() {
        return null;
    }

    @Override
    public void applyPersisted(CommonPublicChatMessageReactionStore persisted) {

    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return null;
    }

    @Override
    public Message.Builder getBuilder(boolean serializeForHash) {
        return null;
    }

    @Override
    public Message toProto(boolean serializeForHash) {
        return null;
    }
}
