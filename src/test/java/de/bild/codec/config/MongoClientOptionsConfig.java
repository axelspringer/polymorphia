package de.bild.codec.config;

import com.mongodb.MongoClientOptions;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

/**
 * provides a MongoClientOptions
 */
@Configuration
public class MongoClientOptionsConfig {

    private final MongoClientOptions mongoClientOptions;

    @Autowired
    public MongoClientOptionsConfig(Optional<CodecRegistry> codecRegistry) {
        MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
        if (codecRegistry.isPresent()) {
            builder.codecRegistry(codecRegistry.get());
        }
        this.mongoClientOptions = builder.build();
    }

    @Bean
    public MongoClientOptions getMongoClientOptions() {
        return mongoClientOptions;
    }

}
