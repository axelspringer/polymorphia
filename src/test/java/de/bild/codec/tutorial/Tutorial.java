package de.bild.codec.tutorial;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.bild.codec.EnumCodecProvider;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.tutorial.model.Pojo;
import de.bild.codec.tutorial.model.PolymorphicPojo;
import de.bild.codec.tutorial.model.SubPojo;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Tutorial {
    private static final Logger LOGGER = LoggerFactory.getLogger(Tutorial.class);

    /**
     *
     * @return a CodecRegistry build from the model package and the DefaultCodecRegistry from the mongo driver
     */
    public static CodecRegistry getCodecRegistry() {
        return CodecRegistries.fromRegistries(
                CodecRegistries.fromProviders(
                        new EnumCodecProvider(),
                        PojoCodecProvider.builder()
                                .register("de.bild.codec.tutorial.model")
                                .build()
                ),
                MongoClient.getDefaultCodecRegistry());
    }


    public static void main(String[] args) {
        MongoClientOptions mongoClientOptions = new MongoClientOptions.Builder().codecRegistry(getCodecRegistry()).build();
        MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017), mongoClientOptions);

        MongoDatabase database = mongoClient.getDatabase("tutorial");
        MongoCollection<PolymorphicPojo> collection = database.getCollection("entities").withDocumentClass(PolymorphicPojo.class);

        // create some pojo
        Pojo pojo = new Pojo();
        pojo.setName("A nice name");
        pojo.setPojos(Arrays.asList(new SubPojo(42), new SubPojo(48)));

        // insert into db
        collection.insertOne(pojo);

        // read from db
        PolymorphicPojo foundPojo = collection.find(Filters.eq("_id", pojo.getId())).first();

        // output
        LOGGER.debug("Found pojo {}", foundPojo);
    }
}
