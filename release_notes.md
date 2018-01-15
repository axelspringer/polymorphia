Release Notes
=======

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