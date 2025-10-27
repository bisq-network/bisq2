package bisq.http_api.auth;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;

public class AuthenticationAddOn implements AddOn {
    private final String password;

    public AuthenticationAddOn(String password) {
        this.password = password;
    }

    public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
        int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);
        if (httpServerFilterIdx >= 0) {
            builder.add(httpServerFilterIdx, new WebSocketAuthenticationFilter(password));
        }
    }
}
