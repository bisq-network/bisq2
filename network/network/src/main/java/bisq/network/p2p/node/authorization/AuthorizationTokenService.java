package bisq.network.p2p.node.authorization;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.network_load.NetworkLoad;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
public abstract class AuthorizationTokenService<T extends AuthorizationToken> {

    abstract public T createToken(EnvelopePayloadMessage message,
                                  NetworkLoad networkLoad,
                                  String peerAddress,
                                  int messageCounter);

    public abstract boolean isAuthorized(EnvelopePayloadMessage message,
                                         AuthorizationToken authorizationToken,
                                         NetworkLoad currentNetworkLoad,
                                         Optional<NetworkLoad> previousNetworkLoad,
                                         String connectionId,
                                         String myAddress);
}
