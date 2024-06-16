# OMS Kotlin Jackson Document Library
[![standard-readme compliant](https://img.shields.io/badge/readme%20style-standard-brightgreen.svg?style=plastic)](https://github.com/RichardLitt/standard-readme)

## Table of Contents
- [Table of Contents](#table-of-contents)
    - [Background](#background)
        - [Basic Project Structure](#basic-project-structure)
    - [Library Usage](#library-usage)
        - [JacksonDocument Instantiation](#jacksondocument-instantiation)
        - [JacksonDocument Navigation](#jacksondocument-navigation)

## Background
This library provides a JSON Kotlin DSL inspired by Koson while being completely based on Jackson (which is the *de-facto* reference for Json manipulation in the JDK world).

### Basic Project Structure
The "OMS Kotlin Jackson Document" code-base root package is
```
commons.json
```

## Library Usage
Usage of the library is better explained by samples:
* JacksonDocument instantiation
* JacksonDocument builder
* JacksonDocument navigation via Json Path

### JacksonDocument Instantiation
In order to instantiate a JacksonDocument take the following pseudocode snippet as reference:

```
 From an exising Jackson JsonNode instance
 val document = JacksonDocument(rootNode)
 
 Via the Json DSL enabled by the library:
         val document = obj {
            "message" to "hello"
            
            "arrayTest" to arr[
                    obj {
                        "Message" to "test_message"
                    },

                    obj {
                        "Message2" to "test_message"
                    },
            ]
        }.toJacksonDocument()
 
```
### JacksonDocument Navigation
Assuming you have a JacksonDocument instance with you as in the following example:
```
val document = obj {
                  "Integer" to 2
                  "String" to "test"
                  "Boolean" to true
                  "Array" to arr[ 1, 2, obj { "Hello" to "World" } ]
                }.toJacksonDocument()
```
then it is true that
```
 document.string("$.String") == "test"
 document.boolean("$.Boolean") ?: false
 document.number("$.Integer")?.compareTo(2.toBigDecimal()) == 0
 document.string("$.Array[2].Hello") == "World"