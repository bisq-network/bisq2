/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.http_api.rest_api.domain.payment_accounts;

import bisq.account.AccountService;
import bisq.account.accounts.Account;
import bisq.account.accounts.UserDefinedFiatAccount;
import bisq.account.payment_method.*;
import bisq.bisq_easy.BisqEasyServiceUtil;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.currency.Market;
import bisq.common.observable.collection.ObservableSet;
import bisq.dto.DtoMappings;
import bisq.dto.account.UserDefinedFiatAccountDto;
import bisq.dto.account.UserDefinedFiatAccountPayloadDto;
import bisq.dto.settings.SettingsDto;
import bisq.http_api.rest_api.domain.RestApiBase;
import bisq.http_api.rest_api.domain.offers.CreateOfferRequest;
import bisq.http_api.rest_api.domain.offers.CreateOfferResponse;
import bisq.http_api.rest_api.domain.settings.SettingsChangeRequest;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.PriceSpec;
import bisq.user.identity.UserIdentity;
import bisq.user.profile.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Path("/payment_accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Payment Accounts API", description = "API for managing user payment accounts")
public class PaymentAccountsRestApi extends RestApiBase {
    private final AccountService accountService;

    public PaymentAccountsRestApi(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Operation(
            summary = "Get user payment accounts",
            description = "Retrieve the payment accounts",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment accounts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = SettingsDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getPaymentAccounts() {
        try {
            Map<String, Account<?, ? extends PaymentMethod<?>>> accountByNameMap = accountService.getAccountByNameMap();

            List<Account<?, ? extends PaymentMethod<?>>> accountsList = new ArrayList<>(accountByNameMap.values());

            List<UserDefinedFiatAccountDto> userAccounts = new ArrayList<>();
            for (Account<?, ? extends PaymentMethod<?>> account : accountsList) {
                if (account instanceof UserDefinedFiatAccount) {
                    UserDefinedFiatAccount castedAccount = (UserDefinedFiatAccount) account;
                    userAccounts.add(
                            new UserDefinedFiatAccountDto(
                                    castedAccount.getAccountName(),
                                    new UserDefinedFiatAccountPayloadDto(
                                            castedAccount.getAccountPayload().getAccountData()
                                    )
                            )
                    );
                }
            }

            return buildOkResponse(userAccounts);
        } catch (Exception e) {
            log.error("Failed to retrieve payment accounts", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Operation(
            summary = "Add new user payment account",
            description = "Add new user payment account",
            requestBody = @RequestBody(
                    description = "",
                    content = @Content(schema = @Schema(implementation = AddAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "",
                            content = @Content(schema = @Schema(example = ""))),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public void addAccount(AddAccountRequest request, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            accountService.addPaymentAccount(new UserDefinedFiatAccount(request.accountName(), request.accountData()));
            asyncResponse.resume(buildResponse(Response.Status.CREATED, new AddAccountResponse(request.accountName())));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @DELETE
    @Operation(
            summary = "Delete Payment account",
            description = "Delete Payment account",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Offer successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Offer or user identity not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    @Path("/{accountName}")
    public void removeAccount(@PathParam("accountName") String accountName, @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            UserDefinedFiatAccount account = new UserDefinedFiatAccount(
                    accountName, ""
            );
            accountService.removePaymentAccount(account);
            asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }
}