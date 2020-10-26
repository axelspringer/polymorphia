package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;

@SpringBootTest
@Import(MongoClientConfiguration.class) // activate test specific (CodecRegistry) mongoClient configuration
public abstract class AbstractTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTest.class);

    private static final MongoDBContainer MONGO_DB = new MongoDBContainer("mongo:4.1");

    @Configuration
    static class Config {
        @Bean
        @ConditionalOnMissingBean(CodecRegistry.class)
        public CodecRegistry getCodecRegistry() {
            return MongoClientSettings.getDefaultCodecRegistry();
        }
    }


    @Autowired
    public MongoClient mongoClient;

    @Autowired
    public CodecRegistry codecRegistry;

    static {
        //https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers
        MONGO_DB.start();
    }


    @DynamicPropertySource
    private static void mongoDBProperties(final DynamicPropertyRegistry registry) {
        LOGGER.info("spring.data.mongodb.uri = {}", MONGO_DB.getReplicaSetUrl());
        registry.add("spring.data.mongodb.uri", MONGO_DB::getReplicaSetUrl);
    }

    public MongoCollection<Document> getCollection() {
        return mongoClient.getDatabase("test").getCollection("documents_" + getClass().getSimpleName());
    }

    public <E> MongoCollection<E> getCollection(Class<E> clazz) {
        return getCollection().withDocumentClass(clazz);
    }


}