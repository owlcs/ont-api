package ru.avicomp.ontapi.internal;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.jena.graph.Triple;
import org.semanticweb.owlapi.model.OWLAxiom;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntStatement;

/**
 * The base class to perform Axiom Graph Translator (operator 'T'), both for reading and writing.
 * Specification: <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Mapping_from_the_Structural_Specification_to_RDF_Graphs'>2.1 Translation of Axioms without Annotations</a>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
@SuppressWarnings("WeakerAccess")
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
     * @return Set of {@link InternalObject} with {@link OWLAxiom} as key and Set of {@link Triple} as value
     */
    public Set<InternalObject<Axiom>> read(OntGraphModel model) {
        try {
            return readAxioms(model);
        } catch (Exception e) {
            throw new OntApiException(String.format("Can't process reading. Translator <%s>.", getClass()), e);
        }
    }

    public Set<InternalObject<Axiom>> readAxioms(OntGraphModel model) {
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
     *
     * @param statement {@link OntStatement} any statement, not necessarily local.
     * @return true if the statement corresponds the {@link Axiom}.
     */
    public abstract boolean testStatement(OntStatement statement);

    /**
     * Wraps the statement as OWL Axiom.
     *
     * @param statement {@link OntStatement} the statement which determines the axiom
     * @return {@link InternalObject} around the {@link OWLAxiom}
     */
    public abstract InternalObject<Axiom> asAxiom(OntStatement statement);

    /**
     * Gets the config from model's settings or dummy if it is naked Jena model.
     *
     * @param model {@link OntGraphModel}
     * @return {@link ConfigProvider.Config}
     */
    public ConfigProvider.Config getConfig(OntGraphModel model) {
        return model instanceof ConfigProvider ? ((ConfigProvider) model).getConfig() : ConfigProvider.DEFAULT;
    }

    /**
     * Gets the config from statement.
     *
     * @param statement {@link OntStatement}
     * @return {@link ConfigProvider.Config}
     */
    protected ConfigProvider.Config getConfig(OntStatement statement) {
        return getConfig(statement.getModel());
    }
}
