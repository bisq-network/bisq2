package bisq.web.server;

import bisq.application.DefaultApplicationService;
import bisq.web.json.JsonTransform;
import bisq.web.server.handler.GetOffersHandler;
import bisq.web.server.handler.GetVersionHandler;
import ratpack.registry.RegistrySpec;
import ratpack.registry.internal.DefaultRegistryBuilder;

/**
 * The registry makes bisq apis and web handlers available through the ratpack context.
 */
class BisqRegistrySpec extends DefaultRegistryBuilder implements RegistrySpec {

    private final JsonTransform jsonTransform;

    BisqRegistrySpec(DefaultApplicationService coreApi) {
        this.jsonTransform = new JsonTransform();
        init(coreApi);
    }

    void init(DefaultApplicationService applicationService) {
        add(DefaultApplicationService.class, applicationService);
        add(GetVersionHandler.class, new GetVersionHandler(jsonTransform));
        add(GetOffersHandler.class, new GetOffersHandler(jsonTransform));
    }
}
