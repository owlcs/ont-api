/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.testutils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Objects;

public class RDFIOUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RDFIOUtils.class);


    public static void print(Model model) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("\n{}", asString(model, Lang.TURTLE));
        }
    }

    public static String asString(Model model, Lang ext) {
        return toStringWriter(model, ext).toString();
    }

    public static StringWriter toStringWriter(Model model, Lang lang) {
        StringWriter sw = new StringWriter();
        model.write(sw, lang.getName(), null);
        return sw;
    }

    public static Model loadResourceAsModel(String resource, Lang lang) {
        Model m = ModelFactory.createDefaultModel();
        try (InputStream in = Objects.requireNonNull(RDFIOUtils.class.getResourceAsStream(resource))) {
            return m.read(in, null, lang.getName());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
