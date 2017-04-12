package ru.avicomp.ontapi;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ConfigurationOptions;

import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.transforms.GraphTransformers;

/**
 * The config builder.
 * see base class {@link org.semanticweb.owlapi.model.OntologyConfigurator}
 * Created by szuev on 27.02.2017.
 */
public class OntConfig extends OntologyConfigurator {

    @Override
    public LoaderConfiguration buildLoaderConfiguration() {
        return new LoaderConfiguration(super.buildLoaderConfiguration());
    }

    @Override
    public WriterConfiguration buildWriterConfiguration() {
        return new WriterConfiguration(super.buildWriterConfiguration());
    }

    public static OntConfig copy(OntologyConfigurator from) {
        OntConfig res = new OntConfig();
        if (from == null) return res;
        try {
            Field ignoredImports = from.getClass().getDeclaredField("ignoredImports");
            ignoredImports.setAccessible(true);
            ignoredImports.set(res, ignoredImports.get(from));
            Field overrides = from.getClass().getDeclaredField("overrides");
            overrides.setAccessible(true);
            overrides.set(res, overrides.get(from));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new OntApiException("Can't copy configuration.", e);
        }
        return res;
    }

    /**
     * Extended {@link OWLOntologyWriterConfiguration}.
     * Currently there is no new options and it is mostly copy-paste of OWL-API class.
     */
    @SuppressWarnings("WeakerAccess")
    public static class WriterConfiguration extends OWLOntologyWriterConfiguration {
        public enum Settings {
            SAVE_IDS(false),
            REMAP_IDS(true),
            USE_NAMESPACE_ENTITIES(false),
            INDENTING(true),
            LABEL_AS_BANNER(false),
            BANNERS_ENABLED(true),
            INDENT_SIZE(4),;
            private final Serializable value;

            Settings(Serializable value) {
                this.value = value;
            }

            public Serializable value() {
                return value;
            }
        }

        protected EnumMap<Settings, Object> map = new EnumMap<>(Settings.class);

        public WriterConfiguration(OWLOntologyWriterConfiguration owl) {
            if (owl == null) return;
            this.map.put(Settings.SAVE_IDS, owl.shouldSaveIdsForAllAnonymousIndividuals());
            this.map.put(Settings.REMAP_IDS, owl.shouldRemapAllAnonymousIndividualsIds());
            this.map.put(Settings.USE_NAMESPACE_ENTITIES, owl.isUseNamespaceEntities());
            this.map.put(Settings.INDENTING, owl.isIndenting());
            this.map.put(Settings.LABEL_AS_BANNER, owl.isLabelsAsBanner());
            this.map.put(Settings.BANNERS_ENABLED, owl.shouldUseBanners());
            this.map.put(Settings.INDENT_SIZE, owl.getIndentSize());
        }

        protected WriterConfiguration copy(OWLOntologyWriterConfiguration owl) {
            return new WriterConfiguration(owl);
        }

        protected Object get(Settings key) {
            return map.getOrDefault(key, key.value());
        }

        protected WriterConfiguration set(Settings key, Object o) {
            if (Objects.equals(get(key), o)) return this;
            WriterConfiguration copy = copy(this);
            copy.map.put(key, o);
            return copy;
        }


        /**
         * @see super#shouldUseBanners()
         */
        @Override
        public boolean shouldUseBanners() {
            return (boolean) get(Settings.BANNERS_ENABLED);
        }

        /**
         * NOTE: a OWL-API BUG in the original implementation.
         *
         * @see super#withBannersEnabled(boolean)
         */
        public WriterConfiguration withBannersEnabled(boolean b) {
            return set(Settings.BANNERS_ENABLED, b);
        }

        /**
         * @see super#isLabelsAsBanner()
         */
        @Override
        public boolean isLabelsAsBanner() {
            return (boolean) get(Settings.LABEL_AS_BANNER);
        }

        /**
         * @see super#withLabelsAsBanner(boolean)
         */
        @Override
        public WriterConfiguration withLabelsAsBanner(boolean b) {
            return set(Settings.LABEL_AS_BANNER, b);
        }

        /**
         * @see super#shouldSaveIdsForAllAnonymousIndividuals()
         */
        @Override
        public boolean shouldSaveIdsForAllAnonymousIndividuals() {
            return (boolean) get(Settings.SAVE_IDS);
        }

        /**
         * @see super#withSaveIdsForAllAnonymousIndividuals(boolean)
         */
        @Override
        public WriterConfiguration withSaveIdsForAllAnonymousIndividuals(boolean b) {
            return set(Settings.SAVE_IDS, b);
        }

        /**
         * @see super#shouldRemapAllAnonymousIndividualsIds()
         */
        @Override
        public boolean shouldRemapAllAnonymousIndividualsIds() {
            return (boolean) get(Settings.REMAP_IDS);
        }

        /**
         * @see super@withRemapAllAnonymousIndividualsIds
         */
        @Override
        public WriterConfiguration withRemapAllAnonymousIndividualsIds(boolean b) {
            return set(Settings.REMAP_IDS, b);
        }

        /**
         * @see super#isUseNamespaceEntities()
         */
        @Override
        public boolean isUseNamespaceEntities() {
            return (boolean) get(Settings.USE_NAMESPACE_ENTITIES);
        }

        /**
         * @see super#withUseNamespaceEntities(boolean)
         */
        @Override
        public WriterConfiguration withUseNamespaceEntities(boolean b) {
            return set(Settings.USE_NAMESPACE_ENTITIES, b);
        }

        /**
         * @see super#isIndenting()
         */
        @Override
        public boolean isIndenting() {
            return (boolean) get(Settings.INDENTING);
        }

        /**
         * @see super#withIndenting(boolean)
         */
        @Override
        public WriterConfiguration withIndenting(boolean b) {
            return set(Settings.INDENTING, b);
        }

        /**
         * @see super#getIndentSize()
         */
        @Override
        public int getIndentSize() {
            return (int) get(Settings.INDENT_SIZE);
        }

        /**
         * @see super#withIndentSize(int)
         */
        @Override
        public WriterConfiguration withIndentSize(int indent) {
            return set(Settings.INDENT_SIZE, indent);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof WriterConfiguration)) return false;
            WriterConfiguration that = (WriterConfiguration) o;
            return Objects.equals(map, that.map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map);
        }
    }

    /**
     * Extended {@link OWLOntologyLoaderConfiguration} with ONT-API specific settings.
     * It is a wrapper since all members of original base class are private.
     * TODO: new (ONT-API) options should be configured in global ({@link OntConfig}) config also.
     */
    @SuppressWarnings({"WeakerAccess", "SameParameterValue", "unused"})
    public static class LoaderConfiguration extends OWLOntologyLoaderConfiguration {
        protected final OWLOntologyLoaderConfiguration inner;
        // WARNING: OntPersonality is not serializable:
        protected transient OntPersonality personality;
        protected GraphTransformers.Store transformers;
        protected boolean performTransformation = true;
        protected HashSet<Scheme> supportedSchemes;
        protected boolean allowBulkAnnotationAssertions = true;
        protected boolean allowReadDeclarations = true;
        protected boolean ignoreAnnotationAxiomOverlaps = true;
        protected boolean useOWLParsersToLoad = false;
        // from super class:
        protected Set<IRI> ignoredImports;
        protected EnumMap<ConfigurationOptions, Object> overrides;

        public LoaderConfiguration(OWLOntologyLoaderConfiguration owl) {
            this.inner = owl == null ? new OWLOntologyLoaderConfiguration() :
                    owl instanceof LoaderConfiguration ? ((LoaderConfiguration) owl).inner : owl;
        }

        /**
         * the analogue of{@link super#copyConfiguration()}, since the original method is private.
         *
         * @param owl to copy from.
         * @return new instance of {@link LoaderConfiguration}
         */
        protected LoaderConfiguration copy(OWLOntologyLoaderConfiguration owl) {
            LoaderConfiguration res = new LoaderConfiguration(owl);
            res.personality = this.personality;
            res.transformers = this.transformers;
            res.performTransformation = this.performTransformation;
            res.supportedSchemes = this.supportedSchemes;
            res.allowBulkAnnotationAssertions = this.allowBulkAnnotationAssertions;
            res.allowReadDeclarations = this.allowReadDeclarations;
            res.ignoreAnnotationAxiomOverlaps = this.ignoreAnnotationAxiomOverlaps;
            res.useOWLParsersToLoad = this.useOWLParsersToLoad;
            return res;
        }

        @SuppressWarnings("unchecked")
        protected Set<IRI> ignoredImports() {
            try {
                if (ignoredImports != null) return ignoredImports;
                Field field = inner.getClass().getDeclaredField("ignoredImports");
                field.setAccessible(true);
                return ignoredImports = (Set<IRI>) field.get(inner);
            } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
                throw new OntApiException("Can't get ignoredImports.", e);
            }
        }

        @SuppressWarnings("unchecked")
        protected EnumMap<ConfigurationOptions, Object> overrides() {
            try {
                if (overrides != null) return overrides;
                Field field = inner.getClass().getDeclaredField("overrides");
                field.setAccessible(true);
                return overrides = (EnumMap<ConfigurationOptions, Object>) field.get(inner);
            } catch (IllegalAccessException | NoSuchFieldException | ClassCastException e) {
                throw new OntApiException("Can't get ignoredImports.", e);
            }
        }

        /**
         * ONT-API config method.
         *
         * @return if true some graph transformations will be performed after loading graph.
         * @see #getGraphTransformers()
         * @see #setGraphTransformers(GraphTransformers.Store)
         */
        public boolean isPerformTransformation() {
            return performTransformation;
        }

        /**
         * ONT-API config setter.
         *
         * @param b if false all graph transformations will be disabled.
         * @return {@link LoaderConfiguration}
         * @see #getGraphTransformers()
         * @see #setGraphTransformers(GraphTransformers.Store)
         */
        public LoaderConfiguration setPerformTransformation(boolean b) {
            if (b == performTransformation) return this;
            LoaderConfiguration res = copy(inner);
            res.performTransformation = b;
            return res;
        }

        /**
         * ONT-API config method.
         * Note: after deserialization it is always default.
         *
         * @return the {@link org.apache.jena.enhanced.Personality}, if null then default ({@link OntModelConfig#getPersonality()})
         */
        public OntPersonality getPersonality() {
            return personality == null ? personality = OntModelConfig.getPersonality() : personality;
        }

        /**
         * ONT-API config setter.
         *
         * @param p {@link OntPersonality} new personality. Null means default ({@link OntModelConfig#getPersonality()})
         * @return {@link LoaderConfiguration}
         */
        public LoaderConfiguration setPersonality(OntPersonality p) {
            if (Objects.equals(personality, p)) return this;
            LoaderConfiguration res = copy(inner);
            res.personality = p;
            return res;
        }

        /**
         * ONT-API config method.
         *
         * @return {@link GraphTransformers.Store} a collection with {@link GraphTransformers.Maker}s.
         * @see #isPerformTransformation()
         * @see #setPerformTransformation(boolean)
         */
        public GraphTransformers.Store getGraphTransformers() {
            return transformers == null ? transformers = GraphTransformers.getTransformers() : transformers;
        }

        /**
         * ONT-API config setter.
         *
         * @param t {@link GraphTransformers.Store} new graph transformers store. null means default
         * @return {@link LoaderConfiguration}
         * @see #isPerformTransformation()
         * @see #setPerformTransformation(boolean)
         */
        public LoaderConfiguration setGraphTransformers(GraphTransformers.Store t) {
            if (Objects.equals(transformers, t)) return this;
            LoaderConfiguration res = copy(inner);
            res.transformers = t;
            return res;
        }

        /**
         * ONT-API config method.
         *
         * @return Set of {@link Scheme}
         */
        public Set<Scheme> getSupportedSchemes() {
            return supportedSchemes == null ? supportedSchemes = DefaultScheme.all().collect(Collectors.toCollection(HashSet::new)) : supportedSchemes;
        }

        /**
         * ONT-API config setter.
         *
         * @param schemes the collection of {@link Scheme}
         * @return {@link LoaderConfiguration}
         */
        public LoaderConfiguration setSupportedSchemes(Collection<Scheme> schemes) {
            if (Objects.equals(supportedSchemes, schemes)) return this;
            LoaderConfiguration res = copy(inner);
            res.supportedSchemes = new HashSet<>(schemes);
            return res;
        }

        /**
         * ONT-API config method.
         * The additional to the {@link #isLoadAnnotationAxioms()} optional setting to manage behaviour of annotation axioms.
         * By default annotated annotation assertions are allowed.
         * See the example in the description of {@link #setAllowBulkAnnotationAssertions(boolean)}
         *
         * @return true if annotation assertions could be annotated.
         * @see #setAllowBulkAnnotationAssertions(boolean)
         * @see #setLoadAnnotationAxioms(boolean)
         * @see #isLoadAnnotationAxioms()
         */
        public boolean isAllowBulkAnnotationAssertions() {
            return allowBulkAnnotationAssertions;
        }

        /**
         * ONT-API config setter.
         * This option manages annotation assertion axioms in conjunction with declaration axioms.
         * In depends of parameter specified bulk annotations fall either into declaration or annotation assertion.
         * Consider the following example:
         * <pre>
         * <http://class>   a                       owl:Class ;
         *                  rdfs:comment            "plain assertion" ;
         *                  rdfs:label              "bulk assertion" .
         * [                a                       owl:Axiom ;
         *                  rdfs:comment            "the child" ;
         *                  owl:annotatedProperty   rdfs:label ;
         *                  owl:annotatedSource     <http://class> ;
         *                  owl:annotatedTarget     "bulk assertion"
         * ] .
         * </pre>
         * In case {@link #isAllowBulkAnnotationAssertions()} equals {@code true} this slice of graph corresponds to the following list of axioms:
         * * AnnotationAssertion(rdfs:comment <http://class> "plain assertion"^^xsd:string)
         * * AnnotationAssertion(Annotation(rdfs:comment "the child"^^xsd:string) rdfs:label <http://class> "bulk assertion"^^xsd:string)
         * * Declaration(Class(<http://class>))
         * In case {@link #isAllowBulkAnnotationAssertions()} equals {@code false} there would be following axioms:
         * * Declaration(Annotation(Annotation(rdfs:comment "the child"^^xsd:string) rdfs:label "bulk assertion"^^xsd:string) Class(<http://class>))
         * * AnnotationAssertion(rdfs:comment <http://class> "plain assertion"^^xsd:string)
         * Note: the {@link org.semanticweb.owlapi.formats.ManchesterSyntaxDocumentFormat} does NOT work correctly
         * in the second case (to test try to reload ontology in manchester syntax. The loss of annotations is expected).
         *
         * @param b if false only plain annotation assertions axioms expected.
         * @return {@link LoaderConfiguration}
         * @see #isAllowBulkAnnotationAssertions()
         * @see #setLoadAnnotationAxioms(boolean)
         * @see #isLoadAnnotationAxioms()
         * @see OntFormat#MANCHESTER_SYNTAX
         */
        public LoaderConfiguration setAllowBulkAnnotationAssertions(boolean b) {
            if (b == allowBulkAnnotationAssertions) return this;
            LoaderConfiguration res = copy(inner);
            res.allowBulkAnnotationAssertions = b;
            return res;
        }

        /**
         * ONT-API config method.
         * By default it is {@code true}.
         * See description of {@link #setAllowReadDeclarations(boolean)}.
         *
         * @return true if declarations are allowed in the structural view
         */
        public boolean isAllowReadDeclarations() {
            return allowReadDeclarations;
        }

        /**
         * ONT-API config setter.
         * This method is invited to match OWL-API behaviour.
         * Some of the true-OWL-API parsers skips declarations on loading.
         * It seems to me incorrect.
         * You never can know whether there are declarations in the structural form or not
         * if you have the same ontology but different formats.
         * Using this method you can change global behaviour.
         *
         * @param b true to skip declarations while reading graph
         * @return new or the same instance of config.
         */
        public LoaderConfiguration setAllowReadDeclarations(boolean b) {
            if (b == allowReadDeclarations) return this;
            LoaderConfiguration res = copy(inner);
            res.allowReadDeclarations = b;
            return res;
        }

        /**
         * ONT-API config method.
         * By default it is {@code true}.
         * See description of {@link #setIgnoreAnnotationAxiomOverlaps(boolean)}.
         *
         * @return true if possible ambiguities with annotation axioms should be ignored.
         */
        public boolean isIgnoreAnnotationAxiomOverlaps() {
            return ignoreAnnotationAxiomOverlaps;
        }

        /**
         * ONT-API config setter.
         * Determines the behavior while reading annotation axioms
         * if there is a 'punning' entity as subject in the root statement.
         * There are three types of annotation axioms with following defining statements:
         * - 'A rdfs:subPropertyOf Aj'
         * - 'A rdfs:domain U'
         * - 'A rdfs:range U'
         * and in case 'A' is also object property ('P') or data property ('R')
         * then these statements define also corresponded object or data property axioms.
         *
         * @param b if true all such axioms will be skipped in favour of data or/and object property axioms
         * @return this or new config.
         */
        public LoaderConfiguration setIgnoreAnnotationAxiomOverlaps(boolean b) {
            if (b == ignoreAnnotationAxiomOverlaps) return this;
            LoaderConfiguration res = copy(inner);
            res.ignoreAnnotationAxiomOverlaps = b;
            return res;
        }

        /**
         * Determines whether or not annotation axioms (instances of {@code OWLAnnotationAxiom}) should be loaded.
         * By default the loading of annotation axioms is enabled.
         *
         * @return if {@code false} all annotation axioms (assertion, range and domain) will be discarded on loading.
         * @see OWLOntologyLoaderConfiguration#isLoadAnnotationAxioms()
         */
        @Override
        public boolean isLoadAnnotationAxioms() {
            return inner.isLoadAnnotationAxioms();
        }

        /**
         * see description for {@link #isLoadAnnotationAxioms()}
         *
         * @param b true to enable reading and writing annotation axioms
         * @return instance of new config.
         * @see OWLOntologyLoaderConfiguration#setLoadAnnotationAxioms(boolean)
         */
        @Override
        public LoaderConfiguration setLoadAnnotationAxioms(boolean b) {
            return copy(inner.setLoadAnnotationAxioms(b));
        }

        /**
         * ONT-API config method.
         * By default it is {@code false}.
         *
         * @return true if loading through Jena is disabled (the loading is done through the OWL-API mechanisms by one axiom at a time).
         */
        public boolean isUseOWLParsersToLoad() {
            return useOWLParsersToLoad;
        }

        /**
         * ONT-API config setter.
         * To choose the preferable way to load (Jena vs pure OWL-API).
         * It is mainly for test purposes.
         * NOTE: It is strongly recommended to use default settings ({@code {@link #isUseOWLParsersToLoad()} = false}):
         * - There is no confidence in OWL-API mechanisms: all of them work different way, there is no any guaranty.
         * - They definitely contain bugs
         * - You never can not be sure that graph would be the same, moreover - in some cases you can be sure that you get broken graph.
         * - Worse performance.
         *
         * @param b true to use pure OWL-API parsers to load.
         * @return this or new config.
         */
        public LoaderConfiguration setUseOWLParsersToLoad(boolean b) {
            if (b == useOWLParsersToLoad) return this;
            LoaderConfiguration res = copy(inner);
            res.useOWLParsersToLoad = b;
            return res;
        }

        @Override
        public LoaderConfiguration addIgnoredImport(@Nonnull IRI iri) {
            return copy(inner.addIgnoredImport(iri));
        }

        @Override
        public LoaderConfiguration clearIgnoredImports() {
            return copy(inner.clearIgnoredImports());
        }

        @Override
        public boolean isIgnoredImport(@Nonnull IRI iri) {
            return inner.isIgnoredImport(iri);
        }

        @Override
        public LoaderConfiguration removeIgnoredImport(@Nonnull IRI ontologyDocumentIRI) {
            return copy(inner.removeIgnoredImport(ontologyDocumentIRI));
        }

        @Override
        public PriorityCollectionSorting getPriorityCollectionSorting() {
            return inner.getPriorityCollectionSorting();
        }

        @Override
        public LoaderConfiguration setPriorityCollectionSorting(PriorityCollectionSorting sorting) {
            return copy(inner.setPriorityCollectionSorting(sorting));
        }

        @Override
        public int getConnectionTimeout() {
            return inner.getConnectionTimeout();
        }

        @Override
        public LoaderConfiguration setConnectionTimeout(int l) {
            return copy(inner.setConnectionTimeout(l));
        }

        @Override
        public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
            return inner.getMissingImportHandlingStrategy();
        }

        @Override
        public LoaderConfiguration setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy missingImportHandlingStrategy) {
            return copy(inner.setMissingImportHandlingStrategy(missingImportHandlingStrategy));
        }

        @Override
        public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
            return inner.getMissingOntologyHeaderStrategy();
        }

        @Override
        public LoaderConfiguration setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy missingOntologyHeaderStrategy) {
            return copy(inner.setMissingOntologyHeaderStrategy(missingOntologyHeaderStrategy));
        }

        @Override
        public int getRetriesToAttempt() {
            return inner.getRetriesToAttempt();
        }

        @Override
        public LoaderConfiguration setRetriesToAttempt(int retries) {
            return copy(inner.setRetriesToAttempt(retries));
        }

        @Override
        public boolean isAcceptingHTTPCompression() {
            return inner.isAcceptingHTTPCompression();
        }

        @Override
        public LoaderConfiguration setAcceptingHTTPCompression(boolean b) {
            return copy(inner.setAcceptingHTTPCompression(b));
        }

        @Override
        public boolean isFollowRedirects() {
            return inner.isFollowRedirects();
        }

        @Override
        public LoaderConfiguration setFollowRedirects(boolean value) {
            return copy(inner.setFollowRedirects(value));
        }

        @Override
        public boolean isReportStackTrace() {
            return inner.isReportStackTrace();
        }

        @Override
        public boolean isStrict() {
            return inner.isStrict();
        }

        @Override
        public LoaderConfiguration setStrict(boolean strict) {
            return copy(inner.setStrict(strict));
        }

        @Override
        public boolean isTreatDublinCoreAsBuiltIn() {
            return inner.isTreatDublinCoreAsBuiltIn();
        }

        @Override
        public LoaderConfiguration setTreatDublinCoreAsBuiltIn(boolean value) {
            return copy(inner.setTreatDublinCoreAsBuiltIn(value));
        }

        @Override
        public String getBannedParsers() {
            return inner.getBannedParsers();
        }

        @Override
        public LoaderConfiguration setBannedParsers(@Nonnull String ban) {
            return copy(inner.setBannedParsers(ban));
        }

        @Override
        public String getEntityExpansionLimit() {
            return inner.getEntityExpansionLimit();
        }

        @Override
        public LoaderConfiguration setEntityExpansionLimit(@Nonnull String limit) {
            return copy(inner.setEntityExpansionLimit(limit));
        }

        @Override
        public LoaderConfiguration setReportStackTraces(boolean b) {
            return copy(inner.setReportStackTraces(b));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LoaderConfiguration)) return false;
            LoaderConfiguration that = (LoaderConfiguration) o;
            return isPerformTransformation() == that.isPerformTransformation() &&
                    isAllowBulkAnnotationAssertions() == that.isAllowBulkAnnotationAssertions() &&
                    isAllowReadDeclarations() == that.isAllowReadDeclarations() &&
                    isIgnoreAnnotationAxiomOverlaps() == that.isIgnoreAnnotationAxiomOverlaps() &&
                    isUseOWLParsersToLoad() == that.isUseOWLParsersToLoad() &&
                    Objects.equals(ignoredImports(), that.ignoredImports()) &&
                    Objects.equals(overrides(), that.overrides()) &&
                    Objects.equals(getPersonality(), that.getPersonality()) &&
                    Objects.equals(getGraphTransformers(), that.getGraphTransformers()) &&
                    Objects.equals(getSupportedSchemes(), that.getSupportedSchemes());
        }

        @Override
        public int hashCode() {
            return Objects.hash(ignoredImports(), overrides(),
                    getPersonality(), getGraphTransformers(),
                    isPerformTransformation(), getSupportedSchemes(),
                    isAllowBulkAnnotationAssertions(), isAllowReadDeclarations(),
                    isUseOWLParsersToLoad(), isIgnoreAnnotationAxiomOverlaps());
        }
    }


    public enum DefaultScheme implements Scheme {
        HTTP,
        HTTPS,
        FTP,
        FILE,;

        @Override
        public String key() {
            return name().toLowerCase();
        }

        @Override
        public boolean same(IRI iri) {
            return Objects.equals(key(), iri.getScheme());
        }

        public static Stream<DefaultScheme> all() {
            return Stream.of(values());
        }
    }

    public interface Scheme extends Serializable {
        String key();

        boolean same(IRI iri);
    }
}
