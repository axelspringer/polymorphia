package de.bild.codec;

import com.mongodb.MongoClient;
import de.bild.codec.model.AnInterface;
import de.bild.codec.model.Pojo;
import de.bild.codec.model.de.bild.codec.model.RedundantPackageNameClass;
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

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TypesModelPackageTest.class)
@ComponentScan(basePackages = "de.bild")
public class TypesModelPackageTest {

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder().register("de.bild.codec.model").build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    @Test
    public void testRegisteredModelClasses() {
        Assert.assertNotNull(codecRegistry.get(Pojo.class));
        Assert.assertNotNull(codecRegistry.get(AnInterface.class));
        Assert.assertNotNull(codecRegistry.get(RedundantPackageNameClass.class));
    }
}
