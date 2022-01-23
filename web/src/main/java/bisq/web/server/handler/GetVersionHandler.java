package bisq.web.server.handler;


import bisq.application.ApplicationVersion;
import bisq.web.json.JsonTransform;
import ratpack.handling.Context;
import ratpack.handling.Handler;


public class GetVersionHandler extends AbstractHandler implements Handler {

    public GetVersionHandler(JsonTransform jsonTransform) {
        super(jsonTransform);
    }

    @Override
    public void handle(Context ctx) {
        ctx.render(toJson("version", ApplicationVersion.VERSION));
    }
}
