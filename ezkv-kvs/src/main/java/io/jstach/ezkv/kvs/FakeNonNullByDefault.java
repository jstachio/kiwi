package io.jstach.ezkv.kvs;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation is a hack provided for Eclipse Null analysis support. See
 * https://github.com/eclipse-jdt/eclipse.jdt.core/issues/3434
 *
 * It is not public API and will be removed in the future once Eclipse supports JSpecify.
 *
 * The Eclipse EEA are stored in etc/eea and the Eclipse preferences needed are in
 * etc/eea/prefs.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@interface FakeNonNullByDefault {

	DefaultLocation[] value() default { DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD,
			DefaultLocation.TYPE_BOUND, DefaultLocation.TYPE_ARGUMENT };

	enum DefaultLocation {

		PARAMETER, RETURN_TYPE, FIELD, TYPE_PARAMETER, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS

	}

}
