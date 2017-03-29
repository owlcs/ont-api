package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.stream.Stream;

import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntGraphModel;


/**
 * Ontology manager.
 * Base class: {@link OWLOntologyManager}
 * It is the main point for creating, loading and accessing {@link OntologyModel}s models.
 * <p>
 * Created by szuev on 24.10.2016.
 */
public interface OntologyManager extends OWLOntologyManager {

    /**
     * Returns the loading config.
     * Extended immutable {@link OWLOntologyLoaderConfiguration}.
     * Be warned: this is a read only accessor, to change configuration create a new config (using any its setter)
     * and pass it to the {@link #setOntologyLoaderConfiguration(OWLOntologyLoaderConfiguration)} method.
     *
     * @return {@link OntConfig.LoaderConfiguration}
     */
    @Override
    OntConfig.LoaderConfiguration getOntologyLoaderConfiguration();

    /**
     * Returns the copy of global config (extended {@link OntologyConfigurator}) and
     * also access point to the {@link OWLOntologyLoaderConfiguration} and {@link OWLOntologyWriterConfiguration}
     *
     * @return {@link OntConfig}
     */
    @Override
    OntConfig getOntologyConfigurator();

    /**
     * Contrary to the original description this method works with version IRI also if it fails with ontology IRI.
     *
     * @param iri {@link IRI} ontology IRI or version IRI as can be seen from the OWL-API implementation
     *            (see {@link uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#getOntology(IRI)})
     * @return {@link OntologyModel} or null
     */
    @Override
    OntologyModel getOntology(@Nonnull IRI iri);

    @Override
    OntologyModel getOntology(@Nonnull OWLOntologyID id);

    /**
     * See description for {@link #getOntology(IRI)} and be warned!
     *
     * @param iri {@link IRI} the ontology iri or version iri
     * @return true if ontology exists
     * @see uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#contains(IRI)
     */
    @Override
    boolean contains(@Nonnull IRI iri);

    /**
     * Be warned: this method returns always false for any anonymous id.
     * For non-anonymous id it performs searching by ontology iri ignoring version iri.
     * It is to order to make the behaviour the same as OWL-API method.
     *
     * @param id {@link OWLOntologyID} the id
     * @return true if {@code id} is not anonymous and there is an ontology with the same iri as in the specified {@code id}
     * @see uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl#contains(OWLOntologyID)
     */
    @Override
    boolean contains(@Nonnull OWLOntologyID id);

    @Nullable
    OntologyModel getImportedOntology(@Nonnull OWLImportsDeclaration declaration);

    /**
     * Creates an ontology.
     * <p>
     * Note: this method doesn't throw a checked exception {@link OWLOntologyCreationException} like OWL-API.
     * Instead it there is an unchecked exception {@link OntApiException} which may wrap {@link OWLOntologyCreationException}.
     * OWL-API and ONT-API work in different ways.
     * So i see it is impossible to save the same places for exceptions.
     * For example in OWL-API you could expect some kind of exception during saving ontology,
     * but in ONT-API there would be another kind of exception during adding axiom.
     * I believe there is no reasons to save the same behaviour with exceptions everywhere.
     * The return type is also changed from {@link org.semanticweb.owlapi.model.OWLOntology} to our class {@link OntologyModel}.
     *
     * @param id {@link OWLOntologyID}
     * @return ontology {@link OntologyModel}
     * @throws OntApiException in case something wrong.
     */
    @Override
    OntologyModel createOntology(@Nonnull OWLOntologyID id);

    @Override
    OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException;

    default OntologyModel createOntology() {
        return createOntology(new OWLOntologyID());
    }

    default OntologyModel createOntology(@Nullable IRI iri) {
        return createOntology(new OWLOntologyID(Optional.ofNullable(iri), Optional.empty()));
    }

    default OntGraphModel getGraphModel(@Nullable String uri, @Nullable String version) {
        OWLOntologyID id = new OWLOntologyID(Optional.ofNullable(uri).map(IRI::create), Optional.ofNullable(version).map(IRI::create));
        OntologyModel res = getOntology(id);
        return res == null ? null : res.asGraphModel();
    }

    default OntGraphModel getGraphModel(@Nullable String uri) {
        return getGraphModel(uri, null);
    }

    default OntGraphModel createGraphModel(@Nullable String uri, @Nullable String version) {
        OWLOntologyID id = new OWLOntologyID(Optional.ofNullable(uri).map(IRI::create), Optional.ofNullable(version).map(IRI::create));
        return createOntology(id).asGraphModel();
    }

    default OntGraphModel createGraphModel(@Nullable String uri) {
        return createGraphModel(uri, null);
    }

    default Stream<OntGraphModel> models() {
        return ontologies().map(OntologyModel.class::cast).map(OntologyModel::asGraphModel);
    }

    /**
     * The core of manager.
     * The factory to create and load ontologies.
     */
    interface Factory extends OWLOntologyFactory {

        @Override
        OntologyModel createOWLOntology(@Nonnull OWLOntologyManager manager,
                                        @Nonnull OWLOntologyID id,
                                        @Nonnull IRI documentIRI,
                                        @Nonnull OWLOntologyCreationHandler handler);

        @Override
        OntologyModel loadOWLOntology(@Nonnull OWLOntologyManager manager,
                                      @Nonnull OWLOntologyDocumentSource source,
                                      @Nonnull OWLOntologyCreationHandler handler,
                                      @Nonnull OWLOntologyLoaderConfiguration config) throws OWLOntologyCreationException;
    }
}
