package ru.avicomp.ontapi.jena.model;

/**
 * OWL-Entity here.
 * It is always uri-resource
 * TODO:
 * Created by szuev on 01.11.2016.
 */
public interface OntEntity extends OntObject {

    /**
     * Determines is entity-resource local defined.
     *
     * @return true if this resource is local to the base graph.
     */
    boolean isLocal();

}
