package de.bild.backend.polymorphia;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoCollection;
import de.bild.codec.PojoCodecProvider;
import de.bild.codec.SpecialFieldsMap;
import de.bild.codec.annotations.FieldMapping;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.hamcrest.core.IsInstanceOf;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


public class SpecialFieldsMapCodecTest extends AbstractTest{

    @Configuration
    static class Config {
        @Bean
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            PojoCodecProvider.builder().register(SpecialFieldsMapCodecTest.class).build()
                    ),
                    MongoClientSettings.getDefaultCodecRegistry());
        }
    }


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
        MongoCollection<RichTextData> mongoCollection = getCollection(RichTextData.class);

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