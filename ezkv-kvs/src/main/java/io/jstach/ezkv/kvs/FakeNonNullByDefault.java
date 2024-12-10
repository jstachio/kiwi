package io.jstach.ezkv.kvs;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.CLASS)
@interface FakeNonNullByDefault {

	DefaultLocation[] value() default { DefaultLocation.PARAMETER, DefaultLocation.RETURN_TYPE, DefaultLocation.FIELD,
			DefaultLocation.TYPE_BOUND, DefaultLocation.TYPE_ARGUMENT };

	enum DefaultLocation {

		PARAMETER, RETURN_TYPE, FIELD, TYPE_PARAMETER, TYPE_BOUND, TYPE_ARGUMENT, ARRAY_CONTENTS

	}

}
