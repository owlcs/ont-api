package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.output.WriterOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.log4j.Logger;
import org.semanticweb.owlapi.formats.TurtleDocumentFormat;
import org.semanticweb.owlapi.io.OWLOntologyDocumentTarget;
import org.semanticweb.owlapi.io.OWLOntologyStorageIOException;
import org.semanticweb.owlapi.model.*;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import ru.avicomp.ontapi.jena.OntFactory;
import ru.avicomp.ontapi.jena.UnionGraph;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.jena.utils.Graphs;
import ru.avicomp.ontapi.jena.utils.Models;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;
import uk.ac.manchester.cs.owl.owlapi.concurrent.NoOpReadWriteLock;

/**
 * Ontology Manager implementation.
 * <p>
 * Created by @szuev on 03.10.2016.
 */
public class OntologyManagerImpl extends OWLOntologyManagerImpl implements OntologyManager {
    private static final Logger LOGGER = Logger.getLogger(OntologyManagerImpl.class);
    public static final GraphFactory DEFAULT_GRAPH_FACTORY = OntFactory::createDefaultGraph;

    private GraphFactory graphFactory = DEFAULT_GRAPH_FACTORY;
    protected final ReadWriteLock lock;

    @Inject
    public OntologyManagerImpl(OWLDataFactory dataFactory, ReadWriteLock readWriteLock) {
        super(dataFactory, readWriteLock);
        this.lock = readWriteLock;
        ontologyFactories.set(new OntBuildingFactoryImpl());
    }

    /**
     * override to disable @Inject
     * TODO: it is temporary solution.
     *
     * @param factories Set
     */
    @Override
    public void setOntologyFactories(@Nonnull Set<OWLOntologyFactory> factories) {
        super.setOntologyFactories(factories);
    }

    public boolean isConcurrent() {
        return !NoOpReadWriteLock.class.isInstance(lock);
    }

    public ReadWriteLock getLock() {
        return lock;
    }

    @Override
    public OntologyModel createOntology(@Nonnull OWLOntologyID id) {
        try {
            return (OntologyModel) super.createOntology(id);
        } catch (OWLOntologyCreationException e) {
            throw new OntApiException(e);
        }
    }

    @Override
    public OntologyModel loadOntology(@Nonnull IRI source) throws OWLOntologyCreationException {
        return (OntologyModel) super.loadOntology(source);
    }

    @Override
    public void setGraphFactory(GraphFactory factory) {
        this.graphFactory = OntApiException.notNull(factory, "Null graph-factory");
    }

    @Override
    public GraphFactory getGraphFactory() {
        return graphFactory;
    }

    @Override
    public OntologyModel getOntology(@Nullable IRI iri) {
        return (OntologyModel) super.getOntology(iri);
    }

    @Override
    public OntologyModel getOntology(@Nonnull OWLOntologyID id) {
        return (OntologyModel) super.getOntology(id);
    }

    @Override
    public OntologyModel getImportedOntology(@Nonnull OWLImportsDeclaration declaration) {
        return (OntologyModel) super.getImportedOntology(declaration);
    }

    /**
     * hotfix.
     * <p>
     * we have changes in behaviour in comparison with OWL-API while add/remove imports.
     * see {@link OntologyModelImpl.RDFChangeProcessor#addImport(OWLImportsDeclaration)} and
     * {@link OntologyModelImpl.RDFChangeProcessor#removeImport(OWLImportsDeclaration)}.
     * <p>
     * While renaming some ontology OWL-API performs adding/removing imports for ontologies where that one is in use.
     * This method is to avoid exception caused by {@link #checkDocumentIRI} during {@link #getImportedOntology}.
     *
     * @param declaration {@link OWLImportsDeclaration}
     * @return {@link OntologyModelImpl} or null.
     */
    public OntologyModelImpl getOntologyByImportDeclaration(OWLImportsDeclaration declaration) {
        try {
            return (OntologyModelImpl) getImportedOntology(declaration);
        } catch (OWLRuntimeException re) {
            if (re.getCause() instanceof OWLOntologyDocumentAlreadyExistsException) {
                OntologyModelImpl res = getOntologyByDocumentIRI(declaration.getIRI());
                if (res != null) return res;
            }
            throw re;
        }
    }

    /**
     * see private method {@link super#getOntologyByDocumentIRI(IRI)} in parent class
     *
     * @param documentIRI iri
     * @return {@link OntologyModelImpl}
     */
    public OntologyModelImpl getOntologyByDocumentIRI(IRI documentIRI) {
        return documentIRIsByID.entrySet().stream()
                .filter(o -> documentIRI.equals(o.getValue()))
                .map(Map.Entry::getKey)
                .map(ontologiesByID::get)
                .map(OntologyModelImpl.class::cast)
                .findFirst().orElse(null);
    }

    /**
     * The punning is a situation when the same subject (uri-resource) has several declarations,
     * i.e. can be treated as entity of different types (e.g. OWLClass and OWLIndividual).
     * In OWL 2 DL a property cannot have owl:ObjectProperty, owl:DatatypeProperty or olw:AnnotationProperty declaration
     * at the same time, it is so called illegal punning. Same for owl:Class and rdfs:Datatype.     *
     * It seems that all other kind of type intersections are allowed, e.g ObjectProperty can be Datatype or Class,
     * and NamedIndividual can be anything else.
     * More about punnings see <a href='https://www.w3.org/2007/OWL/wiki/Punning'>wiki</a>.
     * See also OWL-API example of implementation this logic {@link OWLDocumentFormat#computeIllegals(Multimap)}
     * <p>
     * In general cases the original method does not do anything but printing errors (OWL-5.0.4),
     * i.e. when we have all entities explicitly declared in graph.
     * But when we use to load ontology some OWL-API parser, we could have some declarations missed
     * (i.e. the corresponding entity is proclaimed implicitly under some other axiom).
     * For this case the original method tries to fix such illegal punning by removing annotation property declaration
     * if the entity is object or date property.
     * It seems it is not correct.
     * There is unpredictable changes in 'signature' when we use different formats or/and reload ontology several times.
     * In addition ONT-API works in different way: the axioms with illegal punnings can not be read from graph
     * (even corresponding triples are present) and can not be added to graph.
     * So we just skip this method.
     *
     * @param o {@link OWLOntology}
     */
    @Override
    protected void fixIllegalPunnings(OWLOntology o) {
        // nothing here.
    }

    @Override
    public void saveOntology(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat, @Nonnull IRI documentIRI) throws OWLOntologyStorageException {
        saveOntology(ontology, ontologyFormat, new OWLOntologyDocumentTarget() {
            @Override
            public Optional<IRI> getDocumentIRI() {
                return Optional.of(documentIRI);
            }
        });
    }

    @Override
    public void saveOntology(@Nonnull OWLOntology ontology, @Nonnull OWLDocumentFormat ontologyFormat, @Nonnull OWLOntologyDocumentTarget documentTarget)
            throws OWLOntologyStorageException {
        try {
            lock.readLock().lock();
            write(ontology, ontologyFormat, documentTarget);
        } finally {
            lock.readLock().unlock();
        }
    }

    protected void write(OWLOntology ontology, OWLDocumentFormat documentFormat, OWLOntologyDocumentTarget target) throws OWLOntologyStorageException {
        OntFormat format = OntFormat.get(documentFormat);
        if (format == null || !format.isJena() || !OntologyModel.class.isInstance(ontology)) {
            if (OntologyModel.class.isInstance(ontology)) {
                // It does not work correctly without expanding axioms for some OWL-API formats such as ManchesterSyntaxDocumentFormat.
                // The cache cleaning encourages extracting hidden axioms (declarations) in an explicit form while getting axioms:
                ((OntologyModel) ontology).clearCache();
            }
            super.saveOntology(ontology, documentFormat, target);
            return;
        }
        OutputStream os = null;
        if (target.getOutputStream().isPresent()) {
            os = target.getOutputStream().get();
        } else if (target.getDocumentIRI().isPresent()) {
            IRI iri = target.getDocumentIRI().get();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Save " + ontology.getOntologyID() + " to " + iri);
            }
            try {
                os = openStream(iri);
            } catch (IOException e) {
                throw new OWLOntologyStorageIOException(e);
            }
        } else if (target.getWriter().isPresent()) {
            os = new WriterOutputStream(target.getWriter().get(), StandardCharsets.UTF_8);
        }
        if (os == null) {
            throw new OWLOntologyStorageException("Null output stream, format = " + documentFormat);
        }
        Model model = ((OntologyModel) ontology).asGraphModel().getBaseModel();
        PrefixManager pm = PrefixManager.class.isInstance(documentFormat) ? (PrefixManager) documentFormat : null;
        setDefaultPrefix(pm, ontology);
        Map<String, String> newPrefixes = pm != null ? pm.getPrefixName2PrefixMap() : Collections.emptyMap();
        Map<String, String> initPrefixes = model.getNsPrefixMap();
        try {
            Models.setNsPrefixes(model, newPrefixes);
            RDFDataMgr.write(os, model, format.getLang());
        } finally {
            Models.setNsPrefixes(model, initPrefixes);
        }
    }


    /**
     * todo: currently it is only for turtle.
     * see similar fragment inside constructor of {@link org.semanticweb.owlapi.rdf.turtle.renderer.TurtleRenderer}.
     * todo: it seems we don't need default prefix at all.
     *
     * @param pm  {@link PrefixManager}
     * @param owl {@link OWLOntology}
     */
    public static void setDefaultPrefix(PrefixManager pm, OWLOntology owl) {
        if (pm == null || owl == null) return;
        if (!TurtleDocumentFormat.class.isInstance(pm)) return;
        if (pm.getDefaultPrefix() != null) return;
        if (!owl.getOntologyID().getOntologyIRI().isPresent()) return;
        String uri = owl.getOntologyID().getOntologyIRI().get().getIRIString();
        if (!uri.endsWith("/")) {
            uri += "#";
        }
        pm.setDefaultPrefix(uri);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static OutputStream openStream(IRI iri) throws IOException {
        if ("file".equals(iri.getScheme())) {
            File file = new File(iri.toURI());
            file.getParentFile().mkdirs();
            return new FileOutputStream(file);
        }
        URL url = iri.toURI().toURL();
        URLConnection conn = url.openConnection();
        return conn.getOutputStream();
    }

    /**
     * serialization.
     * this method fixes graph links
     * (ontology A with ontology B in imports should have {@link UnionGraph} inside which consists of base graph from A and base graph from B).
     *
     * @param in {@link ObjectInputStream}
     * @throws IOException            exception
     * @throws ClassNotFoundException exception
     */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Set<UnionGraph> graphs = ontologies()
                .map(OntologyModelImpl.class::cast)
                .map(OntBaseModelImpl::getBase)
                .map(OntGraphModelImpl::getGraph)
                .collect(Collectors.toSet());
        for (UnionGraph base : graphs) {
            Stream<UnionGraph> imports = Graphs.getImports(base).stream()
                    .map(s -> graphs.stream().filter(g -> Objects.equals(s, Graphs.getURI(g))).findFirst().orElse(null))
                    .filter(Objects::nonNull);
            imports.forEach(base::addGraph);
        }
    }
}
