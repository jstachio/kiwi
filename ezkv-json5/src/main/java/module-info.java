/**
 * Provides a <a href="https://json5.org/">JSON5</a> capable key values media parser
 * that has zero dependencies. It is also able
 * to parse strict JSON as well.
 * <p>
 * The parser was highly inspired by 
 * <a href="https://github.com/Synt4xErr0r4/json5">
 * Synt4xErr0r4 json5
 * </a>. 
 * It is MIT license and thus compatible with this libraries license.
 * 
 * @provides io.jstach.ezkv.kvs.KeyValuesServiceProvider
 * @see io.jstach.ezkv.json5.JSON5KeyValuesMedia
 * @apiNote The JSON5 parser implementation is not exposed as
 * it is likely to change.
 */
@SuppressWarnings("module") // yes javac I want the damn 5 on the end
module io.jstach.ezkv.json5 {
	exports io.jstach.ezkv.json5;
	requires transitive io.jstach.ezkv.kvs;
	requires org.jspecify;
	provides io.jstach.ezkv.kvs.KeyValuesServiceProvider with io.jstach.ezkv.json5.JSON5KeyValuesMedia;
}