package ru.avicomp.ontapi.jena.model;

/**
 * TODO:
 * Created by @szuev on 02.11.2016.
 */
public interface OntSWRLAtom extends OntObject {
    enum Type {
        BUILT_IN, OWL_CLASS, DATA_PROPERTY, DATA_RANGE, DIFFERENT_INDIVIDUALS, OBJECT_PROPERTY, SAME_INDIVIDUALS,;
    }
}
