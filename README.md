## POJO (Plain old java objects) Codec for mongo db

You can use this codec to encode plain old java objects into a mongo database and to decode those back again into POJOs.

## Main features 
 * minimal configuration - but configurable when needed
 * support for polymorphic class hierarchies
 * support for generic types
 * fine grained control over discriminator keys and values (needed to morph into the correct POJO while decoding)
 * fast
 * support for primitives and their object counterparts, sets, lists, maps (Map<String, T> as well as Map<KeyType,ValueType>) multi dimensional arrays, enums
 * allows for easy application [@Id(collectible = true)](src/main/java/de/bild/codec/annotations/Id.java) generation [@see CollectibleCodec](org.bson.codecs.CollectibleCodec)
   * example: [IdTest](src/test/java/de/bild/codec/id/IdTest.java)
 * provides fine grained control over restructuring data written to mongo (and reading from mongo) 
 * partial POJO codec [SpecialFieldsMapCodec](src/main/java/de/bild/codec/SpecialFieldsMapCodec.java)
 * life cycle hook support (pre safe, post load)
 * support mongo java driver > version 3.5


## Release Notes

Release notes are available [release_notes.md](release_notes.md).

```xml
<dependency>
    <groupId>de.bild.backend</groupId>
    <artifactId>polymorphia</artifactId>
    <version>2.3.4</version>
</dependency>
```

## Usage
Note: There are plenty of code examples to be found in the tests.

Attention: In order to scan packages you need to provide either [spring-core](https://github.com/spring-projects/spring-framework) library or [org.reflections.reflections](https://github.com/ronmamo/reflections) library in the class path.
Alternatively you could register all your POJO classes one by one!
When using Polymorphia > version 2.3.0 you can register your own [ClassResolver](src/main/java/de/bild/codec/ClassResolver.java)

Instantiate the [PojoCodecProvider](src/main/java/de/bild/codec/PojoCodecProvider.java) and add it to the CodecRegistry.
Example: [PolymorphicReflectionCodecTest](src/test/java/de/bild/codec/PolymorphicReflectionCodecTest.java)

One motivation to start this project was the missing feature of existing Pojo-codec solutions to easily model polymorphic structures and write/read those to/from the database (e.g.[https://github.com/mongodb/morphia/issues/22](https://github.com/mongodb/morphia/issues/22))

Simple example @see example [PolymorphicCollectionTest](src/test/java/de/bild/codec/PolymorphicCollectionTest.java)
```java
        public interface MyCollectionThing {}
    
        public interface Shape extends MyCollectionThing {}
    
        public class Circle implements Shape {
            int radius;
        }
    
        public class Square implements Shape  {
            int x;
        }
                
        public static void main() {
            MongoCollection<MyCollectionThing> collection = database.getCollection("things").withDocumentClass(MyCollectionThing.class);
    
            Dog brownDog = new Dog("brown");
            Tuna oldTuna = new Tuna(123);
            Circle smallCircle = new Circle(2);
            Cat generalCat = new Cat(9);
            Rectangle rectangle = new Rectangle(2, 5);
            Circle mediumCircle = new Circle(5);
            Square square = new Square(4);
        
            // insert all items into collection of type MongoCollection<MyCollectionThing>
            collection.insertMany(Arrays.asList(brownDog, oldTuna, smallCircle, generalCat, rectangle, mediumCircle, square));
            
            // only find things in collection of type Shape.class...
            for (MyCollectionThing myCollectionThing : collection.find(POJO_CODEC_PROVIDER.getTypeFilter(Shape.class, codecRegistry))) {
                System.out.println(myCollectionThing.getClass());
            }
        
            //would print---
            //Circle
            //Rectangle
            //Circle
            //Square

        }
        
```

More complex example (including use of generics)
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
    float[][][] floats; // multi dimensional float array
}
```
   
```java
    PojoCodecProvider.builder()
        .register("de.bild.codec.model") //adds all static classes of a package to the model
        .register(Pojo.class) // adds a single class
        .build();

    CodecRegistry pojoCodecRegistry = fromRegistries(fromProviders(pojoCodecProvider), MongoClient.getDefaultCodecRegistry());
    
    MongoClient mongoClient = // either use spring auto configuration or create your own MongoClient instance with the above codecregistry
    
     // now simply write POJOs to the database or read from it
    
```
In general your declared POJOs will need an empty default constructor, or a no-arg constructor (access level private will do). (Please feel free to add a mechanism to allow for control over entity instantiation)

If you need to encode/decode enums, you could simply add [EnumCodecProvider](src/main/java/de/bild/codec/EnumCodecProvider.java) to the codec registry.
In that case enum will be encoded with their names. If you have special needs for enum serialization/deserialization, you can register your own enum codec and chain it with a higher priority. 

## Polymorphic hierarchies

The following code examples describe how you can control discriminator keys and discriminator values. These are needed to enable the codec to morph into the correct polymorphic classes.  
Please note that there is absolutely no need to override the default behaviour.  
If your class structures are not annotated with [@Discriminator](src/main/java/de/bild/codec/annotations/Discriminator.java) or [@DiscriminatorKey](src/main/java/de/bild/codec/annotations/DiscriminatorKey.java) the default behaviour is as follows:
* [@Discriminator](src/main/java/de/bild/codec/annotations/Discriminator.java) is the simple class name
* [@DiscriminatorKey](src/main/java/de/bild/codec/annotations/DiscriminatorKey.java) is "_t"
* **ATTENTION: Discriminator-information is only persisted in the database, if polymorphic structures are detected**
* to force persisting discriminator information to the database either add [@Polymorphic](src/main/java/de/bild/codec/annotations/Polymorphic.java) to your class structure or you annotate a class with [@Discriminator](src/main/java/de/bild/codec/annotations/Discriminator.java)

```java
public interface Base {
}

// discriminator: _t : "A"
public class A implements Base {}

// discriminator: _t : "NewEncodingDiscriminatorValue"
// decoding also if _t : "SomeAlias" or _t : "SomOtherOldValue"
@de.bild.codec.annotations.Discriminator(value = "NewEncodingDiscriminatorValue", aliases = {"SomeAlias", "SomOtherOldValue"})
public class B implements Base {}

// use @de.bild.codec.annotations.Polymorphic to instruct the encoder to definitely write a discriminator into the database
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
the codec walks up the class hierarchy until a registered entity is found that is in the set of registered classes. Then the properties of __that__ class will be persisted.
That means you loose properties while encoding to the database. This behaviour might be desired, e.g. if your working instance contains information that is only needed at runtime but that are not relevant for storage.
Alternatively you could mark those properties with [@Transient](src/main/java/de/bild/codec/annotations/Transient.java)
Note that decoding into POJOs that are not registered within the codec does not work.  
A [NonRegisteredModelClassException](src/main/java/de/bild/codec/NonRegisteredModelClassException.java) will be thrown.


## Application Id generation
The mongo java driver allows for application id generation. In general to enable this feature, the mongo java driver requests the codec responsible for an entity to implement org.bson.codecs.CollectibleCodec. This is handled transparently within Polymorphia and you do not need to care about.   

Within your pojo class you can annotate a **single** property with [@Id(collectible=true, value = CustomIdGenerator.class)](src/main/java/de/bild/codec/annotations/Id.java)
When the codec for this pojo is being built and such an Id annotated field is found, the final codec will be wrapped into a java.lang.reflect.Proxy that implements the org.bson.codecs.CollectibleCodec interface.
If your Pojo is part of a polymorphic structure, one or all of your subclasses within that structure can allow for application id generation. Each subclass can generate individual Ids of different types. The mongo database is capable of handling different ids in one collection.
The PolymorphicReflectionCodec takes care of this and will implement the CollectibleCodec interface if (and only if) at least one PolymorphicCodec (one for each sub type) is found to be collectible. 


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
Please be aware that you __cannot__ directly update fields with generic types! You would need to create a concrete subclass of that type so the POJO codec can determine runtime type information, as otherwise type arguments cannot be resolved and you would loose information while encoding to the database (best case).
Please have a look at [UpdateTest](src/test/java/de/bild/codec/update/UpdateTest.java) for an example on how to do that.
Please feel free to support this project and provide a solution for that issues. Ideas on how to solve it are outlined in the test itself.

## Ignoring classes during package scan

When builing your PojoCodecProvider, register some arbitrary annotation types that - once found at your model classes - have the effect that these classes won't be indexed.
The advantage is, that you can keep all your model classes in one package even though you register different Codecs for some types. 
Mark the types you want to be ignored with one (or if desired more) of your created annotation types.
You may wonder, why different annotations can be defined to ignore types. As ou could define multiple instances of PojoCodecProvider you have better control of type exclusions, depending on the instantiated PojoCodecProvider. 
 
```java
    PojoCodecProvider.builder()
        .register(Pojo.class.getPackage().getName())
        .ignoreTypesAnnotatedWith(IgnoreAnnotation.class)
        .build()

```

## Advanced usage

If you have a data structure within your mongo whereby you are not exactly sure what fields are declared, but you know 
some of the declared fields, use the following approach to decode the known fields into known POJOs.
@Todo: It would be great, if the names of the properties could be inferred from the method names.

```java
public class MapWithSpecialFieldsPojo extends Document implements SpecialFieldsMap {
    @FieldMapping("someNiceDate")
    public Date getSomeNiceDate() {
        return (Date) get("someNiceDate");
    }
    @FieldMapping("someNicePojo")
    public SomeNicePojo getSomeNicePojo() {
        return (SomeNicePojo) get("someNicePojo");
    }
}
```


## Hooks for custom codecs

If you want to provide your own serialization and deserialization codecs but at the same time want to benefit from Polymorphias capabilities 
to map polymorphic structures, you can chain your custom codecs by registering a [CodecResolver](src/main/java/de/bild/codec/CodecResolver.java) when building 
the [PojoCodecProvider](src/main/java/de/bild/codec/PojoCodecProvider.java)  
These codecs however need to implement a specialization of org.bson.codecs.Codec namely [PolymorphicCodec](src/main/java/de/bild/codec/PolymorphicCodec.java)   
This interface adds the following features:
 * handles discriminator issues like check for fields names that must not be equal to any discriminator key
 * provides methods to encode any additional fields besides the discriminator  
 * copies the method signatures of org.bson.codecs.CollectibleCodec but without implementing CollectibleCodec (The reason is explained in the section [Application Id generation])
   * this enables your codec to generate application ids
 * provides methods to instantiate your entities and set default values

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

### [TypeCodecProvider](src/main/java/de/bild/codec/TypeCodecProvider.java)
If you desire to register a codec provider that can provide a codec for any given type (not just for java.lang.Class) you may register a [TypeCodecProvider](src/main/java/de/bild/codec/TypeCodecProvider.java)   
For an example of usage have a look at [TypeCodecProviderTest](src/test/java/de/bild/codec/typecodecprovider/TypeCodecProviderTest.java)
You can override any fully specified type or generic type. You can e.g. register alternative codecs for Set or List or Map.   
In contrast to org.bson.codecs.configuration.CodecProvider the registered [TypeCodecProvider](src/main/java/de/bild/codec/TypeCodecProvider.java) accepts any java.lang.reflect.Type
Please be aware of the fact, that these TypeCodecProviders will only take effect when resolved within PojoCodecProvider. 


## Default values
As of Polymoprhia version 2.0.0 the developer has better control over null-handling while encoding to the database. Additionally a developer can control default values when decoding undefined fields.
Use [EncodeNulls](de.bild.codec.annotations.EncodeNulls) to decide whether you need nulls written to the database.     
You can convert null values into defaults before the encoder kicks in. Use [EncodeNullHandlingStrategy](de.bild.codec.annotations.EncodeNullHandlingStrategy) to assign values to null fields if desired.


While decoding fields that are present in the pojo but have undefined values in the database (evolution of pojos!) you can assign a [DecodeUndefinedHandlingStrategy](de.bild.codec.annotations.DecodeUndefinedHandlingStrategy).

The mentioned annotations can be used at class level and field level. Field level annotations overrule class annotations. It is also possible to set global behaviour for these configurations. When building your [PojoCodecProvider](src/main/java/de/bild/codec/PojoCodecProvider.java) use the Builder methods to control the global defaults.
* de.bild.codec.PojoCodecProvider.Builder#encodeNullHandlingStrategy(Strategy)  -> defaults to CODEC (historical reasons)
* de.bild.codec.PojoCodecProvider.Builder#decodeUndefinedHandlingStrategy(Strategy) -> defaults to KEEP_POJO_DEFAULT
* de.bild.codec.PojoCodecProvider.Builder#encodeNulls(boolean) -> defaluts to false

Have a look at [NullHandlingTest](src/test/java/de/bild/codec/NullHandlingTest.java) for an example.

```java

PojoCodecProvider.builder()
        .register(NullHandlingTest.class)
        .encodeNulls(false)
        .decodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.KEEP_POJO_DEFAULT)
        .encodeNullHandlingStrategy(EncodeNullHandlingStrategy.Strategy.KEEP_NULL)
        .build()
                                    

    @EncodeNulls(false) 
    public class Pojo {
        @DecodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.CODEC)
        Integer aField;
  
        @DecodeUndefinedHandlingStrategy(DecodeUndefinedHandlingStrategy.Strategy.SET_TO_NULL)
        @EncodeNulls         
        String anotherField;
    }
```


## Build

To build and test the driver:

```
$ git clone https://github.com/axelspringer/polymorphia
$ cd polymorphia
$ mvn clean install
```
The tests will spin up a spring boot environment and use [flapdoodle](https://github.com/flapdoodle-oss/de.flapdoodle.embed.mongo) to run an in-memory mongo db.


## Todos
* Once Mongo Java Driver 3.5 (or 3.6 most likely) is released, the codec needs to add some lines of code to be more fault tolerant.
[JAVA-2416](https://jira.mongodb.org/browse/JAVA-2416). If multiple marks can be set on the reader we can set a mark whenever 
we read a new entity from the reader. If that entity contains malformed data that cannot be decoded an exception may result and the reader is left in an arbitrary state. 
Resetting the reader to the last mark and skipping the broken entity solves this problem.
* adding more tests
* implement the todos/features mentioned above

