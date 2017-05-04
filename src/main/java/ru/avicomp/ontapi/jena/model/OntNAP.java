package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;

/**
 * The (Named) Annotation Property resource.
 * <p>
 * Created by szuev on 01.11.2016.
 */
public interface OntNAP extends OntPE, OntEntity, Property {

    /**
     * Adds domain statement "A rdfs:domain U", where A is an annotation property, U is any IRI.
     *
     * @param domain uri-{@link Resource}
     * @return {@link OntStatement}
     * @throws ru.avicomp.ontapi.jena.OntJenaException in case anonymous resource.
     * @see #domain()
     * @see OntPE#removeDomain(Resource)
     */
    OntStatement addDomain(Resource domain);

    /**
     * Adds range statement "A rdfs:range U", where A is an annotation property, U is any IRI.
     *
     * @param range uri-{@link Resource}
     * @return {@link OntStatement}
     * @throws ru.avicomp.ontapi.jena.OntJenaException in case anonymous resource.
     * @see #range()
     * @see OntPE#removeRange(Resource)
     */
    OntStatement addRange(Resource range);

    /**
     * Returns domains.
     *
     * @return Stream of uri-{@link Resource}s
     */
    @Override
    Stream<Resource> domain();

    /**
     * Returns ranges.
     *
     * @return Stream of uri-{@link Resource}s
     */
    @Override
    Stream<Resource> range();

    /**
     * Returns all super properties, the pattern is "A1 rdfs:subPropertyOf A2"
     *
     * @return Stream of {@link OntNAP}s
     * @see #addSubPropertyOf(OntNAP)
     * @see OntPE#removeSubPropertyOf(Resource)
     */
    @Override
    default Stream<OntNAP> subPropertyOf() {
        return objects(RDFS.subPropertyOf, OntNAP.class);
    }

    /**
     * Adds super property.
     *
     * @param superProperty {@link OntNAP}
     * @return {@link OntStatement}
     */
    default OntStatement addSubPropertyOf(OntNAP superProperty) {
        return addStatement(RDFS.subPropertyOf, superProperty);
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
