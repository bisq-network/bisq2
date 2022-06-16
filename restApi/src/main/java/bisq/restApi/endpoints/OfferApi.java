package bisq.restApi.endpoints;

import bisq.application.DefaultApplicationService;
import bisq.offer.OpenOfferService;
import bisq.restApi.RestApiApplication;
import bisq.restApi.dto.OfferDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Path("/offer")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Offer API")
public class OfferApi {

    protected final OpenOfferService openOfferService;

    public OfferApi(@Context Application application) {
        DefaultApplicationService appService = ((RestApiApplication) application).getApplicationService();
        openOfferService = appService.getOpenOfferService();
    }

    @GET
    @Path("/get-open-offer-book")
    @Operation(description = "Get a list of all open offers. Also it returns the priceDate, which is the date the prices in the offers were valid.")
    @ApiResponse(responseCode = "200", description = "request successful.",
            content = {
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = List.class)
                    )}
    )
    public List<OfferDto> getOpenOfferBook() {
        List<OfferDto> openOffers = openOfferService.getOpenOffers().stream() //
                .map(openOffer -> new OfferDto().loadFieldsFrom(openOffer.getOffer())) //
                .collect(Collectors.toList());

        return openOffers;
    }
}
