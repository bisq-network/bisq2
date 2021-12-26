package network.misq.web.server;

import network.misq.api.DefaultApi;
import network.misq.web.json.JsonTransform;
import network.misq.web.server.handler.GetOffersHandler;
import network.misq.web.server.handler.GetVersionHandler;
import ratpack.registry.RegistrySpec;
import ratpack.registry.internal.DefaultRegistryBuilder;

/**
 * The registry makes misq apis and web handlers available through the ratpack context.
 */
class MisqRegistrySpec extends DefaultRegistryBuilder implements RegistrySpec {

    private final JsonTransform jsonTransform;

    MisqRegistrySpec(DefaultApi coreApi) {
        this.jsonTransform = new JsonTransform();
        init(coreApi);
    }

    void init(DefaultApi api) {
        add(DefaultApi.class, api);
        add(GetVersionHandler.class, new GetVersionHandler(jsonTransform));
        add(GetOffersHandler.class, new GetOffersHandler(jsonTransform));
    }
}
