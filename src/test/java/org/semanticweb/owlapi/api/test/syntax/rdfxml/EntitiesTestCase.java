package org.semanticweb.owlapi.api.test.syntax.rdfxml;

import org.junit.Assert;
import org.junit.Test;
import org.semanticweb.owlapi.api.test.baseclasses.TestBase;
import org.semanticweb.owlapi.formats.RDFXMLDocumentFormat;
import org.semanticweb.owlapi.io.StringDocumentSource;
import org.semanticweb.owlapi.io.StringDocumentTarget;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.rdf.rdfxml.renderer.RDFXMLStorerFactory;

@ru.avicomp.ontapi.utils.ModifiedForONTApi
@SuppressWarnings("javadoc")
public class EntitiesTestCase extends TestBase {

    /**
     * ONT-API comment:
     * The test was modified to make it passed in ONT-API also.
     * The config option 'useNamespaceEntities' is only for RDF/XML format.
     * As for me it is incorrect to store this option in the global settings since it takes affect on a single format only, but whatever.
     * In the ONT-API there is a substitution of formats and instead OWL-Storer there is Jena mechanism,
     * so this option won't take affect and turns out to be unnecessary.
     * But we always can use direct way with pure OWL format...
     *
     * @throws Exception
     */
    @Test
    public void shouldRoundtripEntities() throws Exception {
        String input = "<?xml version=\"1.0\"?>\n"
                + "<!DOCTYPE rdf:RDF [<!ENTITY vin  \"http://www.w3.org/TR/2004/REC-owl-guide-20040210/wine#\" > ]>\n"
                + "<rdf:RDF"
                + " xmlns:owl =\"http://www.w3.org/2002/07/owl#\""
                + " xmlns:rdf =\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                + " xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\""
                + " xmlns:xsd =\"http://www.w3.org/2001/XMLSchema#\"> \n"
                + "<owl:Ontology rdf:about=\"\">"
                + "<owl:priorVersion rdf:resource=\"&vin;test\"/>"
                + "</owl:Ontology>"
                + "</rdf:RDF>";
        String base = "urn:test";
        IRI iri = IRI.create(base + "#", "test");

        OWLOntologyManager m = setupManager();

        StringDocumentSource source = new StringDocumentSource(input, iri, new RDFXMLDocumentFormat(), null);
        OWLOntology o = m.loadOntologyFromOntologyDocument(source);
        Assert.assertEquals("Wrong ontology IRI", base, o.getOntologyID().getOntologyIRI().map(IRI::getIRIString).orElse(null));
        OWLDocumentFormat format = o.getFormat();
        Assert.assertNotNull("No format", format);

        m.getOntologyConfigurator().withUseNamespaceEntities(true);
        Assert.assertTrue(m.getOntologyWriterConfiguration().isUseNamespaceEntities());
        //m.setOntologyWriterConfiguration(m.getOntologyWriterConfiguration().withUseNamespaceEntities(true));

        StringDocumentTarget target = new StringDocumentTarget();

        OWLStorerFactory store = new RDFXMLStorerFactory();
        store.createStorer().storeOntology(o, target, format);
        //o.getOWLOntologyManager().saveOntology(o, format, target);
        LOGGER.debug("As string:\n{}", target);
        Assert.assertTrue(target.toString().contains("<owl:priorVersion rdf:resource=\"&vin;test\"/>"));
    }
}
