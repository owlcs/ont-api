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

package com.github.owlcs.owlapi.tests.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.semanticweb.owlapi.model.MissingImportHandlingStrategy;
import org.semanticweb.owlapi.model.MissingOntologyHeaderStrategy;
import org.semanticweb.owlapi.model.PriorityCollectionSorting;
import org.semanticweb.owlapi.model.parameters.ConfigurationOptions;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class ConfigurationOptionsTestCase {

    @Parameter()
    public ConfigurationOptions config;
    @Parameter(1)
    public Object value;

    @Parameters(name = "{0}")
    public static List<Object[]> values() {
        List<Object[]> toReturn = new ArrayList<>();
        toReturn.add(new Object[]{ConfigurationOptions.ACCEPT_HTTP_COMPRESSION, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.CONNECTION_TIMEOUT, 20000});
        toReturn.add(new Object[]{ConfigurationOptions.FOLLOW_REDIRECTS, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.INDENT_SIZE, 4});
        toReturn.add(new Object[]{ConfigurationOptions.INDENTING, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.LABELS_AS_BANNER, Boolean.FALSE});
        toReturn.add(new Object[]{ConfigurationOptions.LOAD_ANNOTATIONS, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.PARSE_WITH_STRICT_CONFIGURATION, Boolean.FALSE});
        toReturn.add(new Object[]{ConfigurationOptions.MISSING_IMPORT_HANDLING_STRATEGY,
                MissingImportHandlingStrategy.THROW_EXCEPTION});
        toReturn.add(new Object[]{ConfigurationOptions.MISSING_ONTOLOGY_HEADER_STRATEGY,
                MissingOntologyHeaderStrategy.INCLUDE_GRAPH});
        toReturn.add(new Object[]{ConfigurationOptions.PRIORITY_COLLECTION_SORTING,
                PriorityCollectionSorting.ON_SET_INJECTION_ONLY});
        toReturn.add(new Object[]{ConfigurationOptions.REMAP_IDS, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.REPORT_STACK_TRACES, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.RETRIES_TO_ATTEMPT, 5});
        toReturn.add(new Object[]{ConfigurationOptions.SAVE_IDS, Boolean.FALSE});
        toReturn.add(new Object[]{ConfigurationOptions.TREAT_DUBLINCORE_AS_BUILTIN, Boolean.TRUE});
        toReturn.add(new Object[]{ConfigurationOptions.USE_NAMESPACE_ENTITIES, Boolean.FALSE});
        return toReturn;
    }

    @Test
    public void shouldFindExpectedValue() {
        assertEquals(value, config.getValue(value.getClass(), new EnumMap<>(ConfigurationOptions.class)));
        assertEquals(value, config.getDefaultValue(value.getClass()));
    }
}
