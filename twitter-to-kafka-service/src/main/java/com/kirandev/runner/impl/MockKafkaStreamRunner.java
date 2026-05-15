package com.kirandev.runner.impl;

import com.kirandev.configdata.TwitterToKafkaServiceConfigData;
import com.kirandev.exception.TwitterToKafkaServiceException;
import com.kirandev.listner.TwitterKafkaStatusListener;
import com.kirandev.runner.StreamRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.datafaker.Faker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "true")
public class MockKafkaStreamRunner implements StreamRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MockKafkaStreamRunner.class);

    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    private final Faker faker = new Faker();
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String[] keywords = new String[]{
            "Lorem",
            "ipsum",
            "dolor",
            "sit",
            "amet",
            "consectetuer",
            "adipiscing",
            "elit",
            "Maecenas",
            "porttitor",
            "congue",
            "massa",
            "Fusce",
            "posuere",
            "magna",
            "sed",
            "pulvinar",
            "ultricies",
            "purus",
            "lectus",
            "malesuada",
            "libero"
    };

    public MockKafkaStreamRunner(TwitterToKafkaServiceConfigData configData,
                                 TwitterKafkaStatusListener statusListener) {
        this.twitterToKafkaServiceConfigData = configData;
        this.twitterKafkaStatusListener = statusListener;
    }

    @Override
    public void start() throws TwitterException {
        List<String> keywordsList = twitterToKafkaServiceConfigData.getTwitterKeywords();
        String[] keywords = keywordsList.toArray(new String[0]);
        long sleepTimeMs = twitterToKafkaServiceConfigData.getMockSleepMs();

        LOG.info("Starting mock filtering twitter streams with Datafaker for keywords {}", Arrays.toString(keywords));

        try {
            while (true) {
                String formattedTweetAsRawJson = generateFakerTweetJson();
                Status status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
                twitterKafkaStatusListener.onStatus(status);

                Thread.sleep(sleepTimeMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TwitterToKafkaServiceException("Mock stream runner interrupted while sleeping!", e);
        } catch (Exception e) {
            throw new TwitterToKafkaServiceException("Error executing datafaker mock stream generation pipeline!", e);
        }
    }

    private String generateFakerTweetJson() throws Exception {
        ObjectNode tweetJson = mapper.createObjectNode();
        long tweetId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);
        long userId = ThreadLocalRandom.current().nextLong(Long.MAX_VALUE);

        // Twitter4J maps v1.1 status layouts using string fields and legacy Date objects
        tweetJson.put("id", tweetId);
        tweetJson.put("id_str", String.valueOf(tweetId));
        tweetJson.put("created_at", new Date().toString());

        // Datafaker automatically mixes the tracking target keywords into the generated block
        tweetJson.put("text", faker.twitter().text(keywords, 140,16));

        ObjectNode userJson = mapper.createObjectNode();
        userJson.put("id", userId);
        userJson.put("id_str", String.valueOf(userId));
        userJson.put("name", faker.name().fullName());
        userJson.put("screen_name", faker.name().username());
        tweetJson.set("user", userJson);

        return mapper.writeValueAsString(tweetJson);
    }
}