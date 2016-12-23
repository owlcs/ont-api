package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDFS;

import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * Object Property Expression (i.e. for iri-object property entity and for inverseOf anonymous property expression)
 * <p>
 * Created by @szuev on 08.11.2016.
 */
public interface OntOPE extends OntPE {

    OntNPA.ObjectAssertion addNegativeAssertion(OntIndividual source, OntIndividual target);

    OntStatement addSuperPropertyOf(Stream<OntOPE> chain);

    void removeSuperPropertyOf();

    Stream<OntOPE> superPropertyOf();

    /**
     * anonymous triple "_:x owl:inverseOf PN"
     */
    interface Inverse extends OntOPE {
        OntOPE getDirect();
    }

    OntOPE getInverseOf();

    void setReflexive(boolean reflexive);

    void setIrreflexive(boolean irreflexive);

    void setSymmetric(boolean symmetric);

    void setAsymmetric(boolean asymmetric);

    void setTransitive(boolean transitive);

    void setFunctional(boolean functional);

    void setInverseFunctional(boolean inverseFunctional);

    default Stream<OntNPA.ObjectAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.ObjectAssertion.class).filter(a -> OntOPE.this.equals(a.getProperty()));
    }

    @Override
    default Stream<OntCE> domain() {
        return objects(RDFS.domain, OntCE.class);
    }

    default OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    @Override
    default Stream<OntCE> range() {
        return objects(RDFS.range, OntCE.class);
    }

    default OntStatement addRange(OntCE range) {
        return addStatement(RDFS.range, range);
    }

    default Stream<OntOPE> disjointWith() {
        return objects(OWL.propertyDisjointWith, OntOPE.class);
    }

    default OntStatement addDisjointWith(OntOPE other) {
        return addStatement(OWL.propertyDisjointWith, other);
    }

    default void removeDisjointWith(OntOPE other) {
        remove(OWL.propertyDisjointWith, other);
    }

    default Stream<OntOPE> equivalentProperty() {
        return objects(OWL.equivalentProperty, OntOPE.class);
    }

    default OntStatement addEquivalentProperty(OntOPE other) {
        return addStatement(OWL.equivalentProperty, other);
    }

    default void removeEquivalentProperty(OntOPE other) {
        remove(OWL.equivalentProperty, other);
    }

    default Stream<OntOPE> inverseOf() {
        return objects(OWL.inverseOf, OntOPE.class);
    }

    default OntStatement addInverseOf(OntOPE other) {
        return addStatement(OWL.inverseOf, other);
    }

    default void removeInverseOf(OntOPE other) {
        remove(OWL.inverseOf, other);
    }

    @Override
    default Stream<OntOPE> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntOPE.class);
    }

    default OntStatement addSubPropertyOf(OntOPE superProperty) {
        return addStatement(RDFS.subPropertyOf, superProperty);
    }

    default boolean isInverseFunctional() {
        return hasType(OWL.InverseFunctionalProperty);
    }

    default boolean isTransitive() {
        return hasType(OWL.TransitiveProperty);
    }

    default boolean isFunctional() {
        return hasType(OWL.FunctionalProperty);
    }

    default boolean isSymmetric() {
        return hasType(OWL.SymmetricProperty);
    }

    default boolean isAsymmetric() {
        return hasType(OWL.AsymmetricProperty);
    }

    default boolean isReflexive() {
        return hasType(OWL.ReflexiveProperty);
    }

    default boolean isIrreflexive() {
        return hasType(OWL.IrreflexiveProperty);
    }
}
