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
    private AxiomParserProvider.Config config = AxiomParserProvider.DEFAULT_CONFIG;

    public abstract void write(Axiom axiom, OntGraphModel model);

    abstract Stream<OntStatement> statements(OntGraphModel model);

    abstract Axiom create(OntStatement statement, Set<OWLAnnotation> annotations);

    private Stream<RDF2OWLHelper.AxiomStatement> axiomStatements(OntGraphModel model) {
        return statements(model).map(RDF2OWLHelper.AxiomStatement::new);
    }

    public Map<Axiom, Set<Triple>> read(OntGraphModel model) {
        try {
            return axiomStatements(model).collect(Collectors.toMap(c -> create(c.getStatement(), c.getAnnotations()),
                    RDF2OWLHelper.AxiomStatement::getTriples,
                    (tripleSet1, tripleSet2) -> {
                        tripleSet1.addAll(tripleSet2);
                        return tripleSet1;
                    }));
        } catch (Exception e) {
            throw new OntApiException(String.format("Can't process reading. Translator <%s>.", getClass()), e);
        }
    }

    public AxiomParserProvider.Config getConfig() {
        return config;
    }

    public void setConfig(AxiomParserProvider.Config config) {
        this.config = OntApiException.notNull(config, "Null config.");
    }

}
