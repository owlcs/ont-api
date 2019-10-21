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

package com.github.owlcs.ontapi.config;

import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.transforms.GraphTransformers;

import java.util.Collections;
import java.util.List;

/**
 * A common interface to control ontology loading settings.
 * <p>
 * Created by @szz on 14.05.2019.
 *
 * @param <R> config, either {@link OntConfig} (this instance) or {@link OntLoaderConfiguration} (a copied instance)
 * @see LoadSettings
 * @since 1.4.0
 */
interface LoadControl<R> extends LoadSettings {

    /**
     * Sets {@code OntPersonality} model configuration object.
     *
     * @param p {@link OntPersonality}, not {@code null}
     * @return {@link R} (this or copied instance)
     * @see LoadSettings#getPersonality()
     */
    R setPersonality(OntPersonality p);

    /**
     * Sets {@code GraphTransformers.Store} collection.
     *
     * @param t {@link GraphTransformers.Store}, not {@code null}
     * @return {@link R} (this or copied instance)
     * @see LoadSettings#getGraphTransformers()
     * @see LoadSettings#isPerformTransformation()
     */
    R setGraphTransformers(GraphTransformers.Store t);

    /**
     * Disables or enables the Graph Transformation mechanism depending on the given flag.
     *
     * @param b boolean
     * @return {@link R} (this or copied instance)
     * @see LoadSettings#isPerformTransformation()
     */
    R setPerformTransformation(boolean b);

    /**
     * Disables or enables the processing imports depending on the given flag.
     *
     * @param b boolean
     * @return {@link R} (this or copied instance)
     * @see LoadSettings#isProcessImports()
     * @see org.semanticweb.owlapi.model.OntologyConfigurator#setMissingOntologyHeaderStrategy(org.semanticweb.owlapi.model.MissingOntologyHeaderStrategy)
     * @see org.semanticweb.owlapi.model.OntologyConfigurator#addIgnoredImport(org.semanticweb.owlapi.model.IRI)
     * @see LoadControl#disableWebAccess()
     * @since 1.4.1
     */
    R setProcessImports(boolean b);

    /**
     * Changes the preferable way to load a {@code Graph}.
     * If {@code true} specified, the OWL-API native parsers will be used.
     * Though, it is not recommended,
     * for more details see the {@link LoadSettings#isUseOWLParsersToLoad() getter} description.
     *
     * @param b boolean
     * @return {@link R} (this or copied instance)
     * @see LoadSettings#isUseOWLParsersToLoad()
     */
    R setUseOWLParsersToLoad(boolean b);

    /**
     * Sets a new collection of {@link Scheme}-controllers.
     *
     * @param schemes List of {@link Scheme}s
     * @return {@link R}
     * @see LoadSettings#getSupportedSchemes()
     */
    R setSupportedSchemes(List<Scheme> schemes);

    /**
     * Disables all schemes with except of {@code file} to prevent internet diving.
     * While loading only IRIs starting with {@code file} will be processed.
     *
     * @return {@link R}
     * @see LoadControl#setSupportedSchemes(List)
     * @see #setProcessImports(boolean)
     */
    default R disableWebAccess() {
        return setSupportedSchemes(Collections.singletonList((Scheme) () -> "file"));
    }
}
