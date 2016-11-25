package ru.avicomp.ontapi.translators;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;

import ru.avicomp.ontapi.jena.model.OntEntity;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
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
    public Stream<OWLDeclarationAxiom> read(OntGraphModel model) {
        List<OntEntity> entities = model.ontEntities().filter(OntEntity::isLocal).collect(Collectors.toList());
        return entities.stream().map(e -> new OWLDeclarationAxiomImpl(RDF2OWLHelper.getEntity(e), RDF2OWLHelper.getBulkAnnotations(e)));
    }
}
