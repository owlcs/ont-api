package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * TODO:
 * for named and anonymous individuals
 * Created by @szuev on 02.11.2016.
 */
public interface OntIndividual extends OntObject {

    void attachClass(OntClass clazz);

    void detachClass(OntClass clazz);

    Stream<OntClass> classes();
}
