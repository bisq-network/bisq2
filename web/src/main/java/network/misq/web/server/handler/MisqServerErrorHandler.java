package network.misq.web.server.handler;

import network.misq.web.json.JsonTransform;
import ratpack.error.ServerErrorHandler;
import ratpack.handling.Context;
import ratpack.path.InvalidPathEncodingException;

import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

public class MisqServerErrorHandler extends AbstractHandler implements ServerErrorHandler {

    public MisqServerErrorHandler(JsonTransform jsonTransform) {
        super(jsonTransform);
    }

    @Override
    public void error(Context context, Throwable throwable) {
        try {
            context.getResponse().status(INTERNAL_SERVER_ERROR.code()).send(toJson(throwable));
            throw throwable;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @Override
    public void error(Context context, InvalidPathEncodingException exception) throws Exception {
        ServerErrorHandler.super.error(context, exception);
    }

    @Override
    public void handle(Context ctx) {
        // not implemented
    }
}
