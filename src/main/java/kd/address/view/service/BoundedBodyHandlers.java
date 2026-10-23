package kd.address.view.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

/** Reads HTTP response bodies with a hard memory bound. */
final class BoundedBodyHandlers {
    private BoundedBodyHandlers() {}

    static HttpResponse.BodyHandler<byte[]> rejectOversize(int maxBytes) {
        return responseInfo -> new BoundedSubscriber(maxBytes, false);
    }

    static HttpResponse.BodyHandler<byte[]> truncate(int maxBytes) {
        return responseInfo -> new BoundedSubscriber(maxBytes, true);
    }

    private static final class BoundedSubscriber implements HttpResponse.BodySubscriber<byte[]> {
        private final int maxBytes;
        private final boolean truncate;
        private final ByteArrayOutputStream output;
        private final CompletableFuture<byte[]> body = new CompletableFuture<>();
        private Flow.Subscription subscription;
        private int received;

        private BoundedSubscriber(int maxBytes, boolean truncate) {
            if (maxBytes < 1) throw new IllegalArgumentException("maxBytes must be positive");
            this.maxBytes = maxBytes;
            this.truncate = truncate;
            this.output = new ByteArrayOutputStream(Math.min(maxBytes, 8192));
        }

        @Override
        public java.util.concurrent.CompletionStage<byte[]> getBody() {
            return body;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            try {
                for (ByteBuffer buffer : buffers) {
                    int available = buffer.remaining();
                    int remaining = maxBytes - received;
                    if (available > remaining && !truncate) {
                        fail(new IOException("HTTP 响应超过 " + maxBytes + " 字节上限"));
                        return;
                    }
                    int count = Math.min(available, remaining);
                    if (count > 0) {
                        byte[] chunk = new byte[count];
                        buffer.get(chunk);
                        output.write(chunk, 0, count);
                        received += count;
                    }
                    if (available > count || (truncate && received == maxBytes)) {
                        finish();
                        return;
                    }
                }
                subscription.request(1);
            } catch (RuntimeException exception) {
                fail(exception);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            body.completeExceptionally(throwable);
        }

        @Override
        public void onComplete() {
            body.complete(output.toByteArray());
        }

        private void finish() {
            subscription.cancel();
            body.complete(output.toByteArray());
        }

        private void fail(Throwable throwable) {
            subscription.cancel();
            body.completeExceptionally(throwable);
        }
    }
}
