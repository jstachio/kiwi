# Kiwi

A non-opinionated Java bootstrapping configuration library.

Key values are everywhere (also known as an associative array, or name value pairs).

Environment variables, System properties, cloud meta data, vault,
HTTP FORM post, URI queryies, command line arguments
even most forms of JSON, TOML YAML and XML
can all be represented as simple key value pairs.

Thus it is the perfect denominator for providing applications with initial configuration.
We call this "bootstrapping" configuration and gathered even before logging.

Kiwi loads streams of key values (KeyValue) from resources. What is unique about it
is that certain key values will load more key values.

**Thus it is a key values configuration framework that can be configured with key values.**


Most configuration frameworks are focused on *"binding"*, dependency injection, or ergonomics on a
`Map<String,String>`.

Kiwi is much lower level than those libraries and because of its zero dependency no logging architecture
it can be used very early to provide those other config libraries with a `Map<String,String>`.
