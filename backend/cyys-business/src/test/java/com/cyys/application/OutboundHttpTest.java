package com.cyys.application;

import com.cyys.common.integration.OutboundHttpConfiguration;
import com.cyys.common.integration.OutboundHttpException;
import com.cyys.common.web.GlobalExceptionHandler;
import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.ssl.SslAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.HttpClientAutoConfiguration;
import org.springframework.boot.http.client.autoconfigure.imperative.ImperativeHttpClientAutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientAutoConfiguration;
import org.springframework.boot.restclient.autoconfigure.RestClientSsl;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.assertj.core.api.Assertions.*;

/** 真实环回网络和 TLS；不访问供应商、数据库或外网。 */
class OutboundHttpTest {
    @TempDir Path root;
    private HttpServer server;
    private ExecutorService workers;
    private String base;
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SslAutoConfiguration.class, HttpClientAutoConfiguration.class,
                    ImperativeHttpClientAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
                    RestClientAutoConfiguration.class))
            .withUserConfiguration(OutboundHttpConfiguration.class, com.cyys.common.web.HttpJsonConfiguration.class)
            .withPropertyValues("spring.http.clients.imperative.factory=jdk", "spring.http.clients.connect-timeout=1s",
                    "spring.http.clients.read-timeout=2s", "spring.http.clients.redirects=dont-follow",
                    "spring.http.clients.cookie-handling=disable");

    @BeforeEach void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        workers = Executors.newVirtualThreadPerTaskExecutor(); server.setExecutor(workers); server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }
    @AfterEach void stop() {
        RequestContextHolder.resetRequestAttributes(); server.stop(0); workers.shutdownNow();
    }

    @Test void queryFormAndTextUseActualUtf8WireEncoding() {
        AtomicReference<String> query = new AtomicReference<>(), body = new AtomicReference<>(), contentType = new AtomicReference<>();
        server.createContext("/echo", exchange -> {
            query.set(exchange.getRequestURI().getRawQuery());
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            reply(exchange, 200, "中文 a&b + 空格");
        });
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            var client = context.getBean(RestClient.Builder.class).baseUrl(base).build();
            String value = "中文 a&b + 空格";
            assertThat(client.get().uri(builder -> builder.path("/echo").queryParam("q", "{q}").build(value)).retrieve().body(String.class)).isEqualTo(value);
            assertThat(query.get()).isEqualTo("q=%E4%B8%AD%E6%96%87%20a%26b%20%2B%20%E7%A9%BA%E6%A0%BC");
            var form = new LinkedMultiValueMap<String, String>(); form.add("q", value); form.add("empty", "");
            assertThat(client.post().uri("/echo").contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(String.class)).isEqualTo(value);
            assertThat(query.get()).isNull();
            assertThat(body.get()).isEqualTo("q=%E4%B8%AD%E6%96%87+a%26b+%2B+%E7%A9%BA%E6%A0%BC&empty=");
            assertThat(contentType.get()).startsWith("application/x-www-form-urlencoded");
            client.post().uri("/echo").contentType(MediaType.TEXT_PLAIN).body(value).retrieve().toBodilessEntity();
            assertThat(body.get()).isEqualTo(value);
            assertThat(contentType.get()).containsIgnoringCase("charset=UTF-8");
        });
    }

    @Test void jsonEmptyCollectionsAndSuccessfulEmptyResponseRemainDistinct() {
        server.createContext("/json", exchange -> {
            byte[] bytes = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.createContext("/empty", exchange -> { exchange.sendResponseHeaders(204, -1); exchange.close(); });
        runner.run(context -> {
            var client = context.getBean(RestClient.Builder.class).baseUrl(base).build();
            var value = new Payload("中文", List.of());
            assertThat(client.post().uri("/json").body(value).retrieve().body(Payload.class)).isEqualTo(value);
            assertThat(client.get().uri("/empty").retrieve().toEntity(String.class).getStatusCode().value()).isEqualTo(204);
            assertThat(client.get().uri("/empty").retrieve().body(String.class)).isNull();
        });
    }

    @Test void redirectsAndFailuresDoNotRetryOrLeakResponseData() {
        AtomicInteger failures = new AtomicInteger(), redirected = new AtomicInteger();
        server.createContext("/failure", exchange -> { failures.incrementAndGet(); reply(exchange, 503, "secret-response-token"); });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().set("Location", base + "/destination"); reply(exchange, 307, "redirect-secret");
        });
        server.createContext("/destination", exchange -> { redirected.incrementAndGet(); reply(exchange, 200, "unexpected"); });
        runner.run(context -> {
            var client = context.getBean(RestClient.Builder.class).baseUrl(base).build();
            assertThatThrownBy(() -> client.post().uri("/failure?token=query-secret").body("private-body").retrieve().body(String.class))
                    .isInstanceOf(OutboundHttpException.class).hasMessage("外部服务返回异常状态（503）").hasNoCause();
            assertThat(failures).hasValue(1);
            assertThatThrownBy(() -> client.get().uri("/redirect").exchange((request, response) -> response.getStatusCode().value()))
                    .isInstanceOf(OutboundHttpException.class).hasMessageContaining("307");
            assertThat(redirected).hasValue(0);
            var advice = new GlobalExceptionHandler();
            assertThat(advice.handleOutbound(new OutboundHttpException("外部服务连接失败", false)).getStatusCode().value()).isEqualTo(502);
            assertThat(advice.handleUnexpected(new IllegalStateException(new OutboundHttpException("外部服务请求超时", true))).getStatusCode().value()).isEqualTo(504);
        });
    }

    @Test void advertisedAndChunkedBodiesCannotExceedLimitIncludingStreamSkip() {
        server.createContext("/fixed", exchange -> reply(exchange, 200, "x".repeat(65)));
        server.createContext("/chunked", exchange -> {
            exchange.sendResponseHeaders(200, 0); exchange.getResponseBody().write(new byte[65]); exchange.close();
        });
        server.createContext("/boundary", exchange -> reply(exchange, 200, "x".repeat(64)));
        server.createContext("/compressed", exchange -> {
            var bytes = new java.io.ByteArrayOutputStream();
            try (var gzip = new java.util.zip.GZIPOutputStream(bytes)) { gzip.write(new byte[512]); }
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(200, bytes.size()); exchange.getResponseBody().write(bytes.toByteArray()); exchange.close();
        });
        runner.withPropertyValues("cyys.http.max-response-bytes=64").run(context -> {
            var client = context.getBean(RestClient.Builder.class).baseUrl(base).build();
            for (String path : List.of("/fixed", "/chunked", "/compressed")) {
                assertThatThrownBy(() -> client.get().uri(path).retrieve().body(String.class))
                        .isInstanceOf(OutboundHttpException.class).hasMessageContaining("超过允许大小");
            }
            assertThatThrownBy(() -> client.get().uri("/chunked").exchange((request, response) -> response.getBody().skip(65)))
                    .isInstanceOf(OutboundHttpException.class);
            assertThat(client.get().uri("/boundary").retrieve().body(String.class)).hasSize(64);
        });
    }

    @Test void timeoutBoundsHeadersAndBodyWithoutReplayingPost() {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/slow", exchange -> {
            calls.incrementAndGet();
            try { Thread.sleep(1000); reply(exchange, 200, "late"); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); exchange.close(); }
        });
        server.createContext("/slow-body", exchange -> {
            calls.incrementAndGet(); exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write('a'); exchange.getResponseBody().flush();
            try { Thread.sleep(1000); exchange.getResponseBody().write('b'); exchange.close(); }
            catch (InterruptedException error) { Thread.currentThread().interrupt(); exchange.close(); }
        });
        runner.withPropertyValues("spring.http.clients.read-timeout=200ms").run(context -> {
            var client = context.getBean(RestClient.Builder.class).baseUrl(base).build();
            for (String path : List.of("/slow", "/slow-body")) {
                assertThatThrownBy(() -> client.post().uri(path).body("side-effect").retrieve().body(String.class))
                        .isInstanceOf(OutboundHttpException.class).hasMessage("外部服务请求超时");
            }
            assertThat(calls).hasValue(2);
        });
    }

    @Test void buildersCookiesAndInboundCredentialsAreNotShared() {
        server.createContext("/cookie", exchange -> {
            exchange.getResponseHeaders().set("Set-Cookie", "external-secret=value; Path=/"); reply(exchange, 200, "ok");
        });
        server.createContext("/headers", exchange -> reply(exchange, 200, String.valueOf(exchange.getRequestHeaders().getFirst("Cookie"))
                + "/" + exchange.getRequestHeaders().getFirst("Authorization") + "/" + exchange.getRequestHeaders().getFirst("X-Cyys-Scope-Id")));
        var inbound = new MockHttpServletRequest(); inbound.addHeader("Authorization", "Bearer incoming-secret"); inbound.addHeader("X-Cyys-Scope-Id", "scope-a");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(inbound));
        runner.run(context -> {
            var configured = context.getBean(RestClient.Builder.class).baseUrl(base).defaultHeader("Authorization", "integration-credential").build();
            configured.get().uri("/cookie").retrieve().toBodilessEntity();
            assertThat(configured.get().uri("/headers").retrieve().body(String.class)).isEqualTo("null/integration-credential/null");
            var independent = context.getBean(RestClient.Builder.class).baseUrl(base).build();
            assertThat(independent.get().uri("/headers").retrieve().body(String.class)).isEqualTo("null/null/null");
        });
    }

    @Test void connectDeadlineAlsoBoundsAnUnresponsiveTlsHandshake() throws Exception {
        try (var socket = new java.net.ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            var accepted = new CountDownLatch(1);
            Future<?> stalled = workers.submit(() -> {
                try (var connection = socket.accept()) { accepted.countDown(); Thread.sleep(2000); }
                catch (IOException ignored) { /* 测试结束关闭监听。 */ }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            });
            try {
                runner.withPropertyValues("spring.http.clients.connect-timeout=100ms").run(context -> {
                    var client = context.getBean(RestClient.Builder.class).build();
                    assertThatThrownBy(() -> client.get().uri("https://localhost:" + socket.getLocalPort()).retrieve().body(String.class))
                            .isInstanceOf(OutboundHttpException.class).hasMessage("外部服务请求超时");
                    assertThat(accepted.await(1, TimeUnit.SECONDS)).isTrue();
                });
            } finally { stalled.cancel(true); }
        }
    }

    @Test void invalidLimitsAndInfiniteTimeoutsFailStartup() {
        for (String value : List.of("spring.http.clients.connect-timeout=0", "spring.http.clients.read-timeout=-1s",
                "spring.http.clients.read-timeout=1ns", "spring.http.clients.read-timeout=3m", "spring.http.clients.redirects=follow",
                "spring.http.clients.cookie-handling=enable", "cyys.http.max-response-bytes=0", "cyys.http.max-response-bytes=16777217")) {
            runner.withPropertyValues(value).run(context -> assertThat(context).hasFailed());
        }
    }

    @Test void defaultTrustRejectsSelfSignedAndConfiguredCaStillChecksHostname() throws Exception {
        Path store = root.resolve("loopback.p12"); String password = "test-only-password";
        String keytool = System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool";
        Process certificate = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", keytool).toString(),
                "-genkeypair", "-alias", "loopback", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-dname", "CN=localhost", "-ext", "SAN=dns:localhost", "-storetype", "PKCS12", "-keystore", store.toString(),
                "-storepass", password, "-noprompt").redirectErrorStream(true).redirectOutput(root.resolve("keytool.log").toFile()).start();
        try { assertThat(certificate.waitFor(15, TimeUnit.SECONDS)).isTrue(); assertThat(certificate.exitValue()).isZero(); }
        finally { if (certificate.isAlive()) certificate.destroyForcibly(); }
        var keys = KeyStore.getInstance("PKCS12");
        try (var input = Files.newInputStream(store)) { keys.load(input, password.toCharArray()); }
        var managers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); managers.init(keys, password.toCharArray());
        var ssl = SSLContext.getInstance("TLS"); ssl.init(managers.getKeyManagers(), null, null);
        var secure = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        secure.setExecutor(workers); secure.setHttpsConfigurator(new HttpsConfigurator(ssl));
        secure.createContext("/", exchange -> reply(exchange, 200, "trusted")); secure.start();
        int port = secure.getAddress().getPort();
        try {
            runner.run(context -> assertThatThrownBy(() -> context.getBean(RestClient.Builder.class).build()
                    .get().uri("https://localhost:" + port).retrieve().body(String.class)).isInstanceOf(OutboundHttpException.class));
            runner.withPropertyValues("spring.ssl.bundle.jks.loop.truststore.location=" + store.toUri(),
                    "spring.ssl.bundle.jks.loop.truststore.password=" + password).run(context -> {
                var client = context.getBean(RestClient.Builder.class).apply(context.getBean(RestClientSsl.class).fromBundle("loop")).build();
                assertThat(client.get().uri("https://localhost:" + port).retrieve().body(String.class)).isEqualTo("trusted");
                assertThatThrownBy(() -> client.get().uri("https://127.0.0.1:" + port).retrieve().body(String.class))
                        .isInstanceOf(OutboundHttpException.class).hasMessage("外部服务连接失败");
            });
        } finally { secure.stop(0); }
    }

    public record Payload(String name, List<String> items) { }
    private static void reply(HttpExchange exchange, int status, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (!exchange.getResponseHeaders().containsKey("Content-Type")) exchange.getResponseHeaders().set("Content-Type", "text/plain");
        exchange.sendResponseHeaders(status, bytes.length); exchange.getResponseBody().write(bytes); exchange.close();
    }
}
