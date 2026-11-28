package group16.webapp.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.ai.client.AiClient;
import group16.webapp.modules.ai.client.GeminiAiClient;
import group16.webapp.modules.ai.client.TaskDraftJsonSchema;
import group16.webapp.modules.ai.config.AiClientConfig;
import group16.webapp.modules.ai.config.AiProperties;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * AI-01 / BR-12: the real transport timeout (JDK request factory built by AiClientConfig) against a loopback
 * server. app.ai.timeout = 300 ms; the server only finishes after 3000 ms. CI tolerance: the call must fail
 * with AI_UNAVAILABLE after at least 250 ms (300 ms minus 50 ms clock/scheduling slack) and at most 1500 ms
 * (300 ms plus 1200 ms for slow CI), i.e. well before the server completes its response.
 */
class GeminiAiClientTimeoutTests {

	private static final Duration TIMEOUT = Duration.ofMillis(300);
	private static final long SERVER_DELAY_MS = 3000;
	private static final long MIN_ELAPSED_MS = 250;
	private static final long MAX_ELAPSED_MS = 1500;
	private static final String PATH = "/v1beta/models/gemini-3.5-flash-lite:generateContent";
	private static final String FIRST_CHUNK = "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":\"";
	private static final String REST = "{}\"}]},\"finishReason\":\"STOP\"}]}";

	private final CountDownLatch release = new CountDownLatch(1);
	private final CountDownLatch requestReceived = new CountDownLatch(1);
	private final AtomicBoolean responseCompleted = new AtomicBoolean(false);
	private HttpServer server;
	private ExecutorService serverThreads;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		serverThreads = Executors.newCachedThreadPool();
		server.setExecutor(serverThreads);
		server.start();
	}

	@AfterEach
	void stopServer() throws InterruptedException {
		release.countDown();
		server.stop(0);
		serverThreads.shutdownNow();
		serverThreads.awaitTermination(5, TimeUnit.SECONDS);
	}

	@Test
	void t1SlowHeadersFailWithinTheTimeout() throws Exception {
		server.createContext(PATH, exchange -> {
			requestReceived.countDown();
			awaitDelay();
			writeFullResponse(exchange);
		});

		assertTimesOut();
	}

	@Test
	void t2SlowBodyAfterHeadersAndPartialJsonFailsWithinTheTimeout() throws Exception {
		server.createContext(PATH, exchange -> {
			requestReceived.countDown();
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, 0); // chunked: headers go out now
			OutputStream body = exchange.getResponseBody();
			body.write(FIRST_CHUNK.getBytes(StandardCharsets.UTF_8));
			body.flush(); // headers + partial JSON reach the client before the delay
			awaitDelay();
			body.write(REST.getBytes(StandardCharsets.UTF_8));
			body.close();
			responseCompleted.set(true);
		});

		assertTimesOut();
	}

	private void assertTimesOut() throws InterruptedException {
		AiClient client = AiClientConfig.createClient(new AiProperties(TIMEOUT, new AiProperties.Gemini(
				"test-key", "gemini-3.5-flash-lite", "http://127.0.0.1:" + server.getAddress().getPort())));
		assertThat(client).isInstanceOf(GeminiAiClient.class);

		long start = System.nanoTime();
		assertThatThrownBy(() -> client.generateJson("Ôn tập chương 3", TaskDraftJsonSchema.SCHEMA))
				.isInstanceOfSatisfying(AppException.class,
						ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.AI_UNAVAILABLE));
		long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

		assertThat(requestReceived.await(0, TimeUnit.MILLISECONDS)).as("server received the request").isTrue();
		assertThat(responseCompleted).as("server had not finished its response").isFalse();
		assertThat(elapsedMs).as("elapsed ms").isBetween(MIN_ELAPSED_MS, MAX_ELAPSED_MS);
	}

	private void awaitDelay() {
		try {
			release.await(SERVER_DELAY_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	private void writeFullResponse(HttpExchange exchange) throws IOException {
		byte[] bytes = (FIRST_CHUNK + REST).getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, bytes.length);
		try (OutputStream body = exchange.getResponseBody()) {
			body.write(bytes);
		}
		responseCompleted.set(true);
	}
}
