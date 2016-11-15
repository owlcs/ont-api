package ru.avicomp.ontapi.jena;

import org.apache.jena.graph.Graph;

import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;

/**
 * New strategy here.
 * TODO: Now there's nothing here
 * TODO: This is GraphModel with methods to work with the axioms. It combines jena(Graph) model and owl(OWLAxiom).
 * TODO: will be used to load and write from {@link ru.avicomp.ontapi.OntologyModel}.
 *
 * Created by @szuev on 26.10.2016.
 */
public class AxiomGraphModelImpl extends OntGraphModelImpl {

    public AxiomGraphModelImpl(Graph base) {
        super(base);
    }

}
