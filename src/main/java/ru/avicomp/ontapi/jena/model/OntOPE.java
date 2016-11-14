package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * Object Property Expression (i.e. for iri-object property entity and for inverseOf anonymous property expression)
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public interface OntOPE extends OntPE {

    OntStatement addDomain(OntCE domain);

    OntStatement addRange(OntCE range);

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
}
