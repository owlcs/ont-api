package ru.avicomp.ontapi.tests;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Model;
import org.apache.log4j.Logger;
import org.junit.Test;

import ru.avicomp.ontapi.jena.impl.GraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntClassEntity;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

/**
 * to test {@link ru.avicomp.ontapi.jena.impl.GraphModelImpl}
 * Created by szuev on 07.11.2016.
 */
public class GraphModelTest {
    private static final Logger LOGGER = Logger.getLogger(GraphModelTest.class);

    @Test
    public void testLoadCE() {
        LOGGER.info("load pizza");
        Model _m = ReadWriteUtils.load(ReadWriteUtils.getResourceURI("pizza.ttl"), null);
        GraphModelImpl m = new GraphModelImpl(_m.getGraph());

        LOGGER.info("Classes:");
        List<OntClassEntity> classes = m.listClasses().collect(Collectors.toList());
        classes.forEach(LOGGER::debug);
        LOGGER.info("Classes Count = " + classes.size());

        LOGGER.info("Class Expressions:");
        List<OntCE> ces = m.ontObjects(OntCE.class).collect(Collectors.toList());
        ces.forEach(LOGGER::debug);
        LOGGER.info("Class Expressions Count = " + ces.size());
    }


}
