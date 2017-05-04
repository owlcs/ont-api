package ru.avicomp.ontapi.jena.model;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Negative property assertion. For {@link OntNDP} and {@link OntOPE} properties.
 * Negative Object Property Assertion example:
 * _:x rdf:type owl:NegativePropertyAssertion ; _:x owl:sourceIndividual a1; _:x owl:assertionProperty P; _:x owl:targetIndividual a2.
 * Negative Data Property Assertion example:
 * _:x rdf:type owl:NegativePropertyAssertion; _:x owl:sourceIndividual a; _:x owl:assertionProperty R;  _:x owl:targetValue v.
 * <p>
 * Created by @szuev on 15.11.2016.
 */
public interface OntNPA<P extends OntPE, T extends RDFNode> extends OntObject {

    /**
     * Returns the source individual.
     *
     * @return {@link OntIndividual}
     */
    OntIndividual getSource();

    /**
     * Returns the assertion property
     *
     * @return either {@link OntOPE} or {@link OntNDP}
     */
    P getProperty();

    /**
     * Returns the target
     *
     * @return either {@link OntIndividual} or {@link Literal}
     */
    T getTarget();

    interface ObjectAssertion extends OntNPA<OntOPE, OntIndividual> {
    }

    interface DataAssertion extends OntNPA<OntNDP, Literal> {
    }
}
