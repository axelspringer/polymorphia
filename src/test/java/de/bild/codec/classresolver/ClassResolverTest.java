package de.bild.codec.classresolver;

import de.bild.codec.PojoCodecProvider;
import de.bild.codec.classresolver.model.AlsoToBeResolved;
import de.bild.codec.classresolver.model.ToBeResolved;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ClassResolverTest.class)
@ComponentScan(basePackages = "de.bild")
public class ClassResolverTest {

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromProviders(
                           PojoCodecProvider.builder()
                                   .register("de.bild.codec.classresolver.model")
                                   .registerClassResolver(packageName -> {
                                       if (ToBeResolved.class.getPackage().getName().equals(packageName)) {
                                           return Arrays.asList(ToBeResolved.class, AlsoToBeResolved.class);
                                       }
                                       return null;
                                   }).build());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    @Test
    public void specializedClassResolverTest() {
        Assert.assertNotNull(codecRegistry.get(ToBeResolved.class));
        Assert.assertNotNull(codecRegistry.get(AlsoToBeResolved.class));
    }
}