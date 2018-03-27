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

import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.model.parameters.ConfigurationOptions;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.conf.Configurable;
import ru.avicomp.ontapi.transforms.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum of all ONT-API settings (20 origin OWL-API options + 9 new ONT-API options + ignored imports)
 * Note: System properties are not taken into account (this is a difference from OWL-API).
 * We use the properties file as the primary settings store and this enum goes as secondary attempt to load.
 * See <a href="file:../resources/ontapi.properties">ontapi.properties</a>
 * <p>
 * Created by @szuev on 14.04.2017.
 * @see ConfigurationOptions
 */
public enum OntSettings implements OntConfig.OptionSetting {
    OWL_API_LOAD_CONF_IGNORED_IMPORTS(new ArrayList<String>()),

    ONT_API_LOAD_CONF_TRANSFORMERS(Stream.of(OWLIDTransform.class, OWLRecursiveTransform.class,
            RDFSTransform.class, OWLCommonTransform.class, OWLDeclarationTransform.class)
            .collect(Collectors.toCollection(ArrayList::new))),
    ONT_API_LOAD_CONF_SUPPORTED_SCHEMES(OntConfig.DefaultScheme.all().collect(Collectors.toCollection(ArrayList::new))),
    ONT_API_LOAD_CONF_PERSONALITY_MODE(Configurable.Mode.MEDIUM),
    ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS(true),
    ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS(true),
    ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS(true),
    ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS(true),
    ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD(false),
    ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS(false),

    OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION(true),
    OWL_API_LOAD_CONF_CONNECTION_TIMEOUT(20000),
    OWL_API_LOAD_CONF_FOLLOW_REDIRECTS(true),
    OWL_API_LOAD_CONF_LOAD_ANNOTATIONS(true),
    OWL_API_LOAD_CONF_MISSING_IMPORT_HANDLING_STRATEGY(MissingImportHandlingStrategy.THROW_EXCEPTION),
    OWL_API_LOAD_CONF_MISSING_ONTOLOGY_HEADER_STRATEGY(MissingOntologyHeaderStrategy.INCLUDE_GRAPH),
    OWL_API_LOAD_CONF_REPORT_STACK_TRACES(true),
    OWL_API_LOAD_CONF_RETRIES_TO_ATTEMPT(5),
    OWL_API_LOAD_CONF_PARSE_WITH_STRICT_CONFIGURATION(false),
    OWL_API_LOAD_CONF_TREAT_DUBLINCORE_AS_BUILTIN(true),
    OWL_API_LOAD_CONF_PRIORITY_COLLECTION_SORTING(PriorityCollectionSorting.ON_SET_INJECTION_ONLY),
    OWL_API_LOAD_CONF_BANNED_PARSERS(""),
    OWL_API_LOAD_CONF_ENTITY_EXPANSION_LIMIT("100000000"),

    ONT_API_WRITE_CONF_CONTROL_IMPORTS(true),

    OWL_API_WRITE_CONF_SAVE_IDS(false),
    OWL_API_WRITE_CONF_REMAP_IDS(true),
    OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES(false),
    OWL_API_WRITE_CONF_INDENTING(true),
    OWL_API_WRITE_CONF_LABEL_AS_BANNER(false),
    OWL_API_WRITE_CONF_BANNERS_ENABLED(true),
    OWL_API_WRITE_CONF_INDENT_SIZE(4),;

    protected static final ExtendedProperties PROPERTIES = loadProperties();

    protected final Serializable secondary;

    OntSettings(Serializable value) {
        this.secondary = value;
    }

    @Override
    public Serializable getDefaultValue() {
        String k = key();
        Serializable primary;
        if (secondary instanceof Enum) {
            primary = PROPERTIES.getEnumProperty(k);
        } else if (secondary instanceof Class) {
            primary = PROPERTIES.getClassProperty(k);
        } else if (secondary instanceof List) {
            List<?> list = PROPERTIES.getListProperty(k);
            primary = list == null ? new ArrayList<>() : list instanceof Serializable ? (Serializable) list : new ArrayList<>(list);
        } else if (secondary instanceof Boolean) {
            primary = PROPERTIES.getBooleanProperty(k);
        } else if (secondary instanceof Integer) {
            primary = PROPERTIES.getIntegerProperty(k);
        } else if (secondary instanceof Long) {
            primary = PROPERTIES.getLongProperty(k);
        } else if (secondary instanceof Double) {
            primary = PROPERTIES.getDoubleProperty(k);
        } else if (secondary instanceof String) {
            primary = PROPERTIES.getProperty(k);
        } else {
            throw new OntApiException("Unsupported value " + secondary.getClass());
        }
        return primary == null ? secondary : primary;
    }

    public boolean isLoad() {
        return name().contains("_LOAD_CONF_");
    }

    public boolean isWrite() {
        return name().contains("_WRITE_CONF_");
    }

    public boolean isONT() {
        return name().startsWith("ONT_API");
    }

    public String key() {
        return name().toLowerCase().replace("_", ".");
    }

    protected static ExtendedProperties loadProperties() {
        ExtendedProperties res = new ExtendedProperties();
        try (InputStream io = OntApiException.notNull(OntSettings.class.getResourceAsStream("/ontapi.properties"), "Null properties")) {
            res.load(io);
        } catch (IOException e) {
            throw new OntApiException("No properties", e);
        }
        return res;
    }
}
