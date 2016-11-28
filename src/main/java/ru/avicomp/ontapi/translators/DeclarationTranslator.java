package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLDeclarationAxiomImpl;

/**
 * simple triplet with rdf:type predicate.
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DeclarationTranslator extends AbstractSingleTripleTranslator<OWLDeclarationAxiom> {
    @Override
    public OWLEntity getSubject(OWLDeclarationAxiom axiom) {
        return axiom.getEntity();
    }

    @Override
    public Property getPredicate() {
        return RDF.type;
    }

    @Override
    public RDFNode getObject(OWLDeclarationAxiom axiom) {
        return OWL2RDFHelper.getType(getSubject(axiom));
    }

    @Override
    Map<OWLObject, OntStatement> find(OntGraphModel model) {
        return model.ontEntities().filter(OntObject::isLocal).collect(Collectors.toMap(RDF2OWLHelper::getEntity, OntObject::getRoot));
    }

    @Override
    OWLDeclarationAxiom create(OWLObject object, Set<OWLAnnotation> annotations) {
        return new OWLDeclarationAxiomImpl((OWLEntity) object, annotations);
    }
}
