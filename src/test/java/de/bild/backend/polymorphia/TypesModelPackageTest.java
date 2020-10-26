package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.model.AnInterface;
import de.bild.codec.model.Pojo;
import de.bild.codec.model.de.bild.codec.model.RedundantPackageNameClass;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

public class TypesModelPackageTest extends AbstractTest {

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder().register("de.bild.codec.model").build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }

    @Test
    public void testRegisteredModelClasses() {
        Assert.assertNotNull(codecRegistry.get(Pojo.class));
        Assert.assertNotNull(codecRegistry.get(AnInterface.class));
        Assert.assertNotNull(codecRegistry.get(RedundantPackageNameClass.class));
    }
}
