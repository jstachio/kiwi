
/**
 * Kiwi Boot is tiny opinionated configuration framework modeled after Spring
 * Boot that uses Kiwi KVS system to load key values into Map. It is made to be
 * a slightly better {@link System#getProperties()}.
 * <p>
 * Features are purposely very limited.
 * <p>
 * The loading is as follows:
 * <ol>
 * <li>System.getProperties and System.getEnv are load for interpolation.</li>
 * <li>Resource <code>classpath:/application.properties</code> is loaded (but not required)</li>
 * <li>Resource <code>profile.classpath:/application__PROFILE__.properties</code> is loaded (but not required)
 * </li>
 * </ol>
 * @see io.jstach.kiwi.boot.KiwiConfig
 */
module io.jstach.kiwi.boot {
	exports io.jstach.kiwi.boot;
	requires io.jstach.kiwi.kvs;
	requires static org.jspecify;
	/*
	 * I cannot remember if JLink will pull this in or not.
	 */
	requires static java.management;
}