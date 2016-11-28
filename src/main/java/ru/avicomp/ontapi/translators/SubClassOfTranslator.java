package ru.avicomp.ontapi.translators;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;
import uk.ac.manchester.cs.owl.owlapi.OWLSubClassOfAxiomImpl;

/**
 * Examples:
 * pizza:JalapenoPepperTopping rdfs:subClassOf pizza:PepperTopping.
 * pizza:JalapenoPepperTopping rdfs:subClassOf [ a owl:Restriction; owl:onProperty pizza:hasSpiciness; owl:someValuesFrom pizza:Hot].
 * <p>
 * Created by @szuev on 28.09.2016.
 */
class SubClassOfTranslator extends AxiomTranslator<OWLSubClassOfAxiom> {
    @Override
    public void write(OWLSubClassOfAxiom axiom, OntGraphModel model) {
        OWL2RDFHelper.writeTriple(model, axiom.getSubClass(), RDFS.subClassOf, axiom.getSuperClass(), axiom.annotations());
    }

    @Override
    public Set<OWLTripleSet<OWLSubClassOfAxiom>> read(OntGraphModel model) {
        Stream<OntStatement> statements = model.ontObjects(OntCE.class)
                .map(subj -> subj.subClassOf().map(obj -> subj.getStatement(RDFS.subClassOf, obj)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
        Set<OWLTripleSet<OWLSubClassOfAxiom>> res = new HashSet<>();
        statements.forEach(s -> {
            RDF2OWLHelper.StatementContent content = new RDF2OWLHelper.StatementContent(s);
            OWLClassExpression sub = RDF2OWLHelper.getClassExpression(s.getSubject().as(OntCE.class));
            OWLClassExpression sup = RDF2OWLHelper.getClassExpression(s.getObject().as(OntCE.class));
            OWLSubClassOfAxiom axiom = new OWLSubClassOfAxiomImpl(sub, sup, content.getAnnotations());
            res.add(wrap(axiom, content.getTriples()));
        });
        return res;
    }
}
