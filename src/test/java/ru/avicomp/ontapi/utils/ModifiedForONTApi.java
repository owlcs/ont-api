package ru.avicomp.ontapi.utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * To mark the changed OWL-API-test classes (from owlapi-contract, see package {@link org.semanticweb}).
 * <p>
 * Created by szuev on 17.01.2017.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModifiedForONTApi {
}
