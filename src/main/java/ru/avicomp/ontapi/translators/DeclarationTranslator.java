package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;

/**
 * Declaration of OWLEntity.
 * Simple triplet with rdf:type predicate.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DeclarationTranslator extends AxiomTranslator<OWLDeclarationAxiom> {
    @Override
    public void write(OWLDeclarationAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getEntity(), RDF.type, OWL2RDFHelper.getType(axiom.getEntity()), axiom.annotations(), false);
    }

    @Override
    Stream<OntStatement> statements(OntGraphModel model) {
        return model.ontEntities().filter(OntObject::isLocal).map(OntObject::getRoot);
    }

    @Override
    OWLDeclarationAxiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        OWLEntity entity = RDF2OWLHelper.getEntity(statement.getSubject().as(OntEntity.class));
        return new OWLDeclarationAxiomImpl(entity, annotations);
    }
}
