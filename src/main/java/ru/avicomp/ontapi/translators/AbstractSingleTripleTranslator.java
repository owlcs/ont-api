package ru.avicomp.ontapi.translators;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLObject;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * Base class for parse axiom which is related to single triplet.
 * sub-classes:
 *  {@link DeclarationTranslator},
 *  {@link FunctionalDataPropertyTranslator},
 *  {@link FunctionalObjectPropertyTranslator},
 *  {@link ReflexiveObjectPropertyTranslator},
 *  {@link IrreflexiveObjectPropertyTranslator},
 *  {@link AsymmetricObjectPropertyTranslator},
 *  {@link SymmetricObjectPropertyTranslator},
 *  {@link TransitiveObjectPropertyTranslator},
 *  {@link InverseFunctionalObjectPropertyTranslator},
 * <p>
 * Created by @szuev on 28.09.2016.
 */
abstract class AbstractSingleTripleTranslator<Axiom extends OWLAxiom> extends AxiomTranslator<Axiom> {

    public abstract OWLObject getSubject(Axiom axiom);

    public abstract Property getPredicate();

    public abstract RDFNode getObject(Axiom axiom);

    @Override
    public void write(Axiom axiom, OntGraphModel graph) {
        OWL2RDFHelper.writeTriple(graph, getSubject(axiom), getPredicate(), getObject(axiom), axiom.annotations(), true);
    }

    @Override
    public Set<OWLTripleSet<Axiom>> read(OntGraphModel model) {
        Map<OWLObject, OntStatement> map = find(model);
        Set<OWLTripleSet<Axiom>> res = new HashSet<>();
        for (OWLObject key : map.keySet()) {
            Set<Triple> triples = new HashSet<>();
            triples.add(map.get(key).asTriple());
            Set<OWLAnnotation> annotations = new HashSet<>();
            RDF2OWLHelper.getBulkAnnotations(map.get(key)).forEach(a -> {
                triples.addAll(a.getTriples());
                annotations.add(a.getObject());
            });
            res.add(createAndWrap(key, annotations, triples));
        }
        return res;
    }

    abstract Map<OWLObject, OntStatement> find(OntGraphModel model);

    abstract Axiom create(OWLObject object, Set<OWLAnnotation> annotations);

    private OWLTripleSet<Axiom> createAndWrap(OWLObject object, Set<OWLAnnotation> annotations, Set<Triple> triples) {
        return new OWLTripleSet<>(create(object, annotations), triples);
    }
}
