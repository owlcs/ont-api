/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, The University of Manchester, owl.cs group.
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

import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.model.parameters.ConfigurationOptions;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.jena.impl.conf.OntModelConfig;
import ru.avicomp.ontapi.jena.vocabulary.*;
import ru.avicomp.ontapi.transforms.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The {@code Enum} of all ONT-API settings (22 OWL-API options + 15 ONT-API specific options)
 * Note: System properties are not taken into account (this is a difference from OWL-API-impl).
 * The properties file is used as the primary settings store.
 * The default value, that is encoding in this enum,
 * is used as secondary attempt in case a property entry is absent in the file.
 * See <a href="file:../resources/ontapi.properties">ontapi.properties</a>
 * <p>
 * Created by @szuev on 14.04.2017.
 *
 * @see ConfigurationOptions
 */
public enum OntSettings {
    OWL_API_LOAD_CONF_IGNORED_IMPORTS(OWL.NS
            , RDF.getURI()
            , RDFS.getURI()
            , SWRL.NS
            , SWRLB.NS
            , "http://www.w3.org/XML/1998/namespace"
            , XSD.NS),

    ONT_API_LOAD_CONF_SUPPORTED_SCHEMES(OntConfig.DefaultScheme.HTTP
            , OntConfig.DefaultScheme.HTTPS
            , OntConfig.DefaultScheme.FTP
            , OntConfig.DefaultScheme.FILE),

    ONT_API_LOAD_CONF_PERFORM_TRANSFORMATIONS(true),
    ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS(true),
    ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS(true),
    ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS(true),
    ONT_API_LOAD_CONF_USE_OWL_PARSERS_TO_LOAD(false),
    ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS(false),
    ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS(false),

    // cache options since 1.4.0
    ONT_API_LOAD_CONF_CACHE_MODEL(CacheSettings.CACHE_ALL),
    ONT_API_LOAD_CONF_CACHE_OBJECTS(2048),
    ONT_API_LOAD_CONF_CACHE_NODES(50_000),
    ONT_API_MANAGER_CACHE_IRIS(2048),

    // since 1.4.1
    ONT_API_LOAD_CONF_PROCESS_IMPORTS(true),

    OWL_API_LOAD_CONF_ACCEPT_HTTP_COMPRESSION(true),
    OWL_API_LOAD_CONF_CONNECTION_TIMEOUT(20_000),
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
    OWL_API_AUTHORIZATION_VALUE(""),

    ONT_API_WRITE_CONF_CONTROL_IMPORTS(false),

    OWL_API_WRITE_CONF_SAVE_IDS(false),
    OWL_API_WRITE_CONF_REMAP_IDS(true),
    OWL_API_WRITE_CONF_USE_NAMESPACE_ENTITIES(false),
    OWL_API_WRITE_CONF_INDENTING(true),
    OWL_API_WRITE_CONF_LABEL_AS_BANNER(false),
    OWL_API_WRITE_CONF_BANNERS_ENABLED(true),
    OWL_API_WRITE_CONF_INDENT_SIZE(4),

    ONT_API_LOAD_CONF_PERSONALITY_MODE(OntModelConfig.StdMode.MEDIUM) {
        @Override
        public Object getDefaultValue() { // note: the return object is not Serializable
            OntModelConfig.StdMode mode = getPersonalityMode();
            switch (mode) {
                case LAX:
                    return OntModelConfig.ONT_PERSONALITY_LAX;
                case MEDIUM:
                    return OntModelConfig.ONT_PERSONALITY_MEDIUM;
                case STRICT:
                    return OntModelConfig.ONT_PERSONALITY_STRICT;
                default:
                    throw new OntApiException.Unsupported("Unsupported personality mode " + mode);
            }
        }

        OntModelConfig.StdMode getPersonalityMode() {
            OntModelConfig.StdMode res = (OntModelConfig.StdMode) PROPERTIES.getEnumProperty(key);
            if (res == null) {
                res = (OntModelConfig.StdMode) secondary;
            }
            return res;
        }
    },
    ONT_API_LOAD_CONF_TRANSFORMERS(OWLIDTransform.class
            , RDFSTransform.class
            , OWLCommonTransform.class
            , OWLDeclarationTransform.class
            , SWRLTransform.class) {
        @Override
        public Object getDefaultValue() {
            GraphTransformers.Store res = new GraphTransformers.Store();
            for (Class<? extends Transform> c : getTransformTypes()) {
                res = res.add(new GraphTransformers.DefaultMaker(c));
            }
            return res;
        }

        @SuppressWarnings("unchecked")
        List<Class<? extends Transform>> getTransformTypes() {
            List<?> res = PROPERTIES.getListProperty(key);
            if (res == null) {
                res = (List<?>) secondary;
            }
            return (List<Class<? extends Transform>>) res;
        }
    };

    public static final ExtendedProperties PROPERTIES = loadProperties();

    static final OntSettings[] LOAD_CONFIG_KEYS = filter(OntSettings::isLoad);
    static final OntSettings[] WRITE_CONFIG_KEYS = filter(OntSettings::isWrite);

    protected final Serializable secondary;
    protected final String key;
    private final boolean isONT;
    private final boolean isLoad;
    private final boolean isWrite;

    OntSettings(Serializable... values) {
        this(Arrays.stream(values).collect(Collectors.toCollection(ArrayList::new)));
    }

    OntSettings(Serializable value) {
        this.secondary = value;
        this.key = name().toLowerCase().replace("_", ".");
        this.isONT = key.startsWith("ont.api");
        this.isLoad = key.contains(".load.conf.");
        this.isWrite = key.contains(".write.conf.");
    }

    /**
     * Returns a default option value.
     * Looks firstly into properties file, if the record is not found returns the default enum value.
     *
     * @return an immutable (or unmodifiable) object that is associated with this key
     */
    public Object getDefaultValue() {
        String k = key();
        Object primary;
        if (secondary instanceof Enum) {
            primary = PROPERTIES.getEnumProperty(k);
        } else if (secondary instanceof Class) {
            primary = PROPERTIES.getClassProperty(k);
        } else if (secondary instanceof List) {
            List<?> list = PROPERTIES.getListProperty(k);
            if (list == null) {
                list = (List<?>) secondary;
            }
            return Collections.unmodifiableList(list);
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
        return isLoad;
    }

    public boolean isWrite() {
        return isWrite;
    }

    public boolean isONT() {
        return isONT;
    }

    public String key() {
        return key;
    }

    private static ExtendedProperties loadProperties() {
        ExtendedProperties res = new ExtendedProperties();
        try (InputStream io = OntApiException.notNull(OntSettings.class.getResourceAsStream("/ontapi.properties"),
                "Null properties")) {
            res.load(io);
        } catch (IOException e) {
            throw new OntApiException("No ontapi.properties found.", e);
        }
        return res;
    }

    private static OntSettings[] filter(Predicate<OntSettings> filter) {
        return Arrays.stream(values()).filter(filter).toArray(OntSettings[]::new);
    }
}
