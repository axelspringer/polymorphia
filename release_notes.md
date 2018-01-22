Release Notes
=======

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