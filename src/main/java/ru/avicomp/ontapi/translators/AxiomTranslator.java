package ru.avicomp.ontapi.translators;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * Base class for any Axiom Graph Translator (operator 'T').
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomTranslator<Axiom extends OWLAxiom> {

    public abstract void write(Axiom axiom, OntGraphModel model);

    Stream<OntStatement> statements(OntGraphModel model) {
        //todo:
        throw new OntApiException.Unsupported(getClass(), "statements");
    }

    Axiom create(OntStatement statement, Set<OWLAnnotation> annotations) {
        //todo:
        throw new OntApiException.Unsupported(getClass(), "create");
    }

    public Map<Axiom, Set<Triple>> read(OntGraphModel model) {
        return statements(model)
                .map(RDF2OWLHelper.StatementProcessor::new)
                .collect(Collectors.toMap(c -> create(c.getStatement(), c.getAnnotations()),
                        RDF2OWLHelper.StatementProcessor::getTriples));
    }
}
