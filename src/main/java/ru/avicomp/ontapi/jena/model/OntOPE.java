package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

/**
 * Object Property Expression (i.e. for iri-object property entity and for inverseOf anonymous property expression)
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public interface OntOPE extends OntPE {

    OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target);

    @Override
    Stream<OntCE> domain();

    @Override
    Stream<OntCE> range();

    /**
     * anonymous triple "_:x owl:inverseOf PN"
     */
    interface Inverse extends OntOPE {

    }

    void addInverseOf(OntOPE other);

    void removeInverseOf(OntOPE other);

    OntOPE getInverseOf();

    void setReflexive(boolean reflexive);

    boolean isReflexive();

    void setIrreflexive(boolean irreflexive);

    boolean isIrreflexive();

    void setSymmetric(boolean symmetric);

    boolean isSymmetric();

    void setAsymmetric(boolean asymmetric);

    boolean isAsymmetric();

    void setTransitive(boolean transitive);

    boolean isTransitive();

    void setFunctional(boolean functional);

    boolean isFunctional();

    void setInverseFunctional(boolean inverseFunctional);

    boolean isInverseFunctional();

    default OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    default OntStatement addRange(OntCE range) {
        return addStatement(RDFS.range, range);
    }

    default OntStatement addDisjointWith(OntOPE other) {
        return addStatement(OWL2.propertyDisjointWith, other);
    }

    default void removeDisjointWith(OntOPE other) {
        remove(OWL2.propertyDisjointWith, other);
    }

    default Stream<OntNPA.ObjectAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.ObjectAssertion.class).filter(a -> OntOPE.this.equals(a.getProperty()));
    }

    default void removeNegativeAssertion(OntNPA.ObjectAssertion assertion) {
        getModel().removeAll(assertion, null, null);
    }
}
