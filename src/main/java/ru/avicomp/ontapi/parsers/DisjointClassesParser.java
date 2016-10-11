package ru.avicomp.ontapi.parsers;

import java.util.Iterator;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.OWL2;
import org.apache.jena.vocabulary.RDF;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;

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
    public void translate(Graph graph) {
        OWLDisjointClassesAxiom axiom = getAxiom();
        long count = axiom.classExpressions().count();
        if (count < 2) throw new OntException("Should be at least two classes " + axiom);
        Model model = ModelFactory.createModelForGraph(graph);
        if (count == 2) { // classic way (owl:disjointWith)
            OWLClass clazz = (OWLClass) axiom.classExpressions().filter(e -> !e.isAnonymous()).findFirst().orElse(null);
            if (clazz == null)
                throw new OntException("Can't find a single non-anonymous class expression inside " + axiom);
            OWLClassExpression rest = axiom.classExpressions().filter((obj) -> !clazz.equals(obj)).findFirst().orElse(null);
            Resource subject = AxiomParseUtils.toResource(clazz.getIRI());
            model.add(subject, OWL.disjointWith, AxiomParseUtils.toResource(model, rest));
        } else { // OWL2 (owl:AllDisjointClasses)
            Resource root = model.createResource();
            model.add(root, RDF.type, OWL2.AllDisjointClasses);
            Iterator<? extends RDFNode> iterator = AxiomParseUtils.toResourceIterator(model, axiom.classExpressions());
            model.add(root, OWL2.members, model.createList(iterator));
        }
    }

}
