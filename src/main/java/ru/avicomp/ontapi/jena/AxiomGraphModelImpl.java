package ru.avicomp.ontapi.jena;

import org.apache.jena.graph.Graph;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntologyID;

import ru.avicomp.ontapi.jena.impl.GraphModelImpl;
import ru.avicomp.ontapi.translators.rdf2axiom.GraphParseHelper;

/**
 * New strategy here.
 * TODO: Now there's nothing here
 * TODO: This is GraphModel with methods to work with the axioms. It combines jena(Graph) model and owl(OWLAxiom).
 * TODO: will be used to load and write from {@link ru.avicomp.ontapi.OntologyModel}.
 *
 * Created by @szuev on 26.10.2016.
 */
public class AxiomGraphModelImpl extends GraphModelImpl {

    public AxiomGraphModelImpl(Graph base) {
        super(base);
    }

    public OWLOntologyID getID() {
        return GraphParseHelper.getOWLOntologyID(graph);
    }

    public ExtendedIterator<OWLEntity> entities() {
        return GraphParseHelper.entities(graph);
    }

}
