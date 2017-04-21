package ru.avicomp.ontapi.utils;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * The vocabulary for <a href='http://topbraid.org/spin/spinmapl'>topbraid spinmapl</a>
 * <p>
 * Created by szuev on 21.04.2017.
 */
public class SPINMAPL {
    public final static String URI = "http://topbraid.org/spin/spinmapl";
    public final static String NS = URI + "#";

    public static final Resource self = resource("self");
    public static final Resource concatWithSeparator = resource("concatWithSeparator");
    public static final Resource changeNamespace = resource("changeNamespace");
    public static final Resource composeURI = resource("composeURI");
    public static final Property separator = property("separator");
    public static final Property template = property("template");
    public static final Property targetNamespace = property("targetNamespace");

    protected static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }
}
