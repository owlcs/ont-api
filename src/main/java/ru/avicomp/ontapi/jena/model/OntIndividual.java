package ru.avicomp.ontapi.jena.model;

import java.util.stream.Stream;

/**
 * TODO:
 * for named and anonymous individuals
 * Created by @szuev on 02.11.2016.
 */
public interface OntIndividual extends OntObject {

    OntStatement attachClass(OntClass clazz);

    void detachClass(OntClass clazz);

    Stream<OntClass> classes();

    /**
     * Named Individual here.
     * <p>
     * Created by szuev on 01.11.2016.
     */
    interface Named extends OntIndividual, OntEntity {

    }

    /**
     * Anonymous Individual here.
     * <p>
     * Created by szuev on 10.11.2016.
     */
    interface Anonymous extends OntIndividual {
    }
}
