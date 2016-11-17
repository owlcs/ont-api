package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDFS;

/**
 * (Named) Datatype Property here.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNDP extends OntPE, OntEntity, Property {

    OntNPA.DataAssertion addNegativeAssertion(OntIndividual source, Literal target);

    void setFunctional(boolean functional);

    boolean isFunctional();

    @Override
    default Stream<OntCE> domain() {
        return objects(RDFS.domain, OntCE.class);
    }

    default OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    @Override
    default Stream<OntDR> range() {
        return objects(RDFS.range, OntDR.class);
    }

    default OntStatement addRange(OntDR range) {
        return addStatement(RDFS.range, range);
    }

    @Override
    default Stream<OntNDP> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntNDP.class);
    }

    default OntStatement addSubPropertyOf(OntNDP superProperty) {
        return addStatement(RDFS.subPropertyOf, superProperty);
    }

    default Stream<OntNDP> disjointWith() {
        return objects(OWL2.propertyDisjointWith, OntNDP.class);
    }

    default OntStatement addDisjointWith(OntNDP other) {
        return addStatement(OWL2.propertyDisjointWith, other);
    }

    default void removeDisjointWith(OntNDP other) {
        remove(OWL2.propertyDisjointWith, other);
    }

    default Stream<OntNDP> equivalentProperty() {
        return objects(OWL2.equivalentProperty, OntNDP.class);
    }

    default OntStatement addEquivalentProperty(OntNDP other) {
        return addStatement(OWL2.equivalentProperty, other);
    }

    default void removeEquivalentProperty(OntNDP other) {
        remove(OWL2.equivalentProperty, other);
    }

    default Stream<OntNPA.DataAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.DataAssertion.class).filter(a -> OntNDP.this.equals(a.getProperty()));
    }

    @Override
    default boolean isProperty() {
        return true;
    }

    @Override
    default int getOrdinal() {
        return as(Property.class).getOrdinal();
    }
}
