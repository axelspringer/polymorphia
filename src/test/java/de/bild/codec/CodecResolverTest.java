package de.bild.codec;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import de.bild.codec.annotations.Id;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
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

import java.io.StringWriter;
import java.util.Date;
import java.util.Map;

/**
 * This test encodes a {@link Document} into the database by restructuring the fields
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = CodecResolverTest.class)
@EnableAutoConfiguration
@ComponentScan(basePackages = "de.bild")
public class CodecResolverTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CodecResolverTest.class);

    static class Config {
        @Bean()
        public static CodecRegistry getCodecRegistry() {
            return CodecRegistries.fromRegistries(
                    CodecRegistries.fromProviders(
                            new EnumCodecProvider(),
                            PojoCodecProvider.builder()
                                    .register(CodecResolverTest.class)
                                    .registerCodecResolver((CodecResolver) (type, typeCodecRegistry, codecConfiguration) -> {
                                        if (TypeUtils.isAssignable(type, Base.class)) {
                                            return new DocumentCodec((Class<? extends Base>) type, typeCodecRegistry, codecConfiguration);
                                        }
                                        return null;
                                    })
                                    .build()
                    ),
                    MongoClient.getDefaultCodecRegistry());
        }
    }

    @Autowired
    CodecRegistry codecRegistry;

    @Autowired
    private MongoClient mongoClient;

    static class MetaBase {

    }

    static class MetaData extends MetaBase {
        @Id(collectible = true)
        ObjectId id;
        long version;

        public ObjectId getId() {
            return id;
        }

        public long getVersion() {
            return version;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MetaData metaData = (MetaData) o;

            if (version != metaData.version) return false;
            return id != null ? id.equals(metaData.id) : metaData.id == null;
        }

        @Override
        public int hashCode() {
            int result = id != null ? id.hashCode() : 0;
            result = 31 * result + (int) (version ^ (version >>> 32));
            return result;
        }
    }

    interface Base {
        MetaData getMeta();
    }

    static class Document implements Base {
        MetaData meta = new MetaData();
        String stringProperty;
        Date datProperty;

        @Override
        public MetaData getMeta() {
            return meta;
        }

        public String getStringProperty() {
            return stringProperty;
        }

        public Date getDatProperty() {
            return datProperty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Document document = (Document) o;

            if (meta != null ? !meta.equals(document.meta) : document.meta != null) return false;
            if (stringProperty != null ? !stringProperty.equals(document.stringProperty) : document.stringProperty != null)
                return false;
            return datProperty != null ? datProperty.equals(document.datProperty) : document.datProperty == null;
        }

        @Override
        public int hashCode() {
            int result = meta != null ? meta.hashCode() : 0;
            result = 31 * result + (stringProperty != null ? stringProperty.hashCode() : 0);
            result = 31 * result + (datProperty != null ? datProperty.hashCode() : 0);
            return result;
        }
    }


    static class DocumentCodec<T extends Base> extends BasicReflectionCodec<T> {
        final ReflectionCodec<MetaData> documentMetaCodec;

        public DocumentCodec(Class<T> type, TypeCodecRegistry typeCodecRegistry, CodecConfiguration codecConfiguration) {
            super(type, typeCodecRegistry, codecConfiguration);
            MappedField mappedField = getMappedField("meta");
            Codec metaCodec = mappedField.getCodec();

            while (metaCodec instanceof DelegatingCodec) {
                DelegatingCodec<T> delegatingCodec = (DelegatingCodec<T>)metaCodec;
                metaCodec = delegatingCodec.getDelegate();
            }
            if (metaCodec instanceof PolymorphicReflectionCodec) {
                PolymorphicReflectionCodec<MetaData> polymorphicMetaCodec = (PolymorphicReflectionCodec<MetaData>) metaCodec;
                this.documentMetaCodec = (ReflectionCodec<MetaData>)polymorphicMetaCodec.getCodecForClass(mappedField.getField().getType());
            } else {
                this.documentMetaCodec = (ReflectionCodec<MetaData>) metaCodec;
            }
        }


        @Override
        public T decodeFields(BsonReader reader, DecoderContext decoderContext, T instance) {
            MetaData documentMeta = instance.getMeta();
            while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
                String fieldName = reader.readName();
                if ("data".equals(fieldName)) {
                    reader.readStartDocument();
                    super.decodeFields(reader, decoderContext, instance);
                    reader.readEndDocument();
                } else {
                    MappedField field = documentMetaCodec.getMappedField(fieldName);
                    if (field != null) {
                        field.decode(reader, documentMeta, decoderContext);
                    } else {
                        reader.skipValue();
                    }
                }
            }

            documentMetaCodec.postDecode(documentMeta);

            return instance;
        }

        @Override
        public void encodeFields(BsonWriter writer, T instance, EncoderContext encoderContext) {
            // first persist meta document
            documentMetaCodec.encodeFields(writer, instance.getMeta(), encoderContext);

            // only persist data property if and only if properties exist
            Map<String, MappedField> persistenceFields = getPersistenceFields();
            if (!persistenceFields.isEmpty()) {
                writer.writeName("data");
                writer.writeStartDocument();

                for (MappedField persistenceField : persistenceFields.values()) {
                    if (!"meta".equals(persistenceField.getMappedFieldName())) {
                        persistenceField.encode(writer, instance, encoderContext);
                    }
                }
                writer.writeEndDocument();
            }
        }

        @Override
        public boolean isCollectible() {
            return documentMetaCodec.isCollectible();
        }

        @Override
        public T generateIdIfAbsentFromDocument(T document) {
            documentMetaCodec.generateIdIfAbsentFromDocument(document.getMeta());
            return document;
        }

        @Override
        public boolean documentHasId(T document) {
            return documentMetaCodec.documentHasId(document.getMeta());
        }

        @Override
        public BsonValue getDocumentId(T document) {
            return documentMetaCodec.getDocumentId(document.getMeta());
        }
    }


    @Test
    public void reorderingDocumentTest() {
        Document document = new Document();
        document.stringProperty = "a nice string";
        document.datProperty = new Date();

        MongoCollection<Document> documentMongoCollection = mongoClient.getDatabase("test").getCollection("documents").withDocumentClass(Document.class);

        documentMongoCollection.insertOne(document);

        Document readDocument = documentMongoCollection.find(Filters.eq("_id", document.getMeta().getId())).first();

        Assert.assertEquals(document, readDocument);

        Codec<Document> documentCodec = codecRegistry.get(Document.class);
        StringWriter stringWriter = new StringWriter();
        JsonWriter writer = new JsonWriter(stringWriter, new JsonWriterSettings(true));
        documentCodec.encode(writer, document, EncoderContext.builder().build());
        LOGGER.info("The encoded json looks like: {}", stringWriter);

    }
}
