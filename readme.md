# Kiwi

A non-opinionated Java *bootstrapping configuration* library
that allows recursive chain loading of configuration from key values.

**Key values are everywhere** 
(also known as an associative arrays, or name value pairs)!

Environment variables, System properties, cloud meta data, vault,
HTTP FORM post, URI queries, command line arguments
even most forms of JSON, HOCON, TOML YAML and XML
can all be represented as simple *"key value"* pairs.

Thus it is the perfect common denominator for providing applications with initial configuration.
We call this *"bootstrapping configuration"* and it is usually gathered even before logging.

To do this Kiwi loads streams of key values (KeyValue) from resources. 
What is unique about it is that certain key values will load more key values to the current
stream which is what we call chaining.

    URI -> KeyValues\* -> URI\* -> KeyValues -> ...

Streams are useful because they can be filtered and transformed
which matters because keys and values from various sources often need transformation. 
For example environment variable names often need to be converted to lower case
and some prefix removed.

**In short it is a configuration framework that can be configured with key values.**

A simple example using  `java.util.Properties` files that could be parsed to `KeyValues` would be:


```java
var kvs = KeyValuesSystem.defaults()
  .loader()
  .add("classpath:/start.properties")
  .add("system:///") // add system properties to override
  .add("env:///")    // add env variables to override
  .add("cmd:///-D")  // add command line for final override
  .load();

// give the key values to some other config framework like Spring:
var map = kvs.toMap();
ConfigurableEnvironment env = applicationContext.getEnvironment();
env.getPropertySources().addFirst(new MapPropertySource("start", map));
```

**start.properties** (first loaded):

```properties
message=Hello ${user.name}
_load_foo=classpath:/foo.properties
port.prefix=1
```

**foo.properties loaded** (second loaded resource):


```properties

user.name=kenny
message="Merchandising"
db.port=${port.prefix}5672
_load_bar=file:/${user.home}/.config/myapp/user.properties
_flag_bar=sensitive
```

**user.properties** (third loaded resource treated as sensitive)

```properties
secret=12345 # my luggage combination
port.prefix=3
```

The effective key values printed out will be:

```properties
message=Merchandising
user.name=kenny
port.prefix=3
db.port=35672
secret=REDACTED
```


## Kiwi is not `System.getProperty` or other config framework replacements

Most configuration frameworks are focused on *"binding"*, dependency injection, or ergonomics on a
`Map<String,String>`. They are focused on transforming the flat key values to objects. Kiwi does not do that.

Kiwi is much lower level than those libraries and because of its zero dependency and no logging architecture
it can be used very early to provide those other early init libraries with a `Map<String,String>`
(or the complete stream of key values found).

In fact Kiwi rather just fill `System.getProperties` from loaded resources so that
you do not have to use another library for configuration lookup. That is for retrieval 
a singleton like `System.getProperties` is often good enough. 


## Kiwi's advantages:

* **Zero opinions** - it does not assume you want to load `app.properties` first. You define whats resources and order.
* Zero dependencies
* Zero reflection
* Zero auto loading - you pick that
* Zero logging (unless you want it)
*  a `module-info.java`, jspecify annotations, and  jdk 21 ready.
* **Framework agnostic!**
* Fast initialization
* Chaining of overriding key values


## Architecture

Kiwi's two major concepts are:

1. KeyValues - a stream of key values
1. Resources - a URI with associated key value meta data.

Resources are used to load key values and key values can be used to specify and find more resources 
(to load more key values). Kiwi is recursive.

### KeyValue

A `KeyValue` object in Kiwi unlike a `Map.Entry<String,String>` has more information than just the simple tuple of `key` and `value`. 

`KeyValue` are:

* Immutable
* Have interpolated value as well as the original pre-interpolated value
* Have source information
* Whether or not it should be used for interpolation
* Whether or not it should be printed out ever (e.g. a password or other sensitive information)

Kiwi provides ergonomics working with streams of key values to filter, and collect key values
as well as parse and format.

**Notice that Kiwi is like a list of key values and thus:**

* Order can be important
* There can be duplicate "keys" (that may or may not override in the final result)

Finally a KeyValue can be a special key that can reference another resource to load.

These keys are usually prefixed with `_` to avoid collision and maximum compatibility.
The most important one is `_load_name` where name is the name you like to give the resource and the value is a `URI`.

### KeyValuesResource 

A `KeyValuesResource` has a `URI` and symbolic name (used to find configuration). 
It is backed by a key/value with additional meta data on how to load that resource. A
URIs are designed to point at resources and the additional meta data 
in a `KeyValuesResource` surprise surprise is more `KeyValues`. 

The additional meta data is used to know how to load the key values 
and what meta data should be associated with each key value.

Some examples are:

* The key values from the resource are sensitive and should not be easily printed out
* The key values should not be interpolated because the data is raw
* The loaded key values should or should not load other key values

This is all configurable again through key values (and URIs).

### KeyValuesResourceLoader

A resource loader will take a `KeyValuesResource` and turn it into `KeyValues`.

Essentially it is an extension point to take a URI and load `KeyValues` usually
based on the schema of the URI. For example `classpath` will use the JDK 
classloader mechanism and `file` will use `java.io`/`java.nio` file loading.

This is part of the library is extendable.

### KeyValueMedia

Some `KeyValuesResourceLoader`s will know how to parse the `URI` directly to key values
BUT many will will want to use a parser. 

Kiwi provides a framework to parse and format key values from/to byte streams, 
or strings based on ["media type" aka "Content Type" aka MIME](https://en.wikipedia.org/wiki/Media_type) 
or file extension.

This part of the library is extendable.

## History

This library is a rewrite of an organic part of my companies code base that
has been evolving for over 14 years.

Many libraries and frameworks have come and gone with differing opinions on configuration. 
While our backing frameworks have changed over the years our configuration style, format
and behavior because of the library has largely not thanks to the flexibility.

Opinionated maybe vogue but **not** opinionated goes the distance.

## Other work

* [avaje-config](https://avaje.io/config/) 

Kiwi hopes to bring many of its concepts and design to `avaje-config`
