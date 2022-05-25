package bisq.api.jax;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.jaxrs2.Reader;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Path("openapi.json")
@Produces(MediaType.APPLICATION_JSON)
@Hidden
public class SwaggerResource {
    private static String swagger;

    @GET

    public String swagIt(@Context Application app) {
        if (swagger == null) {
            try {
                OpenAPI oas = new OpenAPI();
                Info info = new Info()
                        .title("Swagger Sample App bootstrap code")
                        .description("This is a sample server Petstore server.  You can find out more about Swagger " +
                                "at [http://swagger.io](http://swagger.io) or on [irc.freenode.net, #swagger](http://swagger.io/irc/).  For this sample, " +
                                "you can use the api key `special-key` to test the authorization filters.")
                        .termsOfService("http://swagger.io/terms/")
                        .contact(new Contact()
                                .email("apiteam@swagger.io"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html"));

                oas.info(info);
                SwaggerConfiguration oasConfig = new SwaggerConfiguration().openAPI(oas);

                Reader reader = new Reader(oasConfig);
                OpenAPI openAPI = reader.read(app.getClasses());
                swagger = Json.pretty(openAPI);
            } catch (RuntimeException e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }

        return swagger;
    }
}
