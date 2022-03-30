package bisq.web.server.handler;


import bisq.application.DefaultApplicationService;
import bisq.web.json.JsonTransform;
import ratpack.handling.Context;
import ratpack.handling.Handler;


public class GetOffersHandler extends AbstractHandler implements Handler {

    public GetOffersHandler(JsonTransform jsonTransform) {
        super(jsonTransform);
    }

    @Override
    public void handle(Context ctx) {
        DefaultApplicationService applicationService = ctx.get(DefaultApplicationService.class);
        // List<OfferPresentation> offers = applicationService.getOfferPresentationService().getOfferEntities();
        //  ctx.render(toJson("offers", offers));
    }
}
