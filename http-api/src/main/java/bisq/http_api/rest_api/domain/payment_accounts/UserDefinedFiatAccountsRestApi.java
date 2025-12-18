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
import bisq.account.accounts.UserDefinedFiatAccountPayload;
import bisq.account.payment_method.*;
import bisq.dto.DtoMappings;
import bisq.dto.account.UserDefinedFiatAccountDto;
import bisq.http_api.rest_api.domain.RestApiBase;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Path("/payment-accounts/fiat/user-defined-fiat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "User-Defined Fiat Accounts API", description = "API for managing user-defined fiat payment accounts. Future endpoints will support structured account types like SEPA, Revolut, Zelle, etc.")
public class UserDefinedFiatAccountsRestApi extends RestApiBase {
    private final AccountService accountService;

    public UserDefinedFiatAccountsRestApi(AccountService accountService) {
        this.accountService = accountService;
    }

    @GET
    @Operation(
            summary = "Get user-defined fiat accounts",
            description = "Retrieve all user-defined fiat payment accounts",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User-defined fiat accounts retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserDefinedFiatAccountDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public Response getPaymentAccounts() {
        try {
            List<UserDefinedFiatAccountDto> userAccounts = accountService.getAccounts().stream()
                    .filter(account -> account instanceof UserDefinedFiatAccount)
                    .map(account -> (UserDefinedFiatAccount) account)
                    .map(DtoMappings.UserDefinedFiatAccountMapping::fromBisq2Model)
                    .collect(Collectors.toList());

            return buildOkResponse(userAccounts);
        } catch (Exception e) {
            log.error("Failed to retrieve payment accounts", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GET
    @Operation(
            summary = "Get selected user-defined fiat account",
            description = "Get the currently selected user-defined fiat payment account",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Selected user-defined fiat account retrieved successfully",
                            content = @Content(schema = @Schema(implementation = UserDefinedFiatAccountDto.class))),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/selected")
    public Response getSelectedPaymentAccount() {
        try {
            if (accountService.getSelectedAccount().isPresent()) {
                Account<?, ? extends PaymentMethod<?>> account = accountService.getSelectedAccount().get();
                if (account instanceof UserDefinedFiatAccount castedAccount) {
                    UserDefinedFiatAccountDto userAccount = DtoMappings.UserDefinedFiatAccountMapping.fromBisq2Model(castedAccount);
                    return buildOkResponse(userAccount);
                } else {
                    return buildResponse(Response.Status.NO_CONTENT, "");
                }
            }

            return buildResponse(Response.Status.NO_CONTENT, "");
        } catch (Exception e) {
            log.error("Failed to retrieve payment accounts", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }

    @POST
    @Operation(
            summary = "Add new user-defined fiat account",
            description = "Create a new user-defined fiat payment account",
            requestBody = @RequestBody(
                    description = "Account name and account data for the new user-defined fiat account",
                    content = @Content(schema = @Schema(implementation = AddAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "User-defined fiat account created successfully",
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
            summary = "Delete user-defined fiat account",
            description = "Delete a user-defined fiat payment account by account name",
            responses = {
                    @ApiResponse(responseCode = "204", description = "User-defined fiat account successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Invalid input"),
                    @ApiResponse(responseCode = "404", description = "Account not found"),
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
            Optional<Account<?, ? extends PaymentMethod<?>>> result = accountService.findAccount(accountName);
            if (result.isPresent()) {
                Account<?, ? extends PaymentMethod<?>> toRemove = result.get();
                accountService.removePaymentAccount(toRemove);
                asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
            } else {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Payment account not found"));
            }
        } catch (Exception e) {
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PUT
    @Operation(
            summary = "Update user-defined fiat account",
            description = "Update an existing user-defined fiat payment account by replacing it with new data",
            requestBody = @RequestBody(
                    description = "Account name and new account data for the user-defined fiat account",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SaveAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "User-defined fiat account updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data or account not found"),
                    @ApiResponse(responseCode = "404", description = "User-defined fiat account not found"),
                    @ApiResponse(responseCode = "500", description = "Internal server error"),
                    @ApiResponse(responseCode = "503", description = "Request timed out")
            }
    )
    @Path("/{accountName}")
    public void saveAccount(@PathParam("accountName") String accountName,
                            @Valid SaveAccountRequest request,
                            @Suspended AsyncResponse asyncResponse) {
        asyncResponse.setTimeout(10, TimeUnit.SECONDS);
        asyncResponse.setTimeoutHandler(response -> {
            response.resume(buildResponse(Response.Status.SERVICE_UNAVAILABLE, "Request timed out"));
        });
        try {
            Optional<Account<?, ? extends PaymentMethod<?>>> result = accountService.findAccount(accountName);
            if (result.isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.NOT_FOUND, "Payment account not found"));
                return;
            }

            Account<?, ? extends PaymentMethod<?>> selectedAccount = result.get();
            if (!(selectedAccount instanceof UserDefinedFiatAccount)) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account is not a UserDefinedFiatAccount"));
                return;
            }

            String accountData = request.accountData();
            if (accountData == null || accountData.isEmpty()) {
                asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, "Account data cannot be empty"));
                return;
            }

            checkArgument(accountData.length() <= UserDefinedFiatAccountPayload.MAX_DATA_LENGTH,
                    "Account data must not be longer than " + UserDefinedFiatAccountPayload.MAX_DATA_LENGTH + " characters");

            UserDefinedFiatAccount newAccount = new UserDefinedFiatAccount(request.accountName(), request.accountData());
            accountService.updatePaymentAccount(accountName, newAccount);
            asyncResponse.resume(buildResponse(Response.Status.NO_CONTENT, ""));
        } catch (IllegalArgumentException e) {
            log.error("Invalid account data", e);
            asyncResponse.resume(buildErrorResponse(Response.Status.BAD_REQUEST, e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to save payment account", e);
            asyncResponse.resume(buildErrorResponse("An unexpected error occurred: " + e.getMessage()));
        }
    }

    @PATCH
    @Operation(
            summary = "Set selected user-defined fiat account",
            description = "Set the currently selected user-defined fiat payment account",
            requestBody = @RequestBody(
                    description = "The user-defined fiat account to set as selected",
                    required = true,
                    content = @Content(schema = @Schema(implementation = PaymentAccountChangeRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Selected user-defined fiat account set successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid input data"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    @Path("/selected")
    public Response setSelectedPaymentAccount(@Valid PaymentAccountChangeRequest request) {
        try {
            UserDefinedFiatAccountDto account = request.selectedAccount();
            if (account != null) {
                Optional<Account<?, ? extends PaymentMethod<?>>> result = accountService.findAccount(account.accountName());
                if (result.isPresent()) {
                    Account<?, ? extends PaymentMethod<?>> foundAccount = result.get();
                    if (foundAccount instanceof UserDefinedFiatAccount castedAccount) {
                        accountService.setSelectedAccount(castedAccount);
                    } else {
                        return buildErrorResponse(Response.Status.BAD_REQUEST, "Payment account not found");
                    }
                } else {
                    return buildErrorResponse(Response.Status.BAD_REQUEST, "Payment account not found");
                }
            }

            log.info("Updated selected payment account from request: {}", request);
            return Response.noContent().build();
        } catch (Exception e) {
            log.error("Error updating select payment account", e);
            return buildErrorResponse("An unexpected error occurred: " + e.getMessage());
        }
    }
}