package ru.avicomp.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary definition for the
 * <a href='https://www.w3.org/Submission/SWRL/'>SWRL: A Semantic Web Rule Language Combining OWL and RuleML</a>.
 * See <a href='https://www.w3.org/Submission/SWRL/swrl.rdf'>schema</a>
 * See also {@link org.semanticweb.owlapi.vocab.SWRLVocabulary}
 * <p>
 * Created by szuev on 20.10.2016.
 */
public class SWRL {
    public final static String URI = "http://www.w3.org/2003/11/swrl";
    public final static String NS = URI + "#";

    public static final Resource Imp = resource("Imp");
    public static final Resource IndividualPropertyAtom = resource("IndividualPropertyAtom");
    public static final Resource DatavaluedPropertyAtom = resource("DatavaluedPropertyAtom");
    public static final Resource ClassAtom = resource("ClassAtom");
    public static final Resource DataRangeAtom = resource("DataRangeAtom");
    public static final Resource Variable = resource("Variable");
    public static final Resource AtomList = resource("AtomList");
    public static final Resource SameIndividualAtom = resource("SameIndividualAtom");
    public static final Resource DifferentIndividualsAtom = resource("DifferentIndividualsAtom");
    public static final Resource BuiltinAtom = resource("BuiltinAtom");
    public static final Resource Builtin = resource("Builtin");

    public static final Property head = property("head");
    public static final Property body = property("body");
    public static final Property classPredicate = property("classPredicate");
    public static final Property dataRange = property("dataRange");
    public static final Property propertyPredicate = property("propertyPredicate");
    public static final Property builtin = property("builtin");
    public static final Property arguments = property("arguments");
    public static final Property argument1 = property("argument1");
    public static final Property argument2 = property("argument2");

    protected static Resource resource(String local) {
        return ResourceFactory.createResource(NS + local);
    }

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }
}
