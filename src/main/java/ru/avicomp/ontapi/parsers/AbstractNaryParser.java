package ru.avicomp.ontapi.parsers;

import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.IsAnonymous;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLNaryAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.OntException;

/**
 * for following axioms with two or more than two entities:
 * DisjointClasses ({@link DisjointClassesParser}),
 * DisjointObjectProperties ({@link DisjointObjectPropertiesParser}),
 * DisjointDataProperties ({@link DisjointDataPropertiesParser}),
 * DifferentIndividuals ({@link DifferentIndividualsParser})
 * <p>
 * Created by szuev on 12.10.2016.
 */
abstract class AbstractNaryParser<Axiom extends OWLAxiom & OWLNaryAxiom<? extends IsAnonymous>> extends AxiomParser<Axiom> {
    @Override
    public void process(Graph graph) {
        OWLNaryAxiom<? extends IsAnonymous> axiom = getAxiom();
        long count = axiom.operands().count();
        if (count < 2) throw new OntException("Should be at least two entities " + axiom);
        Model model = ModelFactory.createModelForGraph(graph);
        if (count == 2) { // classic way
            OWLObject entity = axiom.operands().filter(e -> !e.isAnonymous()).findFirst().orElse(null);
            if (entity == null)
                throw new OntException("Can't find a single non-anonymous class expression inside " + axiom);
            OWLObject rest = axiom.operands().filter((obj) -> !entity.equals(obj)).findFirst().orElse(null);
            Resource subject = AxiomParseUtils.toResource(entity);
            model.add(subject, getPredicate(), AxiomParseUtils.toResource(model, rest));
            AnnotationsParseUtils.translate(model, getAxiom());
        } else { // OWL2
            Resource root = model.createResource();
            model.add(root, RDF.type, getMembersType());
            Iterator<? extends RDFNode> iterator = AxiomParseUtils.toResourceIterator(model, axiom.operands());
            model.add(root, getMembersPredicate(), model.createList(iterator));
            AnnotationsParseUtils.translate(graph, root, getAxiom());
        }
    }

    public abstract Property getPredicate();

    public abstract Resource getMembersType();

    public abstract Property getMembersPredicate();
}
