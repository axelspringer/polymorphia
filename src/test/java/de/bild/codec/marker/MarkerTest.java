package de.bild.codec.marker;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import org.bson.BsonReader;
import org.bson.BsonReaderMark;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.types.ObjectId;

/**
 * TODO: needs fix for https://jira.mongodb.org/browse/JAVA-2754
 */
public class MarkerTest {

    CodecRegistry codecRegistry = CodecRegistries.fromCodecs(new PojoCodec());

    static class Pojo {
        ObjectId id;
        String stringField;

        public String getStringField() {
            return stringField;
        }

        public void setStringField(String stringField) {
            this.stringField = stringField;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Pojo{");
            sb.append("id=").append(id);
            sb.append(", stringField='").append(stringField).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }


    /**
     * Resilient codec that skips entities, in case of exceptions during decoding
     */
    static class PojoCodec implements Codec<Pojo> {
        @Override
        public Pojo decode(BsonReader reader, DecoderContext decoderContext) {
            Pojo pojo = null;
            BsonReaderMark mark = null;

            try {
                mark = reader.getMark();
                reader.readStartDocument();
                pojo = new Pojo();
                // reading fields and set in Pojo
                pojo.setStringField(readFields(reader));
                reader.readEndDocument();
                return pojo;

            } catch (Exception e) {
                System.out.println(e);
                mark.reset();
                reader.skipValue();
            }
            return pojo;
        }

        private String readFields(BsonReader reader) {
            // the following mark, even if never needed, causes the above mark.reset() to reset the reader to the wrong position
            BsonReaderMark markInReadingFields = reader.getMark();

            reader.readName();
            reader.readObjectId();
            //simulate random decoding exception....
            if (true) throw new RuntimeException("Some random exception occurred");
            reader.readName();
            return reader.readString();
        }

        @Override
        public void encode(BsonWriter writer, Pojo pojo, EncoderContext encoderContext) {
            writer.writeStartDocument();
            writer.writeName("stringField");
            writer.writeString(pojo.getStringField());
            writer.writeEndDocument();
        }

        @Override
        public Class<Pojo> getEncoderClass() {
            return Pojo.class;
        }
    }

    public void testMultipleMarker() {

        MongoClientOptions mongoClientOptions = new MongoClientOptions.Builder().codecRegistry(codecRegistry).build();
        MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017), mongoClientOptions);

        Pojo pojo = new Pojo();
        pojo.setStringField("test");
        System.out.println("Pojo = " + pojo);

        MongoCollection<Pojo> pojoMongoCollection = mongoClient.getDatabase("test").getCollection("pojos").withDocumentClass(Pojo.class);
        pojoMongoCollection.insertOne(pojo);

        Pojo readPojo = pojoMongoCollection.find().first();
        System.out.println("Decoded pojo = " + readPojo);
    }

    public static void main(String[] args) {
        new MarkerTest().testMultipleMarker();
    }
}