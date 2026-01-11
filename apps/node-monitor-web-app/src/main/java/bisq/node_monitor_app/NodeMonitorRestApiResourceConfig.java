package bisq.node_monitor_app;

import bisq.api.ApiConfig;
import bisq.api.access.filter.authn.SessionAuthenticationService;
import bisq.api.access.permissions.PermissionService;
import bisq.api.access.permissions.RestPermissionMapping;
import bisq.api.rest_api.BaseRestApiResourceConfig;
import bisq.network.NetworkService;
import bisq.node_monitor.NodeMonitorRestApi;
import bisq.node_monitor.NodeMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.inject.AbstractBinder;

@Slf4j
public class NodeMonitorRestApiResourceConfig extends BaseRestApiResourceConfig {
    public NodeMonitorRestApiResourceConfig(ApiConfig apiConfig,
                                            PermissionService<RestPermissionMapping> permissionService,
                                            SessionAuthenticationService sessionAuthenticationService,
                                            NetworkService networkService,
                                            NodeMonitorService nodeMonitorService
    ) {
        super(apiConfig, permissionService, sessionAuthenticationService);

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
