package network.misq.web.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;

// https://mkyong.com/java/java-11-httpclient-examples/

public class HttpClientApplication {
    private static final Logger log = LoggerFactory.getLogger(HttpClientApplication.class);

    public static void main(String[] args) throws Exception {
        doStreamingMoviesRequest();
    }

    public static void doStreamingMoviesRequest() throws IOException, URISyntaxException, InterruptedException {
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse<InputStream> httpResponse = httpClient.send(HttpRequest
                .newBuilder(new URI("http://localhost:5050/streaming-movies"))
                .version(HttpClient.Version.HTTP_1_1)
                .GET().build(), HttpResponse.BodyHandlers.ofInputStream());
        InputStream eventStream = httpResponse.body();
        BufferedReader br = new BufferedReader(new InputStreamReader(eventStream));
        Predicate<String> hasData = (l) -> l != null && l.length() > 0;
        String line = "";
        try {
            while ((line = br.readLine()) != null) {
                if (hasData.test(line))
                    log.info("CLIENT -> {}", line);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to read status event stream", e);
        }
    }

    public static void doBidiStreamingMoviesRequest() throws IOException, URISyntaxException, InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        HttpClient httpClient = HttpClient.newBuilder()
                .executor(executorService)
                .build();
        List<URI> targets = Arrays.asList(
                new URI("http://localhost:5050/bidi-streaming-movies"),
                new URI("http://localhost:5050/bidi-streaming-movies/M"),
                new URI("http://localhost:5050/bidi-streaming-movies/G2"),
                new URI("http://localhost:5050/bidi-streaming-movies/2001"));
        List<HttpRequest> requests = Arrays.asList(
                HttpRequest.newBuilder(targets.get(0))
                        .version(HttpClient.Version.HTTP_1_1)
                        .GET()
                        .build(),
                HttpRequest.newBuilder(targets.get(1))
                        .version(HttpClient.Version.HTTP_1_1)
                        .GET()
                        .build(),
                HttpRequest.newBuilder(targets.get(2))
                        .version(HttpClient.Version.HTTP_1_1)
                        .GET()
                        .build(),
                HttpRequest.newBuilder(targets.get(3))
                        .version(HttpClient.Version.HTTP_1_1)
                        .GET()
                        .build());
        HttpResponse.BodyHandler<InputStream> bodyHandler = (rspInfo) -> rspInfo.statusCode() == 200
                ? HttpResponse.BodySubscribers.ofInputStream()
                : HttpResponse.BodySubscribers.ofInputStream();

        CompletableFuture<HttpResponse<InputStream>> response1 = httpClient.sendAsync(requests.get(0), bodyHandler);
        CompletableFuture<HttpResponse<InputStream>> response2 = httpClient.sendAsync(requests.get(1), bodyHandler);
        CompletableFuture<HttpResponse<InputStream>> response3 = httpClient.sendAsync(requests.get(2), bodyHandler);
        CompletableFuture<HttpResponse<InputStream>> response4 = httpClient.sendAsync(requests.get(3), bodyHandler);

        executorService.submit(new StreamProcessor(1, response1.get().body()));
        executorService.submit(new StreamProcessor(2, response2.get().body()));
        executorService.submit(new StreamProcessor(3, response3.get().body()));
        executorService.submit(new StreamProcessor(4, response4.get().body()));
    }

    private static class StreamProcessor implements Runnable {

        private final int requestNumber;
        private final InputStream eventStream;

        public StreamProcessor(int requestNumber, InputStream eventStream) {
            this.requestNumber = requestNumber;
            this.eventStream = eventStream;
        }

        @Override
        public void run() {
            processEventStream();
        }

        public void processEventStream() {
            BufferedReader br = new BufferedReader(new InputStreamReader(eventStream));
            Predicate<String> hasData = (l) -> l != null && l.length() > 0;
            String line = "";
            try {
                while ((line = br.readLine()) != null) {
                    if (hasData.test(line))
                        log.info("CLIENT Request {} -> {}", requestNumber, line);
                }
            } catch (IOException e) {
                throw new RuntimeException("Unable to read status event stream", e);
            }
        }
    }

}

