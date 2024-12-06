package bisq.node_monitor_app;

import bisq.network.NetworkService;
import bisq.node_monitor.NodeMonitorRestApi;
import bisq.node_monitor.NodeMonitorService;
import bisq.http_api.rest_api.BaseRestApiResourceConfig;
import bisq.http_api.rest_api.RestApiService;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
public class NodeMonitorRestApiResourceConfig extends BaseRestApiResourceConfig {
    public NodeMonitorRestApiResourceConfig(RestApiService.Config config,
                                            NetworkService networkService,
                                            NodeMonitorService nodeMonitorService) {
        super(config.getRestApiBaseUrl());

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
}
