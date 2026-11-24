/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.api.rest_api.endpoints.user_identity;

import bisq.bisq_easy.BisqEasyService;
import bisq.user.identity.UserIdentityService;
import jakarta.ws.rs.container.AsyncResponse;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserIdentityRestApiTest {
    /**
     * Creating an identity waits for its new onion service, which takes over 10 seconds in a few
     * percent of cases on Tor. A shorter timeout answers 503 while the node still creates the profile.
     */
    @Test
    void createUserIdentity_usesTheCommonAsyncTimeout() {
        // SecurityService cannot be mocked on the api test classpath (bouncycastle signer mismatch); unused here
        UserIdentityRestApi api = new UserIdentityRestApi(null,
                mock(UserIdentityService.class),
                mock(BisqEasyService.class));
        AsyncResponse asyncResponse = mock(AsyncResponse.class);

        api.createUserIdentity(new CreateUserIdentityRequest(), asyncResponse);

        verify(asyncResponse).setTimeout(120, TimeUnit.SECONDS);
    }
}
