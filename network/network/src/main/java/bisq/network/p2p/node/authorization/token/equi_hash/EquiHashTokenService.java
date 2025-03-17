package bisq.network.p2p.node.authorization.token.equi_hash;

import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.authorization.AuthorizationToken;
import bisq.network.p2p.node.authorization.AuthorizationTokenService;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * TODO: Implement once EquiHash is production ready
 */
@Slf4j
public class EquiHashTokenService extends AuthorizationTokenService<EquiHashToken> {

    public EquiHashTokenService(EquihashProofOfWorkService proofOfWorkService) {
    }

    @Override
    public EquiHashToken createToken(EnvelopePayloadMessage message,
                                     NetworkLoad networkLoad,
                                     String peerAddress,
                                     int messageCounter) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean isAuthorized(EnvelopePayloadMessage message,
                                AuthorizationToken authorizationToken,
                                NetworkLoad currentNetworkLoad,
                                Optional<NetworkLoad> previousNetworkLoad,
                                String connectionId,
                                String myAddress) {
        throw new RuntimeException("Not implemented yet");
    }
}
