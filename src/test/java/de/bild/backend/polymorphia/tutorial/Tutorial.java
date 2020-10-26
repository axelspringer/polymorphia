package de.bild.backend.polymorphia.tutorial;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import de.bild.backend.polymorphia.tutorial.model.AnEnum;
import de.bild.backend.polymorphia.tutorial.model.Pojo;
import de.bild.backend.polymorphia.tutorial.model.PolymorphicPojo;
import de.bild.backend.polymorphia.tutorial.model.SubPojo;
import de.bild.codec.PojoCodecProvider;
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
                        PojoCodecProvider.builder()
                                .register(Pojo.class.getPackage().getName())
                                .build()
                ),
                MongoClientSettings.getDefaultCodecRegistry());
    }


    public static void main(String[] args) {
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString("mongodb://localhost:27017"))
                .codecRegistry(getCodecRegistry())
                .build();
        MongoClient mongoClient = MongoClients.create(mongoClientSettings);

        MongoDatabase database = mongoClient.getDatabase("tutorial");
        MongoCollection<PolymorphicPojo> collection = database.getCollection("entities").withDocumentClass(PolymorphicPojo.class);

        // create some pojo
        Pojo pojo = new Pojo();
        pojo.setName("A nice name");
        pojo.setPojos(Arrays.asList(new SubPojo(42), new SubPojo(48)));
        pojo.setAnEnum(AnEnum.B);

        // insert into db
        collection.insertOne(pojo);

        // read from db
        PolymorphicPojo foundPojo = collection.find(Filters.eq("_id", pojo.getId())).first();

        // output
        LOGGER.debug("Found pojo {}", foundPojo);
    }
}
