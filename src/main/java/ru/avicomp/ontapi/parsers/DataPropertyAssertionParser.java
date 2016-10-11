package ru.avicomp.ontapi.parsers;

import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;

/**
 * property that belongs to individual.
 * individual could be anonymous!
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DataPropertyAssertionParser extends AbstractPropertyAssertionParser<OWLDataPropertyAssertionAxiom> {
    @Override
    public RDFNode getObject() {
        return AxiomParseUtils.toLiteral(getAxiom().getObject());
    }
}
