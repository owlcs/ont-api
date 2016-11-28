package ru.avicomp.ontapi.translators;

import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.HasProperty;
import org.semanticweb.owlapi.model.HasRange;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntPE;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * base class for {@link DataPropertyRangeTranslator} and {@link ObjectPropertyRangeTranslator} and {@link AnnotationPropertyRangeTranslator}
 * example: foaf:name rdfs:range rdfs:Literal
 * <p>
 * Created by @szuev on 30.09.2016.
 */
abstract class AbstractPropertyRangeTranslator<Axiom extends OWLAxiom & HasProperty & HasRange> extends AxiomTranslator<Axiom> {
    @Override
    public void write(Axiom axiom, OntGraphModel graph) {
        OWL2RDFHelper.writeTriple(graph, axiom.getProperty(), RDFS.range, axiom.getRange(), axiom.annotations());
    }

    Stream<OntStatement> statements(OntGraphModel model, Class<? extends OntPE> view) {
        return model.ontObjects(view)
                .map(p -> p.range().map(r -> p.getStatement(RDFS.range, r)))
                .flatMap(Function.identity())
                .filter(OntStatement::isLocal);
    }

    abstract Stream<OntStatement> statements(OntGraphModel model);

    abstract Axiom create(OntPE property, Resource range, Set<OWLAnnotation> annotations);

    @Override
    public Set<OWLTripleSet<Axiom>> read(OntGraphModel model) {
        return statements(model).map(s -> {
            RDF2OWLHelper.StatementContent content = new RDF2OWLHelper.StatementContent(s);
            return wrap(create(s.getSubject().as(OntPE.class), s.getObject().asResource(), content.getAnnotations()), content.getTriples());
        }).collect(Collectors.toSet());
    }
}
