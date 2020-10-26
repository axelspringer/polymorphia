package de.bild.backend.polymorphia.classresolver;

import de.bild.backend.polymorphia.AbstractTest;
import de.bild.backend.polymorphia.classresolver.model.AlsoToBeResolved;
import de.bild.backend.polymorphia.classresolver.model.ToBeResolved;
import de.bild.codec.PojoCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

public class ClassResolverTest extends AbstractTest {

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromProviders(
                    PojoCodecProvider.builder()
                            .register(ToBeResolved.class.getPackage().getName())
                            .registerClassResolver(packageName -> {
                                if (ToBeResolved.class.getPackage().getName().equals(packageName)) {
                                    return Arrays.asList(ToBeResolved.class, AlsoToBeResolved.class);
                                }
                                return null;
                            }).build());
        }
    }

    @Test
    public void specializedClassResolverTest() {
        Assert.assertNotNull(codecRegistry.get(ToBeResolved.class));
        Assert.assertNotNull(codecRegistry.get(AlsoToBeResolved.class));
    }
}