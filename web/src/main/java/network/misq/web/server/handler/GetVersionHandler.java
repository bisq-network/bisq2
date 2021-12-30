package network.misq.web.server.handler;


import network.misq.application.Version;
import network.misq.web.json.JsonTransform;
import ratpack.handling.Context;
import ratpack.handling.Handler;


public class GetVersionHandler extends AbstractHandler implements Handler {

    public GetVersionHandler(JsonTransform jsonTransform) {
        super(jsonTransform);
    }

    @Override
    public void handle(Context ctx) {
        ctx.render(toJson("version", Version.VERSION));
    }
}
