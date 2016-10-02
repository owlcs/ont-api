package ru.avicomp.ontapi.external;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Additional Vocabulary for http://spinrdf.org/spinmap
 * <p>
 * Created by szuev on 05.05.2016.
 */
public class SPINMAP_SPIN {
    public static final String BASE_URI = "http://spinrdf.org/spinmap";

    public static final String NS = BASE_URI + "#";

    public static final String PREFIX = "spinmap";

    public static final Resource Conditional_Mapping_1 = ResourceFactory.createResource(NS + "Conditional-Mapping-1");
    public static final Resource Conditional_Mapping_1_1 = ResourceFactory.createResource(NS + "Conditional-Mapping-1-1");
}

