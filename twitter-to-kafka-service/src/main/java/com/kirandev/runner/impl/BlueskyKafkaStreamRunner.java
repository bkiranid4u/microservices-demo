//package com.kirandev.runner.impl;
//
//import com.kirandev.configdata.TwitterToKafkaServiceConfigData;
//import com.kirandev.listner.TwitterKafkaStatusListener; // Retain or rename as needed
//import com.kirandev.runner.StreamRunner;
//import jakarta.annotation.PreDestroy;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
//import org.springframework.stereotype.Component;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.WebSocket;
//import java.util.concurrent.CompletionStage;
//
//@Component
//@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "false", matchIfMissing = true)
//public class BlueskyKafkaStreamRunner implements StreamRunner {
//
//    private static final Logger logger = LoggerFactory.getLogger(BlueskyKafkaStreamRunner.class);
//
//    // Points to production Jetstream instance isolating post collections
//    private static final String BLUESKY_JETSTREAM_URL =
//            "wss://jetstream1.us-east.bsky.network/subscribe?wantedCollections=app.bsky.feed.post";
//
//    private final TwitterToKafkaServiceConfigData configData;
//    private final TwitterKafkaStatusListener statusListener;
//    private WebSocket webSocket;
//
//    public BlueskyKafkaStreamRunner(TwitterToKafkaServiceConfigData configData,
//                                    TwitterKafkaStatusListener statusListener) {
//        this.configData = configData;
//        this.statusListener = statusListener;
//    }
//
//    @Override
//    public void start() {
//        logger.info("Initializing Bluesky Jetstream WebSocket connection pipeline...");
//        HttpClient httpClient = HttpClient.newHttpClient();
//
//        this.webSocket = httpClient.newWebSocketBuilder()
//                .buildAsync(URI.create(BLUESKY_JETSTREAM_URL), new JetstreamWebSocketListener())
//                .join();
//    }
//
//    @PreDestroy
//    public void shutdown() {
//        if (webSocket != null) {
//            logger.info("Closing active Bluesky stream WebSocket connection!");
//            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Application Context Shutdown");
//        }
//    }
//
//    /**
//     * Inner WebSocket Client implementation translating incoming events
//     * to your existing target consumer infrastructure.
//     */
//    private class JetstreamWebSocketListener implements WebSocket.Listener {
//        private final StringBuilder frameBuffer = new StringBuilder();
//
//        @Override
//        public void onOpen(WebSocket webSocket) {
//            logger.info("Successfully established connection to Bluesky pipeline.");
//            webSocket.request(1); // Signal willingness to receive first event frame
//        }
//
//        @Override
//        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
//            frameBuffer.append(data);
//
//            if (last) {
//                String completeJsonPayload = frameBuffer.toString();
//                frameBuffer.setLength(0); // Wipe out active buffer frame
//
//                try {
//                    // Send the raw JSON string down to your StatusListener.
//                    // Adjust your listener to parse JSON data or evaluate text keywords.
//                    statusListener.onStatus(completeJsonPayload);
//                } catch (Exception e) {
//                    logger.error("Exception handled during message transformation pipeline processing", e);
//                }
//            }
//
//            webSocket.request(1); // Demand next text package from channel stream
//            return null;
//        }
//
//        @Override
//        public void onError(WebSocket webSocket, Throwable error) {
//            logger.error("Critical error encountered on Bluesky WebSocket channel stream", error);
//        }
//
//        @Override
//        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
//            logger.warn("Bluesky stream socket execution terminated by host server. Code: {}, Reason: {}", statusCode, reason);
//            return null;
//        }
//    }
//}
