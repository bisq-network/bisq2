package bisq.network.p2p.node.authorization;

import bisq.network.p2p.node.Feature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuthorizationServiceTest {

    @Test
    void testSelectAuthorizationTokenType() {
        List<AuthorizationTokenType> myPreferredAuthorizationTokenTypes;
        List<Feature> peersFeatures;
        AuthorizationTokenType result;

        // Empty myPreferredAuthorizationTokenTypes not allowed
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            AuthorizationService.selectAuthorizationTokenType(new ArrayList<>(), new ArrayList<>());
        });

        // Empty peersFeatures, we use first myPreferredAuthorizationTokenTypes item as default
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, new ArrayList<>());
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, new ArrayList<>());
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);

        // Match
        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        // If not match we use first peersFeatures item as default
        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        // Multiple myPreferredAuthorizationTokenTypes
        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH, AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH, AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH, AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH, AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);


        // Multiple peersFeatures
        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH, Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH, Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH, Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH, Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);


        // Multiple myPreferredAuthorizationTokenTypes and peersFeatures
        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH, Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH, AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH, Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.HASH_CASH, AuthorizationTokenType.EQUI_HASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.HASH_CASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_HASH_CASH, Feature.AUTHORIZATION_EQUI_HASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH, AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);

        peersFeatures = List.of(Feature.AUTHORIZATION_EQUI_HASH, Feature.AUTHORIZATION_HASH_CASH);
        myPreferredAuthorizationTokenTypes = List.of(AuthorizationTokenType.EQUI_HASH, AuthorizationTokenType.HASH_CASH);
        result = AuthorizationService.selectAuthorizationTokenType(myPreferredAuthorizationTokenTypes, peersFeatures);
        assertEquals(AuthorizationTokenType.EQUI_HASH, result);
    }
}
