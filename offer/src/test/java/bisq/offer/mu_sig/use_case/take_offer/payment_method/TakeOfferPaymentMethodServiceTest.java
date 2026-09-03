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

package bisq.offer.mu_sig.use_case.take_offer.payment_method;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TakeOfferPaymentMethodServiceTest {

    @Test
    public void selectingAnotherMethodReplacesThePreviousSelection() {
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        PaymentMethod<?> sepaMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        Account<?, ?> wiseAccount = accountFor(wiseMethod);
        Account<?, ?> sepaAccount = accountFor(sepaMethod);

        TakeOfferPaymentMethodService service =
                new TakeOfferPaymentMethodService(new PaymentMethodSelectionService(market -> List.of()));
        service.putAccountsByPaymentMethod(wiseMethod, List.of(wiseAccount));
        service.putAccountsByPaymentMethod(sepaMethod, List.of(sepaAccount));

        service.onPaymentMethodSelected(wiseMethod);
        service.onPaymentMethodSelected(sepaMethod);

        assertEquals(1, service.getSelectedAccountByPaymentMethod().size());
        assertEquals(sepaAccount, service.getSelectedAccountByPaymentMethod().get(sepaMethod));
    }


    @Test
    public void selectingAnAccountWhosePaymentMethodDiffersFromTheKeyIsRejected() {
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        PaymentMethod<?> sepaMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.SEPA);
        Account<?, ?> sepaAccount = accountFor(sepaMethod);

        TakeOfferPaymentMethodService service =
                new TakeOfferPaymentMethodService(new PaymentMethodSelectionService(market -> List.of()));
        // Register the SEPA account under the WISE key so the eligibility guard passes and only
        // the payment-method match guard can reject the selection.
        service.putAccountsByPaymentMethod(wiseMethod, List.of(sepaAccount));

        assertThrows(IllegalArgumentException.class,
                () -> service.putSelectedAccountByPaymentMethod(wiseMethod, sepaAccount));
    }

    @Test
    public void selectingAnAccountThatIsNotInTheEligibleListIsRejected() {
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        Account<?, ?> wiseAccount = accountFor(wiseMethod);

        TakeOfferPaymentMethodService service =
                new TakeOfferPaymentMethodService(new PaymentMethodSelectionService(market -> List.of()));
        // No eligible accounts registered for the method.

        assertThrows(IllegalArgumentException.class,
                () -> service.putSelectedAccountByPaymentMethod(wiseMethod, wiseAccount));
    }

    @Test
    public void selectingAConsistentEligibleAccountSucceeds() {
        PaymentMethod<?> wiseMethod = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.WISE);
        Account<?, ?> wiseAccount = accountFor(wiseMethod);

        TakeOfferPaymentMethodService service =
                new TakeOfferPaymentMethodService(new PaymentMethodSelectionService(market -> List.of()));
        service.putAccountsByPaymentMethod(wiseMethod, List.of(wiseAccount));

        service.putSelectedAccountByPaymentMethod(wiseMethod, wiseAccount);

        assertEquals(1, service.getSelectedAccountByPaymentMethod().size());
        assertEquals(wiseAccount, service.getSelectedAccountByPaymentMethod().get(wiseMethod));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Account<?, ?> accountFor(PaymentMethod<?> paymentMethod) {
        Account account = mock(Account.class);
        when(account.getPaymentMethod()).thenReturn(paymentMethod);
        return account;
    }
}
