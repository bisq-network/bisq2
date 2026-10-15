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

package bisq.wallet;

import bisq.persistence.PersistenceService;
import bisq.wallet.vo.AddressBalance;
import bisq.wallet.vo.Utxo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {
    @TempDir
    private Path tempDirPath;
    private MockWalletService walletService;

    @BeforeEach
    void setUp() {
        WalletService.Config config = new WalletService.Config(true, "localhost", 50051);
        PersistenceService persistenceService = new PersistenceService(tempDirPath);
        walletService = new MockWalletService(config, persistenceService);
    }

    /**
     * Test that requestAddressBalances() correctly aggregates UTXOs by address.
     */
    @Test
    void requestAddressBalances_aggregatesUtxosByAddress() throws ExecutionException, InterruptedException {
        // Act: request address balances from wallet service
        var observableSet = walletService.requestAddressBalances().get();

        // Assert: verify the observable set is populated with address balances
        assertNotNull(observableSet);
        assertFalse(observableSet.isEmpty(), "Address balances should not be empty");

        // Get all address balances
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Verify we have the expected addresses (from mock data)
        var addresses = addressBalances.stream().map(AddressBalance::getAddress).collect(Collectors.toSet());
        assertTrue(addresses.contains("bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"), "Expected address should be present");
        assertTrue(addresses.contains("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"), "Expected address should be present");
    }

    /**
     * Test that amounts are correctly summed for addresses with multiple UTXOs.
     */
    @Test
    void requestAddressBalances_correctlyAggregatesAmounts() throws ExecutionException, InterruptedException {
        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Assert: address "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa" has 2 UTXOs (u2: 200000, u7: 125000)
        AddressBalance addressBalance = addressBalances.stream()
                .filter(ab -> ab.getAddress().equals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Address balance not found"));

        // Expected amount: 200000 + 125000 = 325000
        assertEquals(325000L, addressBalance.getAmount(), "Amount should be sum of all UTXOs for this address");
    }

    /**
     * Test that numUsage correctly counts the number of UTXOs per address.
     */
    @Test
    void requestAddressBalances_correctlyCountsUtxos() throws ExecutionException, InterruptedException {
        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Assert: address "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa" should have numUsage=2
        AddressBalance addressBalance = addressBalances.stream()
                .filter(ab -> ab.getAddress().equals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Address balance not found"));

        assertEquals(2, addressBalance.getNumUsage(), "NumUsage should be 2 (two UTXOs for this address)");
    }

    /**
     * Test that numConfirmations is the minimum of all UTXOs for an address.
     */
    @Test
    void requestAddressBalances_correctlyCalculatesMinConfirmations() throws ExecutionException, InterruptedException {
        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Assert: address "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa" has UTXOs with confirmations 3 and 2
        // The minimum should be 2
        AddressBalance addressBalance = addressBalances.stream()
                .filter(ab -> ab.getAddress().equals("1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Address balance not found"));

        assertEquals(2, addressBalance.getNumConfirmations(), 
                "NumConfirmations should be minimum (2) of all UTXOs for this address");
    }

    /**
     * Test that a single-UTXO address is correctly aggregated (trivial case).
     */
    @Test
    void requestAddressBalances_handlesSingleUtxoAddress() throws ExecutionException, InterruptedException {
        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().collect(Collectors.toList());

        // Assert: address "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh" has 1 UTXO
        AddressBalance addressBalance = addressBalances.stream()
                .filter(ab -> ab.getAddress().equals("bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Address balance not found"));

        assertEquals(100000L, addressBalance.getAmount(), "Single UTXO amount should be as-is");
        assertEquals(1, addressBalance.getNumUsage(), "Single UTXO address should have numUsage=1");
        assertEquals(6, addressBalance.getNumConfirmations(), "Single UTXO confirmations should match");
    }

    /**
     * Test that all UTXOs from listUtxos() are accounted for in the aggregation.
     */
    @Test
    void requestAddressBalances_accountsForAllUtxos() throws ExecutionException, InterruptedException {
        // Get raw UTXOs from mock service
        List<Utxo> allUtxos = walletService.listUtxos().get();

        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Assert: total sum of amounts in address balances equals sum of all UTXO amounts
        long expectedTotalAmount = allUtxos.stream().mapToLong(Utxo::getAmount).sum();
        long actualTotalAmount = addressBalances.stream().mapToLong(AddressBalance::getAmount).sum();

        assertEquals(expectedTotalAmount, actualTotalAmount, 
                "Total amount in address balances should equal sum of all UTXOs");

        // Assert: total numUsage equals total number of UTXOs
        long expectedTotalUsage = allUtxos.size();
        long actualTotalUsage = addressBalances.stream().mapToLong(AddressBalance::getNumUsage).sum();

        assertEquals(expectedTotalUsage, actualTotalUsage, 
                "Total numUsage should equal total UTXO count");
    }

    /**
     * Test that the observable set remains in sync with future calls (idempotence).
     */
    @Test
    void requestAddressBalances_returnsSameObservableSet() throws ExecutionException, InterruptedException {
        // Act: request address balances twice
        var observableSet1 = walletService.requestAddressBalances().get();
        var observableSet2 = walletService.requestAddressBalances().get();

        // Assert: both calls return the same observable set instance (idempotent)
        assertSame(observableSet1, observableSet2, "requestAddressBalances should return the same observable set instance");
    }

    /**
     * Test that address balance with 0 confirmations is handled correctly.
     */
    @Test
    void requestAddressBalances_handlesUnconfirmedUtxo() throws ExecutionException, InterruptedException {
        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Assert: find address with 0 confirmations (u3 and u6 in mock data have 0 confirmations)
        // u3: address "3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy", 0 confirmations
        AddressBalance addressBalance = addressBalances.stream()
                .filter(ab -> ab.getAddress().equals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNLy"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Address balance not found"));

        assertEquals(0, addressBalance.getNumConfirmations(), 
                "Address with unconfirmed UTXO should have numConfirmations=0");
    }

    /**
     * Test that addresses are unique in the result (no duplicates).
     */
    @Test
    void requestAddressBalances_noduplicateAddresses() throws ExecutionException, InterruptedException {
        // Act: request address balances
        var observableSet = walletService.requestAddressBalances().get();
        List<AddressBalance> addressBalances = observableSet.stream().toList();

        // Assert: all addresses are unique
        long uniqueAddressCount = addressBalances.stream()
                .map(AddressBalance::getAddress)
                .distinct()
                .count();

        assertEquals(addressBalances.size(), uniqueAddressCount, 
                "All addresses in result should be unique (no duplicates)");
    }
}

