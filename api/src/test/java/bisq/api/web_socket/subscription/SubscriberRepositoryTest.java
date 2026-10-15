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

package bisq.api.web_socket.subscription;

import org.glassfish.grizzly.websockets.WebSocket;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SubscriberRepositoryTest {

    // ── findSubscribers(topic) – grouped ────────────────────────────────────────

    @Test
    void findSubscribersGroupsBySpecifier() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber s1 = addToRepo(repo, request("r1", Topic.ALERT_NOTIFICATIONS, "DESKTOP"), mock(WebSocket.class));
        Subscriber s2 = addToRepo(repo, request("r2", Topic.ALERT_NOTIFICATIONS, "DESKTOP"), mock(WebSocket.class));
        Subscriber s3 = addToRepo(repo, request("r3", Topic.ALERT_NOTIFICATIONS, "MOBILE_CLIENT"), mock(WebSocket.class));

        Map<SubscriptionSpecifier, Set<Subscriber>> groups =
                repo.findSubscribers(Topic.ALERT_NOTIFICATIONS);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(new SubscriptionSpecifier(Topic.ALERT_NOTIFICATIONS, Optional.of("DESKTOP")))).containsExactlyInAnyOrder(s1, s2);
        assertThat(groups.get(new SubscriptionSpecifier(Topic.ALERT_NOTIFICATIONS, Optional.of("MOBILE_CLIENT")))).containsExactlyInAnyOrder(s3);
    }

    @Test
    void findSubscribersPlacesNoParamSubscribersUnderEmptySpecifier() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber s = addToRepo(repo, request("r1", Topic.MARKET_PRICE, null), mock(WebSocket.class));

        Map<SubscriptionSpecifier, Set<Subscriber>> groups =
                repo.findSubscribers(Topic.MARKET_PRICE);

        assertThat(groups).hasSize(1);
        assertThat(groups.get(new SubscriptionSpecifier(Topic.MARKET_PRICE, Optional.empty()))).containsExactly(s);
    }

    @Test
    void findSubscribersReturnsMixedGroups() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber withParam = addToRepo(repo, request("r1", Topic.OFFERS, "EUR"), mock(WebSocket.class));
        Subscriber withoutParam = addToRepo(repo, request("r2", Topic.OFFERS, null), mock(WebSocket.class));

        Map<SubscriptionSpecifier, Set<Subscriber>> groups =
                repo.findSubscribers(Topic.OFFERS);

        assertThat(groups).hasSize(2);
        assertThat(groups.get(new SubscriptionSpecifier(Topic.OFFERS, Optional.of("EUR")))).containsExactly(withParam);
        assertThat(groups.get(new SubscriptionSpecifier(Topic.OFFERS, Optional.empty()))).containsExactly(withoutParam);
    }

    @Test
    void findSubscribersGroupedReturnsEmptyMapForUnknownTopic() {
        SubscriberRepository repo = new SubscriberRepository();

        assertThat(repo.findSubscribers(Topic.MARKET_PRICE)).isEmpty();
    }

    @Test
    void findSubscribersReturnsDefensiveCopy() {
        SubscriberRepository repo = new SubscriberRepository();
        addToRepo(repo, request("r1", Topic.MARKET_PRICE, "x"), mock(WebSocket.class));

        Map<SubscriptionSpecifier, Set<Subscriber>> snapshot =
                repo.findSubscribers(Topic.MARKET_PRICE);
        // Adding a new subscriber must not affect the previously returned snapshot
        addToRepo(repo, request("r2", Topic.MARKET_PRICE, "x"), mock(WebSocket.class));

        assertThat(snapshot.get(new SubscriptionSpecifier(Topic.MARKET_PRICE, Optional.of("x")))).hasSize(1);
    }

    // ── findSubscribers(topic, parameter) ────────────────────────────────────

    @Test
    void findSubscribersByParamReturnsOnlyExactMatch() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber exact = addToRepo(repo, request("r1", Topic.OFFERS, "EUR"), mock(WebSocket.class));
        Subscriber noParam = addToRepo(repo, request("r2", Topic.OFFERS, null), mock(WebSocket.class));
        Subscriber otherParam = addToRepo(repo, request("r3", Topic.OFFERS, "USD"), mock(WebSocket.class));

        Set<Subscriber> result = repo.findSubscribers(Topic.OFFERS, Optional.of("EUR"));

        assertThat(result).containsExactly(exact);
        assertThat(result).doesNotContain(noParam, otherParam);
    }

    @Test
    void findSubscribersByParamReturnsEmptyWhenNoMatch() {
        SubscriberRepository repo = new SubscriberRepository();
        addToRepo(repo, request("r1", Topic.OFFERS, "USD"), mock(WebSocket.class));

        assertThat(repo.findSubscribers(Topic.OFFERS, Optional.of("EUR"))).isEmpty();
    }

    // ── remove ────────────────────────────────────────────────────────────────

    @Test
    void removeDeletesSubscriberFromCorrectParameterBucket() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber s1 = addToRepo(repo, request("r1", Topic.MARKET_PRICE, null), mock(WebSocket.class));
        Subscriber s2 = addToRepo(repo, request("r2", Topic.MARKET_PRICE, null), mock(WebSocket.class));

        repo.remove(s1);

        Set<Subscriber> remaining = allSubscribers(repo, Topic.MARKET_PRICE);
        assertThat(remaining).containsExactly(s2);
    }

    @Test
    void removeLastSubscriberForTopicLeavesNoEntry() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber s = addToRepo(repo, request("r1", Topic.MARKET_PRICE, null), mock(WebSocket.class));
        repo.remove(s);

        assertThat(allSubscribers(repo, Topic.MARKET_PRICE)).isEmpty();
        assertThat(repo.findSubscribers(Topic.MARKET_PRICE)).isEmpty(); // grouped
    }

    @Test
    void removeWorksAcrossMixedParameterBuckets() {
        SubscriberRepository repo = new SubscriberRepository();

        Subscriber desktop = addToRepo(repo, request("r1", Topic.ALERT_NOTIFICATIONS, "DESKTOP"), mock(WebSocket.class));
        Subscriber mobile = addToRepo(repo, request("r2", Topic.ALERT_NOTIFICATIONS, "MOBILE_CLIENT"), mock(WebSocket.class));

        repo.remove(desktop);

        Set<Subscriber> remaining = allSubscribers(repo, Topic.ALERT_NOTIFICATIONS);
        assertThat(remaining).containsExactly(mobile);
    }

    // ── onConnectionClosed ────────────────────────────────────────────────────

    @Test
    void onConnectionClosedRemovesAllSubscribersForSocket() {
        SubscriberRepository repo = new SubscriberRepository();
        WebSocket ws = mock(WebSocket.class);

        addToRepo(repo, request("r1", Topic.ALERT_NOTIFICATIONS, "DESKTOP"), ws);
        addToRepo(repo, request("r2", Topic.ALERT_NOTIFICATIONS, "MOBILE_CLIENT"), ws);
        Subscriber other = addToRepo(repo, request("r3", Topic.ALERT_NOTIFICATIONS, "DESKTOP"), mock(WebSocket.class));

        repo.onConnectionClosed(ws);

        Set<Subscriber> remaining = allSubscribers(repo, Topic.ALERT_NOTIFICATIONS);
        assertThat(remaining).containsExactly(other);
    }

    // ── concurrency ───────────────────────────────────────────────────────────

    /**
     * Exercises concurrent add / remove / find operations on the same repository from
     * multiple threads. Asserts:
     * - No exception is thrown (no ConcurrentModificationException or similar)
     * - Every subscriber that was added and NOT removed is still present
     * - Every subscriber that was removed is no longer present
     * - findSubscribers(topic) returns a consistent grouped snapshot at each point
     */
    @Test
    void concurrentAddRemoveAndFindAreSafe() throws InterruptedException {
        SubscriberRepository repo = new SubscriberRepository();
        int threads = 8;
        int opsPerThread = 200;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        AtomicInteger idSeq = new AtomicInteger();

        List<Subscriber> allAdded = new CopyOnWriteArrayList<>();
        List<Subscriber> allRemoved = new CopyOnWriteArrayList<>();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int t = 0; t < threads; t++) {
            final int threadIndex = t;
            pool.submit(() -> {
                try {
                    startGate.await();
                    for (int i = 0; i < opsPerThread; i++) {
                        String param = (threadIndex % 2 == 0) ? "DESKTOP" : "MOBILE_CLIENT";
                        String reqId = "r-" + idSeq.incrementAndGet();
                        Subscriber added = addToRepo(repo, request(reqId, Topic.ALERT_NOTIFICATIONS, param),
                                mock(WebSocket.class));
                        allAdded.add(added);

                        // Concurrently read — must not throw
                        repo.findSubscribers(Topic.ALERT_NOTIFICATIONS);
                        repo.findSubscribers(Topic.ALERT_NOTIFICATIONS, Optional.of("DESKTOP"));

                        // Remove every other subscriber from this thread to create churn
                        if (i % 2 == 0) {
                            repo.remove(added);
                            allRemoved.add(added);
                        }
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finished).as("All threads completed within timeout").isTrue();
        assertThat(errors).as("No exceptions thrown during concurrent access").isEmpty();

        // Collect the survivors: added but not removed
        List<Subscriber> expectedSurvivors = new ArrayList<>(allAdded);
        expectedSurvivors.removeAll(allRemoved);

        Set<Subscriber> actualSubscribers = allSubscribers(repo, Topic.ALERT_NOTIFICATIONS);

        assertThat(actualSubscribers)
                .containsExactlyInAnyOrderElementsOf(expectedSurvivors);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static Subscriber addToRepo(SubscriberRepository repo, SubscriptionRequest req, WebSocket ws) {
        Optional<String> canonical = Optional.ofNullable(req.getParameter()).filter(s -> !s.isBlank());
        return repo.add(req, canonical, ws);
    }

    private SubscriptionRequest request(String requestId, Topic topic, String parameter) {
        String paramPart = parameter != null ? ",\"parameter\":\"" + parameter + "\"" : "";
        String json = "{\"type\":\"SubscriptionRequest\",\"requestId\":\"" + requestId
                + "\",\"topic\":\"" + topic.name() + "\"" + paramPart + "}";
        return SubscriptionRequest.fromJson(json).orElseThrow();
    }

    private static Set<Subscriber> allSubscribers(SubscriberRepository repo, Topic topic) {
        return repo.findSubscribers(topic).values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }
}
