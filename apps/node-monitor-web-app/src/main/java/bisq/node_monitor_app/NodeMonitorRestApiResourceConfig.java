package bisq.node_monitor_app;

import bisq.api.ApiConfig;
import bisq.api.rest_api.BaseRestApiResourceConfig;
import bisq.api.validator.ApiRequestFilter;
import bisq.network.NetworkService;
import bisq.node_monitor.NodeMonitorRestApi;
import bisq.node_monitor.NodeMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
public class NodeMonitorRestApiResourceConfig extends BaseRestApiResourceConfig {
    public NodeMonitorRestApiResourceConfig(ApiConfig apiConfig,
                                            NetworkService networkService,
                                            NodeMonitorService nodeMonitorService
    ) {
        super(apiConfig);

        // Swagger/OpenApi does not work when using instances at register instead of classes.
        // As we want to pass the dependencies in the constructor, so we need the hack
        // with AbstractBinder to register resources as classes for Swagger
        register(NodeMonitorRestApi.class);

        register(new AbstractBinder() {
            @Override
            protected void configure() {
                bind(new NodeMonitorRestApi(networkService, nodeMonitorService)).to(NodeMonitorRestApi.class);
            }
        });
    }


    @Override
    protected ApiRequestFilter getApiRequestFilter(ApiConfig apiConfig) {
        return new ApiRequestFilter(apiConfig.getRestAllowEndpoints(), apiConfig.getRestDenyEndpoints());
    }
}
