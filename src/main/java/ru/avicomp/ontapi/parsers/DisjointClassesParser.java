package ru.avicomp.ontapi.parsers;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntException;

/**
 * example:
 * :Complex2 owl:disjointWith  :Simple2 , :Simple1 .
 * OWL2 alternative way:
 * [ a owl:AllDisjointClasses ; owl:members ( :Complex2 :Simple1 :Simple2 ) ] .
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class DisjointClassesParser extends AxiomParser<OWLDisjointClassesAxiom> {

    @Override
    public void process(Graph graph) {
        OWLDisjointClassesAxiom axiom = getAxiom();
        long count = axiom.classExpressions().count();
        if (count < 2) throw new OntException("Should be at least two classes " + axiom);
        Model model = ModelFactory.createModelForGraph(graph);
        if (count == 2) { // classic way (owl:disjointWith)
            OWLClass clazz = (OWLClass) axiom.classExpressions().filter(e -> !e.isAnonymous()).findFirst().orElse(null);
            if (clazz == null)
                throw new OntException("Can't find a single non-anonymous class expression inside " + axiom);
            OWLClassExpression rest = axiom.classExpressions().filter((obj) -> !clazz.equals(obj)).findFirst().orElse(null);
            Resource subject = ParseUtils.toResource(clazz.getIRI());
            model.add(subject, OWL.disjointWith, ParseUtils.toResource(model, rest));
        } else { // OWL2 (owl:AllDisjointClasses)
            Resource root = model.createResource();
            model.add(root, RDF.type, OWL2.AllDisjointClasses);
            Iterator<? extends RDFNode> iterator = ParseUtils.toResourceIterator(model, axiom.classExpressions());
            model.add(root, OWL2.members, model.createList(iterator));
        }
    }

    @Override
    public void reverse(Graph graph) {
        OWLDisjointClassesAxiom axiom = getAxiom();
        long count = axiom.classExpressions().count();
        if (count < 2) throw new OntException("Should be at least two classes " + axiom);
        Model model = ModelFactory.createModelForGraph(graph);
        // remove owl:disjointWith
        axiom.asPairwiseAxioms().forEach(a -> {
            OWLClass clazz = (OWLClass) a.classExpressions().filter(e -> !e.isAnonymous()).findFirst().orElse(null);
            if (clazz == null) return;
            Resource subject = ParseUtils.toResource(clazz.getIRI());
            model.removeAll(subject, OWL.disjointWith, null);
        });

        // remove owl:AllDisjointClasses
        List<String> uris = axiom.classExpressions().filter(e -> !e.isAnonymous()).map(HasIRI.class::cast).map(HasIRI::getIRI).map(IRI::getIRIString).sorted().collect(Collectors.toList());
        if (uris.isEmpty()) throw new OntException("Something wrong. No named classes inside axiom, should not happen");
        List<Resource> roots = model.listStatements(null, RDF.type, OWL2.AllDisjointClasses).mapWith(Statement::getSubject).toList();
        for (Resource root : roots) {
            List<RDFList> lists = model.listObjectsOfProperty(root, OWL2.members).mapWith(n -> n.as(RDFList.class)).toList();
            if (lists.isEmpty()) { // just in case. wrong situation, clear
                model.removeAll(root, null, null);
                continue;
            }
            if (lists.size() != 1) throw new OntException("Something wrong. Should be single owl:member node.");
            RDFList list = lists.get(0);
            List<String> members = list.asJavaList().stream().filter(RDFNode::isURIResource).map(n -> n.asResource().getURI()).sorted().collect(Collectors.toList());
            // if specified list is contained in rdf-list - delete all.
            if (members.containsAll(uris)) {
                list.removeList();
                model.removeAll(root, null, null);
            }
        }
    }
}
