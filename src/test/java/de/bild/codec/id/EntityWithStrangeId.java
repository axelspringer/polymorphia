package de.bild.codec.id;

import de.bild.codec.IdGenerator;
import de.bild.codec.annotations.Id;
import org.bson.BsonTimestamp;
import org.bson.types.ObjectId;

import java.util.Random;


public class EntityWithStrangeId {
    @Id(collectible = true, value = CombinedIdGenerator.class)
    CombinedId id;

    String property;


    static class CombinedId {
        final static Random randomGenerator = new Random();
        String idPart1;
        ObjectId idPart2;
        BsonTimestamp idPart3;

        public static CombinedId random() {
            CombinedId combinedId = new CombinedId();
            combinedId.idPart1 = "Hi " + randomGenerator.nextInt();
            combinedId.idPart2 = new ObjectId();
            combinedId.idPart3 = new BsonTimestamp(randomGenerator.nextInt(), 0);
            return combinedId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CombinedId that = (CombinedId) o;

            if (idPart1 != null ? !idPart1.equals(that.idPart1) : that.idPart1 != null) return false;
            if (idPart2 != null ? !idPart2.equals(that.idPart2) : that.idPart2 != null) return false;
            return idPart3 != null ? idPart3.equals(that.idPart3) : that.idPart3 == null;
        }

        @Override
        public int hashCode() {
            int result = idPart1 != null ? idPart1.hashCode() : 0;
            result = 31 * result + (idPart2 != null ? idPart2.hashCode() : 0);
            result = 31 * result + (idPart3 != null ? idPart3.hashCode() : 0);
            return result;
        }
    }

    public static class CombinedIdGenerator implements IdGenerator<CombinedId> {
        @Override
        public CombinedId generate() {
            return CombinedId.random();
        }
    }
}
