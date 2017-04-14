package ru.avicomp.ontapi.config;

import java.util.EnumMap;
import java.util.Objects;

import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;

/**
 * Extended {@link OWLOntologyWriterConfiguration}.
 * Currently there is no new options and it is mostly copy-paste of OWL-API class.
 */
@SuppressWarnings("WeakerAccess")
public class OntWriterConfiguration extends OWLOntologyWriterConfiguration {

    protected EnumMap<OntSettings, Object> map = new EnumMap<>(OntSettings.class);

    public OntWriterConfiguration(OWLOntologyWriterConfiguration owl) {
        if (owl == null) return;
        this.map.put(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, owl.shouldSaveIdsForAllAnonymousIndividuals());
        this.map.put(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, owl.shouldRemapAllAnonymousIndividualsIds());
        this.map.put(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, owl.isUseNamespaceEntities());
        this.map.put(OntSettings.OWL_API_WRITE_CONF_INDENTING, owl.isIndenting());
        this.map.put(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, owl.isLabelsAsBanner());
        this.map.put(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, owl.shouldUseBanners());
        this.map.put(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, owl.getIndentSize());
    }

    protected OntWriterConfiguration copy(OWLOntologyWriterConfiguration owl) {
        return new OntWriterConfiguration(owl);
    }

    protected Object get(OntSettings key) {
        return map.getOrDefault(key, key.getDefaultValue());
    }

    protected OntWriterConfiguration set(OntSettings key, Object o) {
        if (Objects.equals(get(key), o)) return this;
        OntWriterConfiguration copy = copy(this);
        copy.map.put(key, o);
        return copy;
    }

    /**
     * @see super#shouldUseBanners()
     */
    @Override
    public boolean shouldUseBanners() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED);
    }

    /**
     * NOTE: a OWL-API BUG in the original implementation.
     *
     * @see super#withBannersEnabled(boolean)
     */
    public OntWriterConfiguration withBannersEnabled(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, b);
    }

    /**
     * @see super#isLabelsAsBanner()
     */
    @Override
    public boolean isLabelsAsBanner() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER);
    }

    /**
     * @see super#withLabelsAsBanner(boolean)
     */
    @Override
    public OntWriterConfiguration withLabelsAsBanner(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, b);
    }

    /**
     * @see super#shouldSaveIdsForAllAnonymousIndividuals()
     */
    @Override
    public boolean shouldSaveIdsForAllAnonymousIndividuals() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS);
    }

    /**
     * @see super#withSaveIdsForAllAnonymousIndividuals(boolean)
     */
    @Override
    public OntWriterConfiguration withSaveIdsForAllAnonymousIndividuals(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, b);
    }

    /**
     * @see super#shouldRemapAllAnonymousIndividualsIds()
     */
    @Override
    public boolean shouldRemapAllAnonymousIndividualsIds() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS);
    }

    /**
     * @see super@withRemapAllAnonymousIndividualsIds
     */
    @Override
    public OntWriterConfiguration withRemapAllAnonymousIndividualsIds(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, b);
    }

    /**
     * @see super#isUseNamespaceEntities()
     */
    @Override
    public boolean isUseNamespaceEntities() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES);
    }

    /**
     * @see super#withUseNamespaceEntities(boolean)
     */
    @Override
    public OntWriterConfiguration withUseNamespaceEntities(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, b);
    }

    /**
     * @see super#isIndenting()
     */
    @Override
    public boolean isIndenting() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_INDENTING);
    }

    /**
     * @see super#withIndenting(boolean)
     */
    @Override
    public OntWriterConfiguration withIndenting(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_INDENTING, b);
    }

    /**
     * @see super#getIndentSize()
     */
    @Override
    public int getIndentSize() {
        return (int) get(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE);
    }

    /**
     * @see super#withIndentSize(int)
     */
    @Override
    public OntWriterConfiguration withIndentSize(int indent) {
        return set(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE, indent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntWriterConfiguration)) return false;
        OntWriterConfiguration that = (OntWriterConfiguration) o;
        return Objects.equals(map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}
