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

    OntStatement addSubPropertiesOf(Stream<OntOPE> chain);

    void removeSubPropertiesOf();

    Stream<OntOPE> subPropertiesOf();

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
        return objects(OWL2.propertyDisjointWith, OntOPE.class);
    }

    default OntStatement addDisjointWith(OntOPE other) {
        return addStatement(OWL2.propertyDisjointWith, other);
    }

    default void removeDisjointWith(OntOPE other) {
        remove(OWL2.propertyDisjointWith, other);
    }

    default Stream<OntOPE> equivalentProperty() {
        return objects(OWL2.equivalentProperty, OntOPE.class);
    }

    default OntStatement addEquivalentProperty(OntOPE other) {
        return addStatement(OWL2.equivalentProperty, other);
    }

    default void removeEquivalentProperty(OntOPE other) {
        remove(OWL2.equivalentProperty, other);
    }

    default Stream<OntOPE> inverseOf() {
        return objects(OWL2.inverseOf, OntOPE.class);
    }

    default OntStatement addInverseOf(OntOPE other) {
        return addStatement(OWL2.inverseOf, other);
    }

    default void removeInverseOf(OntOPE other) {
        remove(OWL2.inverseOf, other);
    }

    @Override
    default Stream<OntOPE> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntOPE.class);
    }

    default OntStatement addSubPropertyOf(OntOPE superProperty) {
        return addStatement(RDFS.subPropertyOf, superProperty);
    }

    default boolean isInverseFunctional() {
        return hasType(OWL2.InverseFunctionalProperty);
    }

    default boolean isTransitive() {
        return hasType(OWL2.TransitiveProperty);
    }

    default boolean isFunctional() {
        return hasType(OWL2.FunctionalProperty);
    }

    default boolean isSymmetric() {
        return hasType(OWL2.SymmetricProperty);
    }

    default boolean isAsymmetric() {
        return hasType(OWL2.AsymmetricProperty);
    }

    default boolean isReflexive() {
        return hasType(OWL2.ReflexiveProperty);
    }

    default boolean isIrreflexive() {
        return hasType(OWL2.IrreflexiveProperty);
    }
}
