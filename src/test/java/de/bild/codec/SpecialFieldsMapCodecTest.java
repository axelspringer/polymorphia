package de.bild.codec;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import de.bild.codec.annotations.FieldMapping;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = SpecialFieldsMapCodecTest.class)
@ComponentScan(basePackages = "de.bild")
@EnableAutoConfiguration
public class SpecialFieldsMapCodecTest {

    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder().register(SpecialFieldsMapCodecTest.class).build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    private MongoClient mongoClient;

    static class RichTextData extends Document implements SpecialFieldsMap {

        @FieldMapping("entityMap")
        public Map<String, EntityMapEntry<Date>> getEntityMap() {
            return (Map) get("entityMap");
        }

        public static class EntityMapEntry<V> extends Document implements SpecialFieldsMap {
            @FieldMapping("date")
            public V getDate() {
                return (V) get("date");
            }
        }
    }


    @Test
    public void test() {
        MongoCollection<RichTextData> mongoCollection = mongoClient.getDatabase("test")
                .getCollection("documents")
                .withDocumentClass(RichTextData.class);

        RichTextData richText = new RichTextData();
        RichTextData.EntityMapEntry<Date> entityMapEntry = new RichTextData.EntityMapEntry<>();
        entityMapEntry.put("date", new Date());
        RichTextData.EntityMapEntry<Date> entityMapEntry1 = new RichTextData.EntityMapEntry<>();
        entityMapEntry1.put("date", new Date());
        entityMapEntry1.put("someDocumentProperty", new Document("propA", "String"));
        Map<String, RichTextData.EntityMapEntry<Date>> entityMap = new LinkedHashMap<>();
        entityMap.put("0", entityMapEntry);
        entityMap.put("1", entityMapEntry1);
        richText.put("entityMap", entityMap);
        richText.put("someOtherProperty", 11);
        richText.put("document", new Document("p1", new Date()));

        List<Document> value = Arrays.asList(
                new Document("key1", "value1").append("key2", "value2"),
                new Document("key1", "value1").append("key2", "value2"),
                new Document("key1", "value1").append("key2", "value2")
        );


        richText.put("blocks", value);

        mongoCollection.insertOne(richText);

        RichTextData richTextRead = mongoCollection.find().first();


        assertNotNull(richTextRead);
        assertThat(richTextRead.getEntityMap(), IsInstanceOf.instanceOf(Map.class));
        assertThat(richTextRead.getEntityMap().get("0"), IsInstanceOf.instanceOf(RichTextData.EntityMapEntry.class));
        assertEquals(richTextRead.get("blocks")
                , value);


    }
}