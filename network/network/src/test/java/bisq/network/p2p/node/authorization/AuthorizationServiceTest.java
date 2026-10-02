package bisq.network.p2p.node.authorization;

import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.p2p.node.Feature;
import bisq.network.p2p.node.authorization.token.hash_cash_v2.HashCashV2Token;
import bisq.network.p2p.node.network_load.NetworkLoad;
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeRequest;
import bisq.security.pow.equihash.EquihashProofOfWorkService;
import bisq.security.pow.hashcash.HashCashProofOfWorkService;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class AuthorizationServiceTest {
    @Test
    void selectsByLocalPreferenceAndFallsBackToSupportedPeerType() {
        assertEquals(AuthorizationTokenType.HASH_CASH_V2, AuthorizationService.selectAuthorizationTokenType(
                List.of(AuthorizationTokenType.HASH_CASH_V2, AuthorizationTokenType.EQUI_HASH),
                List.of(Feature.AUTHORIZATION_EQUI_HASH, Feature.AUTHORIZATION_HASH_CASH_V2)));
        assertEquals(AuthorizationTokenType.EQUI_HASH, AuthorizationService.selectAuthorizationTokenType(
                List.of(AuthorizationTokenType.EQUI_HASH, AuthorizationTokenType.HASH_CASH_V2),
                List.of(Feature.AUTHORIZATION_HASH_CASH_V2, Feature.AUTHORIZATION_EQUI_HASH)));
        assertEquals(AuthorizationTokenType.EQUI_HASH, AuthorizationService.selectAuthorizationTokenType(
                List.of(AuthorizationTokenType.HASH_CASH_V2), List.of(Feature.AUTHORIZATION_EQUI_HASH)));
        assertEquals(AuthorizationTokenType.HASH_CASH_V2, AuthorizationService.selectAuthorizationTokenType(
                List.of(AuthorizationTokenType.HASH_CASH_V2), List.of()));
        assertThrows(IllegalArgumentException.class, () ->
                AuthorizationService.selectAuthorizationTokenType(List.of(), List.of()));
    }

    @Test
    void neverSelectsLegacyAuthorization() {
        assertTrue(AuthorizationTokenType.fromFeature(Feature.AUTHORIZATION_HASH_CASH).isEmpty());
        for (List<Feature> features : List.of(List.of(Feature.AUTHORIZATION_HASH_CASH),
                List.of(Feature.INVENTORY_HASH_SET))) {
            assertThrows(IllegalArgumentException.class, () -> AuthorizationService.selectAuthorizationTokenType(
                    List.of(AuthorizationTokenType.HASH_CASH_V2), features));
        }
        assertEquals(AuthorizationTokenType.HASH_CASH_V2, AuthorizationService.selectAuthorizationTokenType(
                List.of(AuthorizationTokenType.HASH_CASH_V2),
                List.of(Feature.AUTHORIZATION_HASH_CASH, Feature.AUTHORIZATION_HASH_CASH_V2)));
    }

    @Test
    void rejectsLegacyConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> AuthorizationTokenType.valueOf("HASH_CASH"));
        assertThrows(com.typesafe.config.ConfigException.BadValue.class, () -> AuthorizationService.Config.from(
                ConfigFactory.parseString("myPreferredAuthorizationTokenTypes=[HASH_CASH]")));
    }

    @ParameterizedTest
    @EnumSource(bisq.network.protobuf.AuthorizationTokenType.class)
    void rejectsLegacyTokenBodyRegardlessOfDeclaredType(bisq.network.protobuf.AuthorizationTokenType type) {
        var builder = bisq.network.protobuf.AuthorizationToken.newBuilder()
                .setHashCashToken(bisq.network.protobuf.HashCashToken.getDefaultInstance());
        if (type == bisq.network.protobuf.AuthorizationTokenType.UNRECOGNIZED) {
            builder.setAuthorizationTokenTypeValue(999);
        } else {
            builder.setAuthorizationTokenType(type);
        }
        assertThrows(UnresolvableProtobufException.class, () -> AuthorizationToken.fromProto(builder.build()));
    }

    @ParameterizedTest
    @EnumSource(value = bisq.network.protobuf.AuthorizationTokenType.class,
            names = {"AUTHORIZATIONTOKENTYPE_HASH_CASH", "AUTHORIZATIONTOKENTYPE_UNSPECIFIED", "UNRECOGNIZED"})
    void rejectsInvalidTypeEvenWithV2Body(bisq.network.protobuf.AuthorizationTokenType type) {
        assertThrows(UnresolvableProtobufException.class, () -> AuthorizationTokenType.fromProto(type));
        var builder = bisq.network.protobuf.AuthorizationToken.newBuilder()
                .setHashCashV2Token(bisq.network.protobuf.HashCashV2Token.getDefaultInstance());
        if (type == bisq.network.protobuf.AuthorizationTokenType.UNRECOGNIZED) {
            builder.setAuthorizationTokenTypeValue(999);
        } else {
            builder.setAuthorizationTokenType(type);
        }
        assertThrows(UnresolvableProtobufException.class, () -> AuthorizationToken.fromProto(builder.build()));
    }

    @Test
    void defaultFeaturesCreateAndAuthorizeV2() throws Exception {
        var service = new AuthorizationService(new AuthorizationService.Config(List.of(AuthorizationTokenType.HASH_CASH_V2)),
                new HashCashProofOfWorkService(), new EquihashProofOfWorkService(),
                Set.of(Feature.AUTHORIZATION_HASH_CASH, Feature.AUTHORIZATION_HASH_CASH_V2));
        assertEquals(Set.of(AuthorizationTokenType.HASH_CASH_V2), service.getSupportedServices().keySet());
        assertFalse(Feature.DEFAULT_FEATURES.contains(Feature.AUTHORIZATION_HASH_CASH));
        var message = new PeerExchangeRequest(1, new ArrayList<>());
        var token = service.createToken(message, NetworkLoad.INITIAL_NETWORK_LOAD, "peer", 0, Feature.DEFAULT_FEATURES);
        assertInstanceOf(HashCashV2Token.class, token);
        var decoded = AuthorizationToken.fromProto(token.toProto(false));
        assertTrue(service.isAuthorized(message, decoded, NetworkLoad.INITIAL_NETWORK_LOAD, "connection", "peer"));
        assertFalse(service.isAuthorized(message, decoded, NetworkLoad.INITIAL_NETWORK_LOAD, "connection", "peer"));
        assertThrows(IllegalArgumentException.class, () -> service.createToken(message,
                NetworkLoad.INITIAL_NETWORK_LOAD, "peer", 1, List.of(Feature.AUTHORIZATION_HASH_CASH)));
    }
}
