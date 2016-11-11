package ru.avicomp.ontapi.jena.model;

/**
 * Ontology ID.
 * <p>
 * Created by szuev on 09.11.2016.
 */
public interface OntID extends OntObject {

    String getVersionIRI();

    void setVersionIRI(String uri);

}
