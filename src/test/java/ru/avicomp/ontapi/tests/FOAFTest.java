package ru.avicomp.ontapi.tests;

import ru.avicomp.ontapi.io.OntFormat;

/**
 * Foaf.
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class FOAFTest extends BaseLoadTest {
    @Override
    public String getFileName() {
        return "foaf.rdf";
    }

    @Override
    public long getTotalNumberOfAxioms() {
        return 551;
    }

    @Override
    public OntFormat convertFormat() {
        return OntFormat.XML_RDF;
    }
}
