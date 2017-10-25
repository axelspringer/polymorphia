## POJO (Plain old java objects) Codec for mongo db

You can use this codec to encode plain old java objects into a mongo database and to decode those again.

## Main features 
 * Support for polymorphic class hierarchies
 * support for generic types
 * fine grained control over discriminator keys and values (needed to morph into the correct POJO while decoding)
 * fast
 * support for primitives and there object counterparts, sets, lists, maps (Map<String, T> as well as Map<KeyType,ValueType>) multi dimensional arrays, enums
 * allows for easy application [@Id(collectible = true)](src/main/java/de/bild/codec/annotations/Id.java) generation [@see CollectibleCodec](org.bson.codecs.CollectibleCodec)
   * example: [IdTest](src/test/java/de/bild/codec/id/IdTest.java)
 * provides fine grained control over restructuring data written to mongo (and reading from mongo) 
 * partial POJO codec [SpecialFieldsMapCodec](src/main/java/de/bild/codec/SpecialFieldsMapCodec.java)

## Usage
Note: There are plenty of code examples to be found in the tests.

Attention: In order to scan packages you need to provide either spring-core library or [org.reflections.reflections](https://github.com/ronmamo/reflections) library in the class path.
Alternatively you could register all your POJO classes one by one!

Instantiate the POJOCodecProvider and add it to the CodecRegistry.
Example: [PolymorphicReflectionCodecTest](src/test/java/de/bild/codec/PolymorphicReflectionCodecTest.java)

```java
public class BasePojo<T> {
    T someValue;
}

public class Pojo extends BasePojo<Integer> {
    @Id(collectible = true)
    ObjectId id;
    
    String aString;
    int anInt;
    List<Map<Long, Date>> complexList; // some complex things
    float[][][] floats; // multi dimensional float
}
```
   
```java
    PojoCodecProvider.builder()
        .register("de.bild.codec.model") //adds all static classes of a package to the model
        .register(Pojo.class) // adds a single class
        .build();

    CodecRegistry pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClient.getDefaultCodecRegistry());
    
    MongoClient mongoClient = // either use spring or create your own MongoClient instance with the above codecregistry
    
     // now simply write POJOs to the database or read from it
    
```
In general your declared POJOs will need an empty constructor, or a no-arg constructor. (Please feel free to add a mechanism to allow for control over entity instantiation)

If you need to encode/decode enums, you could simply add [EnumCodecProvider](src/main/java/de/bild/codec/EnumCodecProvider.java) to the codec registry.
In that case enum will be encoded with their names. If you have special needs for enum serialization/deserialization, you can register your own enum codec and chain it with a higher priority. 

## Polymorphic hierarchies
```java
public interface Base {
}

// discriminator: _t : "A"
public class A implements Base {}

// discriminator: _t : "NewEncodingDiscriminatorValue"
// decoding also if _t : "SomeAlias" or _t : "SomOtherOldValue"
@de.bild.codec.annotations.Discriminator(value = "NewEncodingDiscriminatorValue", aliases = {"SomeAlias", "SomOtherOldValue"})
public class B implements Base {}

//use @de.bild.codec.annotations.Polymorphic to instruct the encoder to definitely write a discriminator into the database
// otherwise no discriminator will be written, as the codec assumes this is a non-polymorphic POJO 
@de.bild.codec.annotations.Polymorphic
public class InitialPojoWithNoSubTypes {}

// if later in yor project the POJO evolves into polymorphic structures, mark it as fallback to decode any entity within the database that has no discriinator
@de.bild.codec.annotations.DiscriminatorFallback
public class InitialPojoWithNoSubTypes {}

// you can control the discriminator key as follows
// results in: "someKey" : "PojoEntity"
// Attention: You can use different discriminator keys within your polymorphic structure, but this may be confusing,hence it is discouraged
// If two or more entities in a polymorphic structure use the same combination of DiscriminatorKey:Discriminator an exception will be thrown when initializing the CodecProvider as then the destination Pojo is ambiguous
@de.bild.codec.annotations.DiscriminatorKey("someKey")
@de.bild.codec.annotations.Discriminator("PojoEntity")
public class Pojo {}
```


If you try to encode an entity that is not registered during setup of the POJO codec, but which is a subclass of a class that was registered during setup phase,
the codec walks up the class hierarchy until a registered entity is found that is in the set of registered classes. Then the properties of that class will be persisted.
That means you loose properties while encoding to the database, but sometimes this behaviour might be desired, e.g. if you use transient fields within the POJO that you don't want to get persisted.
Alternatively you could mark those properties with [@Transient](src/main/java/de/bild/codec/annotations/Transient.java)
Note that decoding into POJOs that are not registered within the codec does not work. An [NonRegisteredModelClassException](src/main/java/de/bild/codec/NonRegisteredModelClassException.java) will be thrown.

## Updating entities
Updating entities is straight forward.

```java
    Bson update = combine(set("user", "Jim"),  / String
                set("action", Action.DELETE), // enum
                currentDate("creationDate"),
                currentTimestamp("_id"));
    FindOneAndUpdateOptions findOptions = new FindOneAndUpdateOptions();
        findOptions.upsert(true);
        findOptions.returnDocument(ReturnDocument.AFTER);

        MongoCollection<Pojo> pojoMongoCollection = mongoClient.getDatabase("test").getCollection("documents").withDocumentClass(Pojo.class);

        Pojo pojo = pojoMongoCollection.findOneAndUpdate(Filters.and(Filters.lt(DBCollection.ID_FIELD_NAME, 0),
                Filters.gt(DBCollection.ID_FIELD_NAME, 0)), update, findOptions);

```

__Attention__
Please be aware that you __cannot__ directly update fields with generic types! You would need to create a concrete subclass of that type so the POJO codec can determine runtime type information, as otherwise type arguments cannot be resolved.
Please have a look at [UpdateTest](src/test/java/de/bild/codec/update/UpdateTest.java) for an example on how to do that.
Please feel free to support this project and provide a solution for that issues. Ideas on how to solve it are outlined in the test itself.

## Advanced usage

If you have a data structure within your mongo whereby you are not exactly sure what fields are declared, but you know 
some of the declared fields, use the following approach to decode the known fields into known POJOs.
```java
public class MapWithSpecialFieldsPojo extends Document implements SpecialFieldsMap {
    @FieldMapping("someOtherPojo")
    public SomeOtherPojo getSomeOtherPojo() {
        return (Map) get("someOtherPojo");
    }
}
```

If you need fine grained control over deserialization and serialization register a CodecResolver
For an example of usage @see [CodecResolverTest](src/test/java/de/bild/codec/CodecResolverTest.java)
```java
PojoCodecProvider.builder()
    .register(CodecResolverTest.class)
    .registerCodecResolver((CodecResolver) (type, typeCodecRegistry) -> {
        if (TypeUtils.isAssignable(type, Base.class)) {
            return new DocumentCodec(type, typeCodecRegistry);
        }
        return null;
    })
    .build()
```

## Default values
For now the codecs that encode collections(set, list) and maps encode __null__ values as empty collections or empty maps.
It might be a future improvement to better control this behaviour.
The idea is to provide an annotation that describes the default value in case the field value is null.
Feel free to add this functionality. 
```java
// e.g.
public class Pojo {
    @Default(DefaultValueProvider.class)
    Integer aField;
}
```


## Release Notes

Release notes are available [here](https://github.com/todo).

```xml
<dependency>
    <groupId>de.bild.backend</groupId>
    <artifactId>polymorphia</artifactId>
    <version>x.y.z</version>
</dependency>
```


## Build

To build and test the driver:

```
$ git clone https://github.com/axelspringer/polymorphia
$ cd polymorphia
$ mvn clean install
```
The tests will spin up a spring boor environment and use flapdoodle to run a local mongo db.


## Todos
* Once Mongo Java Driver 3.5 (or 3.6 most likely) is released, the codec needs to add some lines of code to be more fault tolerant.
[JAVA-2416](https://jira.mongodb.org/browse/JAVA-2416). If multiple marks can be set on the reader we can set a mark whenever 
we read a new entity from the reader. If that entity contains malformed data that cannot be decoded an exception may result and the reader is left in an arbitrary state. 
Resetting the reader to the last mark and skipping the broken entity solves this problem.
* adding more tests

