package network.misq.web.server.handler;

import network.misq.web.json.JsonTransform;
import ratpack.handling.Handler;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractHandler implements Handler {

    private final JsonTransform jsonTransform;

    public AbstractHandler(JsonTransform jsonTransform) {
        this.jsonTransform = jsonTransform;
    }

    protected Map<String, Object> toMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    protected String toJson(Map<String, Object> map) {
        return jsonTransform.toJson(map);
    }

    protected String toJson(String key, Object value) {
        return jsonTransform.toJson(toMap(key, value));
    }

    protected String toJson(Throwable throwable) {
        Map<String, Object> map = toMap("error", throwable.getClass().getCanonicalName());
        map.put("message", throwable.getMessage());
        return jsonTransform.toJson(map);
    }
}
