package ru.avicomp.ontapi;

import org.apache.jena.graph.Graph;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyID;

import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntID;

/**
 * New strategy here.
 * Buffer RDF-OWL model.
 * TODO: Now there's nothing here
 * TODO: This is {@link OntGraphModel} with methods to work with the axioms. It combines jena(RDF Graph) and owl(structural, OWLAxiom) ways.
 * TODO: will be used to load and write from {@link ru.avicomp.ontapi.OntologyModel}.
 * <p>
 * Created by @szuev on 26.10.2016.
 */
public class OntInternalModel extends OntGraphModelImpl implements OntGraphModel {

    private final OWLOntologyID anonOntologyID = new OWLOntologyID();

    private final OntGraphEventStore eventStore;

    public OntInternalModel(Graph base) {
        super(base);
        this.eventStore = new OntGraphEventStore();
    }

    public OntGraphEventStore getEventStore() {
        return eventStore;
    }

    public OWLOntologyID getOwlID() {
        OntID id = getID();
        if (id.isAnon()) return anonOntologyID;
        IRI iri = IRI.create(id.getURI());
        IRI versionIRI = null;
        String ver = id.getVersionIRI();
        if (ver != null) {
            versionIRI = IRI.create(ver);
        }
        return new OWLOntologyID(iri, versionIRI);
    }

    public void setOwlID(OWLOntologyID id) {
        if (id.isAnonymous()) {
            setID(null).setVersionIRI(null);
            return;
        }
        IRI iri = id.getOntologyIRI().orElse(null);
        IRI versionIRI = id.getVersionIRI().orElse(null);
        setID(iri == null ? null : iri.getIRIString()).setVersionIRI(versionIRI == null ? null : versionIRI.getIRIString());
    }
}
