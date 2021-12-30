package network.misq.web.server.handler;


import network.misq.application.DefaultServiceProvider;
import network.misq.presentation.offer.OfferEntity;
import network.misq.web.json.JsonTransform;
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
        List<OfferEntity> offers = serviceProvider.getOfferEntityService().getOfferEntities();
        ctx.render(toJson("offers", offers));
    }
}
