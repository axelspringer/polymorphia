package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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

}
