package de.bild.codec;


import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.bild.codec.annotations.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
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

import java.io.StringWriter;
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
                            PojoCodecProvider.builder()
                                    .register(Object.class)
                                    .register(PolymorphicTest.class).build()
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


    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    static class Pojo {
        List<Shape> shapes;
        Shape aShape;
        FinalShapes aFinalShapes;
        Pojo innerPojo;
    }

    /************  DOMAIN MODEL END ************/


    @Test
    public void polymorphiaTest() {
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        MongoCollection<Pojo> collection = database.getCollection("entities").withDocumentClass(Pojo.class);

        Shape[] shapes = {FinalShapes.SMALL_TRIANGLE, new Circle(), new Square(), new Square(), new Circle()};

        Pojo pojo = Pojo.builder()
                .shapes(Arrays.asList(shapes))
                .aFinalShapes(FinalShapes.SMALL_TRIANGLE)
                .aShape(new Square())
                .innerPojo(Pojo.builder()
                        .shapes(Arrays.asList(shapes))
                        .aFinalShapes(FinalShapes.ONE_METER_CIRCLE)
                        .aShape(new Circle())
                        .build())
                .build();

        collection.insertOne(pojo);

        Pojo readPojo = collection.find().first();

        List<Shape> foundShapes = new ArrayList<>();
        for (Shape shape : readPojo.shapes) {
            LOGGER.info("Found shape {} of type {} in database", shape, shape.getClass().getSimpleName());
            foundShapes.add(shape);
        }

        MatcherAssert.assertThat(foundShapes, CoreMatchers.hasItems(shapes));
    }

}