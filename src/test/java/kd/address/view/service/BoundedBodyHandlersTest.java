package kd.address.view.service;

import org.junit.jupiter.api.Test;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoundedBodyHandlersTest {
    @Test
    void rejectingHandlerFailsWhenBodyExceedsLimit() {
        var subscriber = subscriber(BoundedBodyHandlers.rejectOversize(4));
        AtomicBoolean canceled = subscribe(subscriber);
        subscriber.onNext(List.of(ByteBuffer.wrap(bytes("12345"))));

        assertThatThrownBy(() -> subscriber.getBody().toCompletableFuture().join())
                .isInstanceOf(CompletionException.class)
                .hasMessageContaining("HTTP 响应超过 4 字节上限");
        assertThat(canceled).isTrue();
    }

    @Test
    void truncatingHandlerCancelsAfterLimitAndReturnsPrefix() {
        var subscriber = subscriber(BoundedBodyHandlers.truncate(4));
        AtomicBoolean canceled = subscribe(subscriber);
        subscriber.onNext(List.of(ByteBuffer.wrap(bytes("12345"))));

        assertThat(new String(subscriber.getBody().toCompletableFuture().join(), StandardCharsets.UTF_8))
                .isEqualTo("1234");
        assertThat(canceled).isTrue();
    }

    private static HttpResponse.BodySubscriber<byte[]> subscriber(HttpResponse.BodyHandler<byte[]> handler) {
        return handler.apply(null);
    }

    private static AtomicBoolean subscribe(HttpResponse.BodySubscriber<byte[]> subscriber) {
        AtomicBoolean canceled = new AtomicBoolean();
        subscriber.onSubscribe(new Flow.Subscription() {
            @Override public void request(long count) {}
            @Override public void cancel() { canceled.set(true); }
        });
        return canceled;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }
}
