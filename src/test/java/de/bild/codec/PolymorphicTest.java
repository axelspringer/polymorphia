package de.bild.codec;


import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.bild.codec.annotations.Id;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PolymorphicTest.class)
@ComponentScan(basePackages = "de.bild")
@EnableAutoConfiguration
public class PolymorphicTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphicTest.class);

    protected static final String DB_NAME = "test";

    @Autowired
    private MongoClient mongoClient;

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder().register(PolymorphicTest.class).build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    /************  DOMAIN MODEL START ************/
    public interface Shape {
    }

    public static abstract class IdentityShape implements Shape {
        @Id(collectible = true)
        ObjectId id;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            IdentityShape that = (IdentityShape) o;

            return id != null ? id.equals(that.id) : that.id == null;
        }

        @Override
        public int hashCode() {
            return id != null ? id.hashCode() : 0;
        }
    }


    public static class Circle extends IdentityShape {
        String name;
    }

    public static class Square extends IdentityShape {
        int foo;
    }

    enum FinalShapes implements Shape {
        ONE_METER_CIRCLE,
        SMALL_TRIANGLE;
    }

    /************  DOMAIN MODEL END ************/


    @Test
    public void polymorphiaTest() {
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        MongoCollection<Shape> collection = database.getCollection("entities").withDocumentClass(Shape.class);


        Shape[] shapes = {new Circle(), new Square(), new Square(), FinalShapes.ONE_METER_CIRCLE, new Circle(), FinalShapes.SMALL_TRIANGLE};
        collection.insertMany(Arrays.asList(shapes));

        FindIterable<Shape> shapesFoundIterable = collection.find();
        List<Shape> foundShapes = new ArrayList<>();
        for (Shape shape : shapesFoundIterable) {
            LOGGER.info("Found shape {} of type {} in database", shape, shape.getClass().getSimpleName());
            foundShapes.add(shape);
        }

        MatcherAssert.assertThat(foundShapes, CoreMatchers.hasItems(shapes));
    }

}