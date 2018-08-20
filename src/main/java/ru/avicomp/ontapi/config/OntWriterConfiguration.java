/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2018, Avicomp Services, AO
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package ru.avicomp.ontapi.config;

import org.semanticweb.owlapi.model.OWLOntologyWriterConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extended {@link OWLOntologyWriterConfiguration}.
 * Currently there is only one ONT-API (new) options,
 * all other content is mostly modified copy-paste from the original OWL-API class.
 * Note: this config is immutable.
 * @see OntConfig
 */
@SuppressWarnings("WeakerAccess")
public class OntWriterConfiguration extends OWLOntologyWriterConfiguration {

    protected final Map<OntConfig.OptionSetting, Object> map = new HashMap<>();

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

    protected Object get(OntConfig.OptionSetting key) {
        return key.fromMap(map);
    }

    protected OntWriterConfiguration set(OntSettings key, Object o) {
        if (Objects.equals(get(key), o)) return this;
        OntWriterConfiguration copy = copy(this);
        copy.map.put(key, o);
        return copy;
    }

    /**
     * ONT-API getter.
     * by default it is true.
     *
     * @return true if imports control is allowed.
     * @see #setControlImports(boolean)
     */
    public boolean isControlImports() {
        return (boolean) get(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS);
    }

    /**
     * ONT-API config setter to change imports control option setting.
     * This option manages behaviour on adding and removing imports.
     * If it is true then:
     * - while adding import all duplicated declaration are removed from the source ontology,
     * if the imported ontology contains the same declarations.
     * - while removing import the missed declarations will be restored if they are used.
     * If it is false the source ontology stays unchanged.
     *
     * @param b boolean, to enable or disable imports control.
     * @return this or new {@link OntWriterConfiguration} instance.
     * @see #isControlImports()
     */
    public OntWriterConfiguration setControlImports(boolean b) {
        return set(OntSettings.ONT_API_WRITE_CONF_CONTROL_IMPORTS, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#shouldUseBanners()
     */
    @Override
    public boolean shouldUseBanners() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED);
    }

    /**
     * NOTE: a OWL-API (ver. 5.0.5) BUG in the original implementation:
     * copy-paste from {@link OWLOntologyWriterConfiguration#withLabelsAsBanner(boolean)}
     *
     * @see OWLOntologyWriterConfiguration#withBannersEnabled(boolean)
     */
    public OntWriterConfiguration withBannersEnabled(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_BANNERS_ENABLED, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#isLabelsAsBanner()
     */
    @Override
    public boolean isLabelsAsBanner() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER);
    }

    /**
     * @see OWLOntologyWriterConfiguration#withLabelsAsBanner(boolean)
     */
    @Override
    public OntWriterConfiguration withLabelsAsBanner(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_LABEL_AS_BANNER, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#shouldSaveIdsForAllAnonymousIndividuals()
     */
    @Override
    public boolean shouldSaveIdsForAllAnonymousIndividuals() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS);
    }

    /**
     * @see OWLOntologyWriterConfiguration#withSaveIdsForAllAnonymousIndividuals(boolean)
     */
    @Override
    public OntWriterConfiguration withSaveIdsForAllAnonymousIndividuals(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_SAVE_IDS, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#shouldRemapAllAnonymousIndividualsIds()
     */
    @Override
    public boolean shouldRemapAllAnonymousIndividualsIds() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS);
    }

    /**
     * @see OWLOntologyWriterConfiguration#withRemapAllAnonymousIndividualsIds
     */
    @Override
    public OntWriterConfiguration withRemapAllAnonymousIndividualsIds(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_REMAP_IDS, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#isUseNamespaceEntities()
     */
    @Override
    public boolean isUseNamespaceEntities() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES);
    }

    /**
     * @see OWLOntologyWriterConfiguration#withUseNamespaceEntities(boolean)
     */
    @Override
    public OntWriterConfiguration withUseNamespaceEntities(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#isIndenting()
     */
    @Override
    public boolean isIndenting() {
        return (boolean) get(OntSettings.OWL_API_WRITE_CONF_INDENTING);
    }

    /**
     * @see OWLOntologyWriterConfiguration#withIndenting(boolean)
     */
    @Override
    public OntWriterConfiguration withIndenting(boolean b) {
        return set(OntSettings.OWL_API_WRITE_CONF_INDENTING, b);
    }

    /**
     * @see OWLOntologyWriterConfiguration#getIndentSize()
     */
    @Override
    public int getIndentSize() {
        return (int) get(OntSettings.OWL_API_WRITE_CONF_INDENT_SIZE);
    }

    /**
     * @see OWLOntologyWriterConfiguration#withIndentSize(int)
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
