package ru.avicomp.ontapi;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.ontology.ProfileRegistry;
import org.apache.jena.rdf.model.ModelFactory;
import org.semanticweb.owlapi.model.OWLDataFactory;

import com.google.inject.Inject;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;

/**
 * TODO
 * Created by @szuev on 03.10.2016.
 */
public class OntManager extends OWLOntologyManagerImpl {
    private OntModelSpec spec;

    @Inject
    public OntManager(OWLDataFactory dataFactory, ReadWriteLock readWriteLock) {
        super(dataFactory, readWriteLock);
        OntDocumentManager documentManager = new OntDocumentManager();
        documentManager.setProcessImports(false);
        this.spec = new OntModelSpec(ModelFactory.createMemModelMaker(), documentManager, null, ProfileRegistry.OWL_DL_LANG);
    }

    public OntModelSpec getSpec() {
        return spec;
    }
}
