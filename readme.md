# Kiwi

A non-opinionated Java *bootstrapping configuration* library
that allows recursive chain loading of configuration from key values

**Key values are everywhere!** 
(also known as an associative array, or name value pairs)

Environment variables, System properties, cloud meta data, vault,
HTTP FORM post, URI queries, command line arguments
even most forms of JSON, HOCON, TOML YAML and XML
can all be represented as simple *"key value"* pairs.

Thus it is the perfect denominator for providing applications with initial configuration.
We call this *"bootstrapping configuration"* and gathered even before logging.

Kiwi loads streams of key values (KeyValue) from resources. 
What is unique about it is that certain key values will load more key values to the current
stream which is what we call chaining.

    URI -> KeyValues\* -> URI\* -> KeyValues -> ...

**Thus it is a key values configuration framework that can be configured with key values!**

A simple example using  `java.util.properties` that could be parsed to `KeyValues` would be:


**system.properties** (first loaded):

```properties
message=Hello ${user.name}
_load_foo=classpath:/bar.properties
```

**bar.properties loaded** (second loaded):

`classpath:/bar.properties`

```properties
user.name=kenny
message="Merchandising"
```

The effective key values will be:

```properties
message=Merchandising
user.name=kenny
```

## Kiwi is not and does not want to be `System.getProperty` replacement


In fact Kiwi rather just fill `System.getProperties` from loaded resources so that
you do not have to use another library

Most configuration frameworks are focused on *"binding"*, dependency injection, or ergonomics on a
`Map<String,String>`.

Kiwi is much lower level than those libraries and because of its zero dependency and no logging architecture
it can be used very early to provide those other early init libraries with a `Map<String,String>`.


## Kiwi is/has:

* Zero dependencies
* Zero reflection
* Zero auto loading - you pick that
* Zero logging (unless you want it)
* **Zero opinions** and just pure utility of loading resources.
*  a `module-info.java`, jspecify annotations, and  jdk 21 ready.
* **Framework agnostic!**
* Fast initialization
* Chaining of overriding key values


## Architecture

Kiwi's two major concepts are:

1. KeyValues - a stream of key values
1. Resources - a URI with associated key value meta data.

Resources are used to load key values and key values can be used to specify and find more resources 
(to load more key values). Kiwi is very recursive

### KeyValue

A KeyValue object in Kiwi unlike a `Map.Entry<String,String>` has more information than just the `key`
and `value`. 

Each key value is/has:

* Immutable
* Interpolated value as well as the original pre-interpolated value
* Source information
* Whether or not it should be used for interpolation
* Whether or not it should be printed out ever (e.g. a password or other sensitive information)

Kiwi provides ergonomics working with streams of key values to filter, and collect key values
as well as parse and format.

A KeyValue can be a special key that can reference another resource to load.

### KeyValuesResource 



### KeyValueMedia

Kiwi provides a framework to parse key values from byte streams, 
or strings based on media type or file extension.


