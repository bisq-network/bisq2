package bisq.web.server;

import bisq.application.DefaultServiceProvider;
import bisq.web.json.JsonTransform;
import bisq.web.server.handler.GetOffersHandler;
import bisq.web.server.handler.GetVersionHandler;
import ratpack.registry.RegistrySpec;
import ratpack.registry.internal.DefaultRegistryBuilder;

/**
 * The registry makes misq apis and web handlers available through the ratpack context.
 */
class MisqRegistrySpec extends DefaultRegistryBuilder implements RegistrySpec {

    private final JsonTransform jsonTransform;

    MisqRegistrySpec(DefaultServiceProvider coreApi) {
        this.jsonTransform = new JsonTransform();
        init(coreApi);
    }

    void init(DefaultServiceProvider serviceProvider) {
        add(DefaultServiceProvider.class, serviceProvider);
        add(GetVersionHandler.class, new GetVersionHandler(jsonTransform));
        add(GetOffersHandler.class, new GetOffersHandler(jsonTransform));
    }
}
