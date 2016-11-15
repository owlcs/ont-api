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

    @Override
    Stream<OntCE> domain();

    @Override
    Stream<OntDR> range();

    void setFunctional(boolean functional);

    boolean isFunctional();

    default OntStatement addDomain(OntCE domain) {
        return addStatement(RDFS.domain, domain);
    }

    default OntStatement addRange(OntDR range) {
        return addStatement(RDFS.range, range);
    }

    default OntStatement addDisjointWith(OntNDP other) {
        return addStatement(OWL2.propertyDisjointWith, other);
    }

    default void removeDisjointWith(OntNDP other) {
        remove(OWL2.propertyDisjointWith, other);
    }

    default Stream<OntNPA.DataAssertion> negativeAssertions() {
        return getModel().ontObjects(OntNPA.DataAssertion.class).filter(a -> OntNDP.this.equals(a.getProperty()));
    }

    default void removeNegativeAssertion(OntNPA.DataAssertion assertion) {
        getModel().removeAll(assertion, null, null);
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
