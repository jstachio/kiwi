/**
 * Provides a Dotenv capable key values media parser
 * based on <a href="https://github.com/motdotla/dotenv">https://github.com/motdotla/dotenv</a>
 * (<a href="https://search.maven.org/artifact/io.jstach.ezkv/ezkv-dotenv/_VERSION_/jar">
 * io.jstach.ezkv:ezkv-dotenv:_VERSION_
 * </a>).
 * @provides io.jstach.ezkv.kvs.KeyValuesServiceProvider
 * @see io.jstach.ezkv.dotenv.DotEnvKeyValuesMedia
 */
module io.jstach.ezkv.dotenv {
	exports io.jstach.ezkv.dotenv;
	requires transitive io.jstach.ezkv.kvs;
	requires org.jspecify;
	provides io.jstach.ezkv.kvs.KeyValuesServiceProvider with io.jstach.ezkv.dotenv.DotEnvKeyValuesMedia;
}