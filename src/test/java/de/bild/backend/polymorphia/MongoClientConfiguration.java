package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.mongo.MongoClientFactory;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.stream.Collectors;

@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongoClientConfiguration implements MongoClientSettingsBuilderCustomizer {

    final CodecRegistry codecRegistry;

    public MongoClientConfiguration(CodecRegistry codecRegistry) {
        this.codecRegistry = codecRegistry;
    }

    @Override
    public void customize(MongoClientSettings.Builder clientSettingsBuilder) {
        clientSettingsBuilder.codecRegistry(codecRegistry);
    }

    @Bean
    public MongoClient mongo(MongoProperties properties, Environment environment,
                             ObjectProvider<MongoClientSettingsBuilderCustomizer> builderCustomizers,
                             ObjectProvider<MongoClientSettings> settings) {
        return new MongoClientFactory(properties, environment,
                builderCustomizers.orderedStream().collect(Collectors.toList()))
                .createMongoClient(settings.getIfAvailable());
    }
}
