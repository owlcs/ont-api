package ru.avicomp.ontapi.config;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.configuration.Configurable;
import ru.avicomp.ontapi.jena.impl.configuration.OntModelConfig;
import ru.avicomp.ontapi.jena.impl.configuration.OntPersonality;
import ru.avicomp.ontapi.transforms.GraphTransformers;

/**
 * This is the global config and also the builder for the separated load and write configs.
 * It overrides OWL-API {@link OntologyConfigurator} and provides access to the new (ONT-API) settings.
 * Note: this configuration is mutable, while load and write configs are not.
 * Additional (new) ONT-API options (getters):
 * - {@link #getPersonality()}
 * - {@link #getGraphTransformers()}
 * - {@link #isPerformTransformation()}
 * - {@link #getSupportedSchemes()}
 * - {@link #isAllowReadDeclarations()}
 * - {@link #isAllowBulkAnnotationAssertions()}
 * - {@link #isIgnoreAnnotationAxiomOverlaps()}
 * - {@link #isUseOWLParsersToLoad()}
 * - {@link #isControlImports()}
 *
 * @see OntSettings
 * @see OntLoaderConfiguration
 * @see OntWriterConfiguration
 * Created by szuev on 27.02.2017.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class OntConfig extends OntologyConfigurator {
    protected final Map<OptionSetting, Object> map = new HashMap<>();
    // WARNING: OntPersonality is not serializable!
    protected transient OntPersonality personality;
    protected GraphTransformers.Store transformers;

    protected Object get(OptionSetting key) {
        return map.getOrDefault(key, key.getDefaultValue());
    }

    protected OntConfig put(OptionSetting key, Object value) {
        map.put(key, value);
        return this;
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setPersonality(OntPersonality)
     */
    public OntConfig setPersonality(OntPersonality p) {
        personality = OntApiException.notNull(p, "Null personality.");
        return this;
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#getPersonality()
     */
    public OntPersonality getPersonality() {
        return personality == null ? personality = getDefaultPersonality() : personality;
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setGraphTransformers(GraphTransformers.Store)
     */
    public OntConfig setGraphTransformers(GraphTransformers.Store t) {
        transformers = OntApiException.notNull(t, "Null graph transformer store.");
        return this;
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#getGraphTransformers()
     */
    public GraphTransformers.Store getGraphTransformers() {
        return transformers == null ? transformers = getDefaultTransformers() : transformers;
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#getSupportedSchemes()
     */
    @SuppressWarnings("unchecked")
    public List<OntConfig.Scheme> getSupportedSchemes() {
        return (List<OntConfig.Scheme>) get(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES);
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setSupportedSchemes(List)
     */
    public OntConfig setSupportedSchemes(List<OntConfig.Scheme> schemes) {
        return put(OntSettings.ONT_API_LOAD_CONF_SUPPORTED_SCHEMES, schemes instanceof Serializable ? schemes : new ArrayList<>(schemes));
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#isPerformTransformation()
     */
    public boolean isPerformTransformation() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS);
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setPerformTransformation(boolean)
     */
    public OntConfig setPerformTransformation(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS, b);
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#isAllowBulkAnnotationAssertions()
     */
    public boolean isAllowBulkAnnotationAssertions() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS);
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setAllowBulkAnnotationAssertions(boolean)
     */
    public OntConfig setAllowBulkAnnotationAssertions(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS, b);
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#isAllowReadDeclarations()
     */
    public boolean isAllowReadDeclarations() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS);
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setAllowReadDeclarations(boolean)
     */
    public OntConfig setAllowReadDeclarations(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS, b);
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#isIgnoreAnnotationAxiomOverlaps()
     */
    public boolean isIgnoreAnnotationAxiomOverlaps() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS);
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setIgnoreAnnotationAxiomOverlaps(boolean)
     */
    public OntConfig setIgnoreAnnotationAxiomOverlaps(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS, b);
    }

    /**
     * ONT-API(NEW) manager load config getter.
     *
     * @see OntLoaderConfiguration#isUseOWLParsersToLoad()
     */
    public boolean isUseOWLParsersToLoad() {
        return (boolean) get(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD);
    }

    /**
     * ONT-API(NEW) manager load config setter.
     *
     * @see OntLoaderConfiguration#setUseOWLParsersToLoad(boolean)
     */
    public OntConfig setUseOWLParsersToLoad(boolean b) {
        return put(OntSettings.ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD, b);
    }

    /**
     * ONT-API(NEW) manager write config getter.
     *
     * @see OntWriterConfiguration#isControlImports()
     */
    public boolean isControlImports() {
        return (boolean) get(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS);
    }

    /**
     * ONT-API(NEW) manager write config setter.
     *
     * @see OntWriterConfiguration#setControlImports(boolean)
     */
    public OntConfig setControlImports(boolean b) {
        return put(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS, b);
    }

    /**
     * @see OntologyConfigurator#setLoadAnnotationAxioms(boolean)
     */
    @Override
    public OntConfig setLoadAnnotationAxioms(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, b);
    }

    /**
     * @see OntologyConfigurator#shouldLoadAnnotations()
     */
    @Override
    public boolean shouldLoadAnnotations() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS);
    }

    /**
     * OWL-API(NEW) manager load config setter.
     * This is NOT override method.
     * The is NO such method in the original OWL-API ({@link OntologyConfigurator}) class.
     *
     * @see OWLOntologyLoaderConfiguration#setEntityExpansionLimit(String)
     * @see OntLoaderConfiguration#setEntityExpansionLimit(String)
     */
    public OntConfig withEntityExpansionLimit(@Nonnull String s) {
        return put(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT, s);
    }

    /**
     * OWL-API(NEW) manager load config getter.
     * This is NOT override method.
     * The is NO such method in the original OWL-API ({@link OntologyConfigurator}) class.
     *
     * @see OWLOntologyLoaderConfiguration#getEntityExpansionLimit()
     * @see OntLoaderConfiguration#getEntityExpansionLimit()
     */
    public String getEntityExpansionLimit() {
        return (String) get(OntSettings.OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT);
    }


    /**
     * @see OntologyConfigurator#withBannedParsers(String)
     */
    @Override
    public OntConfig withBannedParsers(@Nonnull String parsers) {
        return put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, parsers);
    }

    /**
     * @see OntologyConfigurator#getBannedParsers()
     */
    @Override
    public String getBannedParsers() {
        return (String) get(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS);
    }

    /**
     * @see OntologyConfigurator#setPriorityCollectionSorting(PriorityCollectionSorting)
     */
    @Override
    public OntConfig setPriorityCollectionSorting(@Nonnull PriorityCollectionSorting sorting) {
        return put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, sorting);
    }

    /**
     * @see OntologyConfigurator#getPriorityCollectionSorting()
     */
    @Override
    public PriorityCollectionSorting getPriorityCollectionSorting() {
        return (PriorityCollectionSorting) get(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING);
    }

    @SuppressWarnings("unchecked")
    protected List<String> getIgnoredImports() {
        return (List<String>) map.computeIfAbsent(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, OntConfig.OptionSetting::getDefaultValue);
    }

    /**
     * @see OntologyConfigurator#addIgnoredImport(IRI)
     */
    @Override
    public OntConfig addIgnoredImport(@Nonnull IRI iri) {
        if (!getIgnoredImports().contains(iri.getIRIString())) {
            getIgnoredImports().add(iri.getIRIString());
        }
        return this;
    }

    /**
     * @see OntologyConfigurator#clearIgnoredImports()
     */
    @Override
    public OntConfig clearIgnoredImports() {
        getIgnoredImports().clear();
        return this;
    }

    /**
     * @see OntologyConfigurator#removeIgnoredImport(IRI)
     */
    @Override
    public OntConfig removeIgnoredImport(@Nonnull IRI iri) {
        getIgnoredImports().remove(iri.getIRIString());
        return this;
    }

    /**
     * @see OntologyConfigurator#setAcceptingHTTPCompression(boolean)
     */
    @Override
    public OntConfig setAcceptingHTTPCompression(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, b);
    }

    /**
     * @see OntologyConfigurator#shouldAcceptHTTPCompression()
     */
    @Override
    public boolean shouldAcceptHTTPCompression() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION);
    }

    /**
     * @see OntologyConfigurator#setConnectionTimeout(int)
     */
    @Override
    public OntConfig setConnectionTimeout(int t) {
        return put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, t);
    }

    /**
     * @see OntologyConfigurator#getConnectionTimeout()
     */
    @Override
    public int getConnectionTimeout() {
        return (int) get(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT);
    }

    /**
     * @see OntologyConfigurator#setFollowRedirects(boolean)
     */
    @Override
    public OntConfig setFollowRedirects(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, b);
    }

    /**
     * @see OntologyConfigurator#shouldFollowRedirects()
     */
    @Override
    public boolean shouldFollowRedirects() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS);
    }

    /**
     * @see OntologyConfigurator#setMissingImportHandlingStrategy(MissingImportHandlingStrategy)
     */
    @Override
    public OntConfig setMissingImportHandlingStrategy(@Nonnull MissingImportHandlingStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, strategy);
    }

    /**
     * @see OntologyConfigurator#getMissingImportHandlingStrategy()
     */
    @Override
    public MissingImportHandlingStrategy getMissingImportHandlingStrategy() {
        return (MissingImportHandlingStrategy) get(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY);
    }

    /**
     * @see OntologyConfigurator#setMissingOntologyHeaderStrategy(MissingOntologyHeaderStrategy)
     */
    @Override
    public OntConfig setMissingOntologyHeaderStrategy(@Nonnull MissingOntologyHeaderStrategy strategy) {
        return put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, strategy);
    }

    /**
     * @see OntologyConfigurator#getMissingOntologyHeaderStrategy()
     */
    @Override
    public MissingOntologyHeaderStrategy getMissingOntologyHeaderStrategy() {
        return (MissingOntologyHeaderStrategy) get(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY);
    }

    /**
     * @see OntologyConfigurator#setReportStackTraces(boolean)
     */
    @Override
    public OntConfig setReportStackTraces(boolean b) {
        return put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, b);
    }

    /**
     * @see OntologyConfigurator#shouldReportStackTraces()
     */
    @Override
    public boolean shouldReportStackTraces() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES);
    }

    /**
     * @see OntologyConfigurator#setRetriesToAttempt(int)
     */
    @Override
    public OntConfig setRetriesToAttempt(int retries) {
        return put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, retries);
    }

    /**
     * @see OntologyConfigurator#getRetriesToAttempt()
     */
    @Override
    public int getRetriesToAttempt() {
        return (int) get(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT);
    }

    /**
     * @see OntologyConfigurator#setStrict(boolean)
     */
    @Override
    public OntConfig setStrict(boolean strict) {
        return put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, strict);
    }

    /**
     * @see OntologyConfigurator#shouldParseWithStrictConfiguration()
     */
    @Override
    public boolean shouldParseWithStrictConfiguration() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION);
    }

    /**
     * @see OntologyConfigurator#setTreatDublinCoreAsBuiltIn(boolean)
     */
    @Override
    public OntConfig setTreatDublinCoreAsBuiltIn(boolean value) {
        return put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, value);
    }

    /**
     * @see OntologyConfigurator#shouldTreatDublinCoreAsBuiltin()
     */
    @Override
    public boolean shouldTreatDublinCoreAsBuiltin() {
        return (boolean) get(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN);
    }

    /**
     * @see OntologyConfigurator#withSaveIdsForAllAnonymousIndividuals(boolean)
     */
    @Override
    public OntConfig withSaveIdsForAllAnonymousIndividuals(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, b);
    }

    /**
     * @see OntologyConfigurator#shouldSaveIds()
     */
    @Override
    public boolean shouldSaveIds() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS);
    }

    /**
     * @see OntologyConfigurator#withRemapAllAnonymousIndividualsIds(boolean)
     */
    @Override
    public OntConfig withRemapAllAnonymousIndividualsIds(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, b);
    }

    /**
     * @see OntologyConfigurator#shouldRemapIds()
     */
    @Override
    public boolean shouldRemapIds() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS);
    }

    /**
     * @see OntologyConfigurator#withUseNamespaceEntities(boolean)
     */
    @Override
    public OntConfig withUseNamespaceEntities(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, b);
    }

    /**
     * @see OntologyConfigurator#shouldUseNamespaceEntities()
     */
    @Override
    public boolean shouldUseNamespaceEntities() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES);
    }

    /**
     * @see OntologyConfigurator#withIndenting(boolean)
     */
    @Override
    public OntConfig withIndenting(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_INDENTING, b);
    }

    /**
     * @see OntologyConfigurator#shouldIndent()
     */
    @Override
    public boolean shouldIndent() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_INDENTING);
    }

    /**
     * @see OntologyConfigurator#withIndentSize(int)
     */
    @Override
    public OntConfig withIndentSize(int indent) {
        return put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, indent);
    }

    /**
     * @see OntologyConfigurator#getIndentSize()
     */
    @Override
    public int getIndentSize() {
        return (int) get(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE);
    }

    /**
     * @see OntologyConfigurator#withLabelsAsBanner(boolean)
     */
    @Override
    public OntConfig withLabelsAsBanner(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, b);
    }

    /**
     * @see OntologyConfigurator#shouldUseLabelsAsBanner()
     */
    @Override
    public boolean shouldUseLabelsAsBanner() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER);
    }

    /**
     * @see OntologyConfigurator#withBannersEnabled(boolean)
     */
    @Override
    public OntConfig withBannersEnabled(boolean b) {
        return put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, b);
    }

    /**
     * @see OntologyConfigurator#shouldUseBanners()
     */
    @Override
    public boolean shouldUseBanners() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED);
    }

    /**
     * Builds new loader configuration.
     *
     * @return new {@link OntLoaderConfiguration}
     * @see OntologyConfigurator#buildLoaderConfiguration()
     */
    @Override
    public OntLoaderConfiguration buildLoaderConfiguration() {
        OntLoaderConfiguration res = new OntLoaderConfiguration(null);
        res.personality = getPersonality();
        res.transformers = getGraphTransformers();
        for (OntSettings s : OntSettings.values()) {
            if (!s.isLoad()) continue;
            res.map.put(s, get(s));
        }
        return res;
    }

    /**
     * Builds new writer configuration.
     *
     * @return new {@link OntWriterConfiguration}
     * @see OntologyConfigurator#buildWriterConfiguration()
     */
    @Override
    public OntWriterConfiguration buildWriterConfiguration() {
        OntWriterConfiguration res = new OntWriterConfiguration(null);
        for (OntSettings s : OntSettings.values()) {
            if (!s.isWrite()) continue;
            res.map.put(s, get(s));
        }
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntConfig)) return false;
        OntConfig that = (OntConfig) o;
        return Objects.equals(this.getPersonality(), that.getPersonality()) &&
                Objects.equals(this.getGraphTransformers(), that.getGraphTransformers()) &&
                Objects.equals(this.map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getPersonality(), this.getGraphTransformers(), this.map);
    }

    public static OntConfig copy(OntologyConfigurator from) {
        OntConfig res = new OntConfig();
        if (from == null) return res;
        if (from instanceof OntConfig) {
            res.map.putAll(((OntConfig) from).map);
            res.setPersonality(((OntConfig) from).getPersonality());
            res.setGraphTransformers(((OntConfig) from).getGraphTransformers());
            return res;
        }

        res.map.put(OntSettings.OWL_API_LOAD_CONF_IGNORED_IMPORTS, new ArrayList<>(ignoredImports(from)));
        res.map.put(OntSettings.OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION, from.shouldAcceptHTTPCompression());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_CONNECTION_TIMEOUT, from.getConnectionTimeout());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_FOLLOW_REDIRECTS, from.shouldFollowRedirects());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_LOAD_ANNOTATIONS, from.shouldLoadAnnotations());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY, from.getMissingImportHandlingStrategy());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY, from.getMissingOntologyHeaderStrategy());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_REPORT_STACK_TRACES, from.shouldReportStackTraces());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT, from.getRetriesToAttempt());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION, from.shouldParseWithStrictConfiguration());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN, from.shouldTreatDublinCoreAsBuiltin());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING, from.getPriorityCollectionSorting());
        res.map.put(OntSettings.OWL_API_LOAD_CONF_BANNED_PARSERS, from.getBannedParsers());
        // NOTE: there is no ConfigurationOption.ENTITY_EXPANSION_LIMIT inside original (OWL-API, ver 5.0.5) class.

        res.map.put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, from.shouldSaveIds());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, from.shouldRemapIds());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, from.shouldUseNamespaceEntities());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_INDENTING, from.shouldIndent());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, from.shouldUseLabelsAsBanner());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, from.shouldUseBanners());
        res.map.put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, from.getIndentSize());

        return res;
    }

    @SuppressWarnings("unchecked")
    protected static Set<IRI> ignoredImports(OntologyConfigurator owl) {
        try {
            Field field = owl.getClass().getDeclaredField("ignoredImports");
            field.setAccessible(true);
            return (Set<IRI>) field.get(owl);
        } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new OntApiException("Can't get OntologyConfigurator#ignoredImports.", e);
        }
    }

    public static OntPersonality getDefaultPersonality() {
        Configurable.Mode mode = (Configurable.Mode) OntSettings.ONT_API_LOAD_CONF_PERSONALITY_MODE.getDefaultValue();
        switch (mode) {
            case LAX:
                return OntModelConfig.ONT_PERSONALITY_LAX;
            case MEDIUM:
                return OntModelConfig.ONT_PERSONALITY_MEDIUM;
            case STRICT:
                return OntModelConfig.ONT_PERSONALITY_STRICT;
            default:
                throw new OntApiException("Unsupported personality mode " + mode);
        }
    }

    public static GraphTransformers.Store d;

    @SuppressWarnings("unchecked")
    public static GraphTransformers.Store getDefaultTransformers() {
        List<Class> transformers = (List<Class>) OntSettings.ONT_API_LOAD_CONF_TRANSFORMERS.getDefaultValue();
        GraphTransformers.Store res = new GraphTransformers.Store();
        for (Class c : transformers) {
            res = res.add(new GraphTransformers.DefaultMaker(c));
        }
        return res;
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

    public interface OptionSetting {
        Serializable getDefaultValue();
    }
}
