package bisq.web.server.handler;


import bisq.application.DefaultServiceProvider;
import bisq.presentation.offer.OfferPresentation;
import bisq.web.json.JsonTransform;
import ratpack.handling.Context;
import ratpack.handling.Handler;

import java.util.List;


public class GetOffersHandler extends AbstractHandler implements Handler {

    public GetOffersHandler(JsonTransform jsonTransform) {
        super(jsonTransform);
    }

    @Override
    public void handle(Context ctx) {
        DefaultServiceProvider serviceProvider = ctx.get(DefaultServiceProvider.class);
        List<OfferPresentation> offers = serviceProvider.getOfferPresentationService().getOfferEntities();
        ctx.render(toJson("offers", offers));
    }
}
