package ru.avicomp.ontapi;

import org.semanticweb.owlapi.model.OWLOntologyBuilder;
import org.semanticweb.owlapi.model.OWLOntologyFactory;

import com.google.inject.Inject;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyFactoryImpl;

/**
 * todo:
 * see {@link OWLOntologyFactory}
 * <p>
 * Created by szuev on 24.10.2016.
 */
public class OntologyFactoryImpl extends OWLOntologyFactoryImpl implements OWLOntologyFactory {
    /**
     * @param ontologyBuilder ontology builder
     */
    @Inject
    public OntologyFactoryImpl(OWLOntologyBuilder ontologyBuilder) {
        super(ontologyBuilder);
    }
}
