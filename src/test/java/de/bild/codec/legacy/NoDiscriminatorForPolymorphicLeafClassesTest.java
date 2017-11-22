package de.bild.codec.legacy;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import de.bild.codec.*;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.bson.json.JsonReader;
import org.bson.types.ObjectId;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Assert;
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

import java.util.*;

/**
 * Polymorphia prior to version 1.3.0 did not write proper dsciriminators for classes that where subclassing interfaces
 * Decoding those should be possible but the decoder expects a discriminator.
 * the new behaviour is as follows:
 * 1) Discriminators will be written to the database for new entities
 * 2) If no discriminator is found in the DB, but the destination class is unambiguous,
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = NoDiscriminatorForPolymorphicLeafClassesTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class NoDiscriminatorForPolymorphicLeafClassesTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoDiscriminatorForPolymorphicLeafClassesTest.class);

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(NoDiscriminatorForPolymorphicLeafClassesTest.class)
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    @Autowired
    private MongoClient mongoClient;

    interface IdVersionTuple<T> {
        T getId();
        Integer getVersion();
        default Bson toFilter() {
            return null;
        }
    }

    public abstract static class BaseResult<T> implements IdVersionTuple<T> {
        Integer version;
        Set<ObjectId> references;
        private BaseResult() {
        }
        @Override
        public Integer getVersion() {
            return version;
        }
    }

    public static abstract class AbstractDocumentResult extends BaseResult<ObjectId> {
        ObjectId cmsId;

        @Override
        public ObjectId getId() {
            return cmsId;
        }
    }

    public static class DocumentResult extends AbstractDocumentResult {

    }



        public static class UrlResult extends BaseResult<String> {
        String url;

        private UrlResult() {
            // empty constructor for decoder
        }

        @Override
        public String getId() {
            return url;
        }
    }

    static class DocumentContainer {
        private Set<AbstractDocumentResult> documents = new LinkedHashSet<>();
        private Set<UrlResult> urls = new LinkedHashSet<>();

        private DocumentContainer() {
        }

        public DocumentContainer(Set<AbstractDocumentResult> documents, Set<UrlResult> urls) {
            this.documents = documents;
            this.urls = urls;
        }
    }


    private static final String LEGACY_BSON =
            "{\n" +
                    "    \"documents\" : [{\n" +
                    "     \"_t\" :  \"SomeIncorrectType\"" +
                    "        \"cmsId\" : {\n" +
                    "          \"$oid\" : \"57beb711ed91159e1e3857e4\"\n" +
                    "        },\n" +
                    "        \"version\" : 3\n" +
                    "      }, {\n" +
                    "        \"cmsId\" : {\n" +
                    "          \"$oid\" : \"57beb711ed91159e1e3857e5\"\n" +
                    "        },\n" +
                    "        \"version\" : 3\n" +
                    "      }],\n" +
                    "    \"urls\" : [{\n" +
                    "        \"url\" : \"/test/url1/\",\n" +
                    "        \"version\" : 5\n" +
                    "      }]\n" +
                    "}";

    @Test
    public void reorderingDocumentTest() {
        Codec<DocumentContainer> documentCollectionCodec = mongoClient.getMongoClientOptions().getCodecRegistry().get(DocumentContainer.class);

        MongoCollection<DocumentContainer> documentMongoCollection = mongoClient.getDatabase("test").getCollection("documents").withDocumentClass(DocumentContainer.class);


        JsonReader reader = new JsonReader(LEGACY_BSON);
        DocumentContainer documentContainer = documentCollectionCodec.decode(reader, DecoderContext.builder().build());

        Assert.assertNotNull(documentContainer);
        Assert.assertEquals(documentContainer.documents.size(), 2);
        for (AbstractDocumentResult document : documentContainer.documents) {
            MatcherAssert.assertThat(document, IsInstanceOf.instanceOf(DocumentResult.class));
        }
        for (UrlResult url : documentContainer.urls) {
            MatcherAssert.assertThat(url, IsInstanceOf.instanceOf(UrlResult.class));
        }
    }
}
