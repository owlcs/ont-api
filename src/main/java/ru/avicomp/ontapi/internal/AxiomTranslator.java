package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntConfig;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * The base class to perform Axiom Graph Translator (operator 'T'), both for reading and writing.
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
public abstract class AxiomTranslator<Axiom extends OWLAxiom> {

    /**
     * Writes axiom to model.
     *
     * @param axiom {@link OWLAxiom}
     * @param model {@link OntGraphModel}
     */
    public abstract void write(Axiom axiom, OntGraphModel model);

    /**
     * Reads axioms and triples from model.
     *
     * @param model {@link OntGraphModel}
     * @return Set of {@link Wrap} with {@link OWLAxiom} as key and Set of {@link Triple} as value
     */
    public Set<Wrap<Axiom>> read(OntGraphModel model) {
        try {
            return readAxioms(model);
        } catch (Exception e) {
            throw new OntApiException(String.format("Can't process reading. Translator <%s>.", getClass()), e);
        }
    }

    public Set<Wrap<Axiom>> readAxioms(OntGraphModel model) {
        return statements(model).map(this::asAxiom).collect(Collectors.toSet());
    }

    /**
     * Returns the stream of statements defining the axiom in the base graph of the specified model.
     *
     * @param model {@link OntGraphModel} the model
     * @return Stream of {@link OntStatement}, always local (not from imports)
     */
    public abstract Stream<OntStatement> statements(OntGraphModel model);

    /**
     * Tests if the specified statement answers the axiom's definition.
     * @param statement {@link OntStatement} any statement, not necessarily local.
     * @return true if the statement corresponds the {@link Axiom}.
     */
    public abstract boolean testStatement(OntStatement statement);

    /**
     * Wraps the statement as OWL Axiom.
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @return {@link Wrap} around the {@link OWLAxiom}
     */
    public abstract Wrap<Axiom> asAxiom(OntStatement statement);

    /**
     * Gets {@link OWLDataFactory} from {@link InternalModel} or {@link AxiomParserProvider#DATA_FACTORY} in case any other {@link OntGraphModel}.
     * @param m {@link OntGraphModel}
     * @return {@link OWLDataFactory}
     */
    OWLDataFactory getDataFactory(OntGraphModel m) {
        return m instanceof InternalModel ? ((InternalModel) m).dataFactory() : AxiomParserProvider.DATA_FACTORY;
    }

    /**
     * @param m {@link OntGraphModel}
     * @return {@link ru.avicomp.ontapi.OntConfig.LoaderConfiguration}
     */
    OntConfig.LoaderConfiguration getLoaderConfig(OntGraphModel m) {
        return AxiomParserProvider.LOADER_CONFIGURATION;
    }

    /**
     * @param m {@link OntGraphModel}
     * @return {@link OWLOntologyWriterConfiguration}
     */
    OWLOntologyWriterConfiguration getWriterConfig(OntGraphModel m) {
        return AxiomParserProvider.WRITER_CONFIGURATION;
    }

}
