package de.bild.codec;


import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.bild.codec.annotations.Discriminator;
import de.bild.codec.annotations.DiscriminatorKey;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
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

import java.util.Arrays;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = PolymorphicCollectionTest.class)
@ComponentScan(basePackages = "de.bild")
@EnableAutoConfiguration
public class PolymorphicCollectionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolymorphicCollectionTest.class);

    protected static final String DB_NAME = "test";
    public static final PojoCodecProvider POJO_CODEC_PROVIDER = PojoCodecProvider.builder()
            .register(PolymorphicCollectionTest.class).build();

    @Autowired
    private MongoClient mongoClient;

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            POJO_CODEC_PROVIDER
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

   @Autowired
   private CodecRegistry codecRegistry;

    /**
     * General interface specifies things that can possibly fund in this collection
     */
    public interface MyCollectionThing {}

    /************  DOMAIN MODEL START ************/
    public interface Shape extends MyCollectionThing {
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @DiscriminatorKey("_CircleDiscriminator")
    @Discriminator(value = "Circle", aliases = {"Ball", "Round"})
    @EqualsAndHashCode
    public static class Circle implements Shape {
        int radius;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Rectangle implements Shape {
        int x, y;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Square implements Shape {
        int x;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    static class Animal implements MyCollectionThing {
        String name;
    }

    @EqualsAndHashCode
    static class Mammal extends Animal{

    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    static class Cat extends Mammal {
        int age;
    }
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    static class Dog extends Mammal {
        String color;
    }

    @EqualsAndHashCode
    static class Fish extends Animal {
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    static class Tuna extends Fish {
        int size;
    }



    /************  DOMAIN MODEL END ************/


    @Test
    public void polymorphiaTest() {
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);

        MongoCollection<MyCollectionThing> collection = database.getCollection("documents").withDocumentClass(MyCollectionThing.class);

        Dog brownDog = new Dog("brown");
        Tuna oldTuna = new Tuna(123);
        Circle smallCircle = new Circle(2);
        Cat generalCat = new Cat(9);
        Rectangle rectangle = new Rectangle(2, 5);
        Circle mediumCircle = new Circle(5);
        Square square = new Square(4);


        // insert all items into collection of type MongoCollection<MyCollectionThing>
        collection.insertMany(Arrays.asList(brownDog, oldTuna, smallCircle, generalCat, rectangle, mediumCircle, square));


        Circle legacyCircle = new Circle(99);
        // insert some legacy things
        Document legacyCircleDocument = new Document("_CircleDiscriminator", "Ball").append("radius", legacyCircle.radius);
        collection.withDocumentClass(Document.class).insertOne(legacyCircleDocument);


        MatcherAssert.assertThat(collection.find(getFilterForClass(Shape.class)), IsIterableContainingInAnyOrder.containsInAnyOrder(legacyCircle, rectangle, square, smallCircle, mediumCircle));
        MatcherAssert.assertThat(collection.find(getFilterForClass(Fish.class)), IsIterableContainingInAnyOrder.containsInAnyOrder(oldTuna));
        MatcherAssert.assertThat(collection.find(getFilterForClass(Animal.class)), IsIterableContainingInAnyOrder.containsInAnyOrder(oldTuna, generalCat, brownDog));

    }

    private Bson getFilterForClass(Class<?> clazz) {
        return POJO_CODEC_PROVIDER.getTypeFilter(clazz, codecRegistry);
        //alternative implementation, if PojoCodecProvider ist unknown...
//        Codec<?> codec = codecRegistry.get(clazz);
//        if (codec instanceof TypeCodec) {
//            return ((TypeCodec)codec).getTypeFilter();
//        }
    }
}