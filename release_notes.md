Release Notes
=======
## 2.8.0
* tests now run with newer spring boot version (in order to test newer mongodb versions (driver and db))
* tests require a docker setup since a MongoDB 4.1 [TestContainer](https://www.testcontainers.org/) spins up  

## 2.7.0
* introducing InstanceAwareIdGenerator to allow for instance aware id generation
* add codec transcoding dynamically typed Object values

## 2.6.0
* documentation update
* declare mongo-java-driver dependency as provided - so no need to exclude that dependency in projects
* reverting some generic declaration changes made in version 2.5.0 in RefelctionCodec that may interfere with legacy code

## 2.5.0
* add support to react on exceptional states (DecodingPojoFailureStrategy and DecodingFieldFailureStrategy)
* needs mongo.java-driver version > 3.7.0

## 2.4.0
* ensure compatibility with JDK 11

## 2.3.5
* allow for non generic List, Map, Set within Pojos
* filter for specific type within mongo collection: return null, if super class hierarchy can't be determined

## 2.3.4
* adding example/support for searching a whole super-class tree in a general collection that stores anything (@see PolymorphicCollectionTest) 

## 2.3.3
* a slightly cleaner way to handle parameterized List/Map/Set codecs. If they fail to create, the mongo driver default gets chosen.
* unfortunately there is no easy way to figure out if the CodecRegistry could provide a codec for a given type. Eventually an exception is being thrown instead of returning just null


## 2.3.2
* minor (dirty) fix for parameterized List/Map/Set codecs. If they fail to create, the mongo driver default gets chosen.

## 2.3.1
* minor fix in SpecialFieldsMapCodec

## 2.3.0
* For standard codecs a wrapper was added to enable to use them within polymorphic structures
* when PolymorphicCodecs are registered within the chain of codecs, Polymorphia will use them when encoding polymorphic types (instead of using a reflection based codec)
* PojoCodecProvider.Builder was enriched by two methods to enable excluding classes when building the domain model
* fine tuning while identifying polymorphic types
* type hierarchy was broken if super classes are not part of the domain model but some other superclass in the class hierarchy
* adding support to register a specialized ClassResolver if neither org.springframework.spring-core or org.reflections.reflections are sufficient when resolving classes  


## 2.2.0
* mongo version > 3.5 support
* changed enum handling

## 2.1.1
* removing PojoCodecProvider.getDefaultCodecRegistry - instead, all "broken" codecs will be ignored
* ArrayCodec now encodes bytes[] with mongo native encoding


## 2.1.0
* rewrite of ArrayCodec
* all container like codecs (CollectionTypeCodec, MapTypeCodec, ArrayCodec) now support null values
* java.lang.Object can be used as type in pojos - all classes to be encoded/decoded must be registered to the PojoCodecProvider
* improved logging when no codec can be found for a field
* a DefaultCodecRegistry similar to the one the mongo driver provides can be retrieved via PojoCodecProvider.getDefaultCodecRegistry()
* SortedMap is now supported as pap-type
* if a CollectibleCodec-Proxy is being returned from the PojoCodecProvider, all interfaces of the delegating codec are exposed as well
* the new interface DelegatingCodec - CollectibleCodec-Proxies can be asked for their delegate
* when resolving Codecs for Class-types within the pojo-context, the CodecRegistry-chain will be asked. This allows for overriding codecs for Class types. 

##2.0.0
* Improved error checking while application id generation
* add support to register codecs for any arbitrary type -> de.bild.codec.TypeCodecProvider
* support for SortedSet added
* support for polymorphic codecs that do not need to be ReflectionCodecs  -> PolymorphicCodec CodecResolver.getCodec(...)
* improved null-value handling: now nulls can be encoded as nulls or nulls are not written at all -> de.bild.codec.annotations.EncodeNulls
* added annotation driven null value handling while encoding -> de.bild.codec.annotations.EncodeNullHandlingStrategy
* added annotation driven undefined value handling while decoding -> de.bild.codec.annotations.DecodeUndefinedHandlingStrategy



##1.7.0

added support to ignore model classes in scanned packages


##1.6.2
bug fix for [https://github.com/axelspringer/polymorphia/issues/5](https://github.com/axelspringer/polymorphia/issues/5) 



##1.6.1
bug fix for [https://github.com/axelspringer/polymorphia/issues/4](https://github.com/axelspringer/polymorphia/issues/4) 


##1.6.0
return null for unresolvable discriminator values 

##1.5.0
Abstract classes have been removed from indexing within PolymorphicReflectionCodec

##1.4.0
Improved handling for "legacy" entities that have no discriminator while encoding

## 1.3.0
 
## 1.2.0
- bug fix for polymorphic type detection

## 1.1.0
- now with pre save hook

## 1.0.0
**New**
- first version released