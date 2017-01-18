package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * Ontology ID.
 * Every {@link OntGraphModel} must have only the one {@link OntID} inside.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public interface OntID extends OntObject {

    String getVersionIRI();

    void setVersionIRI(String uri);

    void addImport(String uri);

    void removeImport(String uri);

    Stream<String> imports();

}
