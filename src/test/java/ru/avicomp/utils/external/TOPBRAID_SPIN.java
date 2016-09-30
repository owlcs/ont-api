package ru.avicomp.utils.external;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary of the Topbraid SPINMAPL library.
 * Some of the functions may be commercial.
 * <p>
 * Do we really need this library?
 * <p>
 * Created by @szuev on 03.04.2016.
 */
public class TOPBRAID_SPIN {
    public static final String SMF_URI = "http://topbraid.org/functions-smf";
    public static final String AFN_URI = "http://topbraid.org/functions-afn";
    public static final String FN_URI = "http://topbraid.org/functions-fn";
    public static final String FN_NS = "http://www.w3.org/2005/xpath-functions#";
    public static final String SMF_BASE = "http://topbraid.org/sparqlmotionfunctions";

    public static final String OWL_RL_PROPERTY_CHAIN_HELPER = "http://topbraid.org/spin/owlrl#propertyChainHelper";

    public static final String BASE_URI = "http://topbraid.org/spin/spinmapl";
    public static final String NS = BASE_URI + "#";
    public static final String PREFIX = "spinmapl";

    // functions:
    public static final Resource self = ResourceFactory.createResource(NS + "self");
    public static final Resource concatWithSeparator = ResourceFactory.createResource(NS + "concatWithSeparator");
    public static final Resource buildURI1 = ResourceFactory.createResource(NS + "buildURI1");
    public static final Resource buildURI2 = ResourceFactory.createResource(NS + "buildURI2");
    public static final Resource buildURI3 = ResourceFactory.createResource(NS + "buildURI3");
    public static final Resource buildURI4 = ResourceFactory.createResource(NS + "buildURI4");
    public static final Resource buildURI5 = ResourceFactory.createResource(NS + "buildURI5");
    public static final Resource relatedSubjectContext = ResourceFactory.createResource(NS + "relatedSubjectContext");
    public static final Resource relatedObjectContext = ResourceFactory.createResource(NS + "relatedObjectContext");
    public static final Resource changeNamespace = ResourceFactory.createResource(NS + "changeNamespace");
    public static final Resource composeURI = ResourceFactory.createResource(NS + "composeURI");
    public static final Resource concat = ResourceFactory.createResource(FN_NS + "concat");

    // properties:
    public static final Property separator = ResourceFactory.createProperty(NS + "separator");
    public static final Property template = ResourceFactory.createProperty(NS + "template");
    public static final Property context = ResourceFactory.createProperty(NS + "context");
    public static final Property predicate = ResourceFactory.createProperty(NS + "predicate");
    public static final Property targetNamespace = ResourceFactory.createProperty(NS + "targetNamespace");
}
