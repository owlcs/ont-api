package ru.avicomp.ontapi.tests;

import ru.avicomp.ontapi.io.OntFormat;

/**
 * GoodRelations.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class GoodRelationsTest extends BaseLoadTest {
    @Override
    public String getFileName() {
        return "goodrelations.rdf";
    }

    @Override
    public long getTotalNumberOfAxioms() {
        return 1141;
    }

    @Override
    public OntFormat convertFormat() {
        return OntFormat.XML_RDF;
    }
}
