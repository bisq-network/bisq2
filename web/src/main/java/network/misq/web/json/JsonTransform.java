package network.misq.web.json;

import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class JsonTransform {

    private final Gson gson = new GsonBuilder().serializeNulls().setPrettyPrinting().create();

    public String toJson(Map<String, Object> map) {
        log.info("{}: {}", map.keySet(), Joiner.on(",\n").join(map.values()));
        return gson.toJson(map, map.getClass());
    }
}
