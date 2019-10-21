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

/**
 * A common interface to control axioms settings.
 * <p>
 * Created by @ssz on 15.03.2019.
 *
 * @param <R> config, either {@link OntConfig} (this instance) or {@link OntLoaderConfiguration} (a copied instance)
 * @since 1.4.0
 */
interface AxiomsControl<R> extends AxiomsSettings {

    /**
     * Sets the read annotation axioms option to the specified state.
     *
     * @param b boolean
     * @return {@link R}
     * @see OntSettings#OWL_API_LOAD_CONF_LOAD_ANNOTATIONS
     * @see AxiomsSettings#isLoadAnnotationAxioms()
     */
    R setLoadAnnotationAxioms(boolean b);

    /**
     * Sets the allow bulk annotation assertions option to the specified state.
     *
     * @param b boolean
     * @return {@link R}
     * @see OntSettings#ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS
     * @see AxiomsSettings#isAllowBulkAnnotationAssertions()
     */
    R setAllowBulkAnnotationAssertions(boolean b);

    /**
     * Sets the ignore annotation axioms overlaps option to the specified state.
     *
     * @param b boolean
     * @return {@link R}
     * @see OntSettings#ONT_API_LOAD_CONF_IGNORE_ANNOTATION_AXIOM_OVERLAPS
     * @see AxiomsSettings#isIgnoreAnnotationAxiomOverlaps()
     */
    R setIgnoreAnnotationAxiomOverlaps(boolean b);

    /**
     * Sets the read declarations option to the desired state.
     *
     * @param b boolean
     * @return {@link R}
     * @see OntSettings#ONT_API_LOAD_CONF_ALLOW_READ_DECLARATIONS
     * @see AxiomsSettings#isAllowReadDeclarations()
     */
    R setAllowReadDeclarations(boolean b);

    /**
     * Sets the axiom-annotations-split option setting to the desired state.
     *
     * @param b boolean
     * @return {@link R}
     * @see OntSettings#ONT_API_LOAD_CONF_SPLIT_AXIOM_ANNOTATIONS
     * @see AxiomsSettings#isSplitAxiomAnnotations()
     * @since 1.3.0
     */
    R setSplitAxiomAnnotations(boolean b);

    /**
     * Sets the ignore read errors option to the desired state.
     *
     * @param b boolean
     * @return {@link R}
     * @see OntSettings#ONT_API_LOAD_CONF_IGNORE_AXIOMS_READ_ERRORS
     * @see AxiomsSettings#isIgnoreAxiomsReadErrors()
     * @since 1.1.0
     */
    R setIgnoreAxiomsReadErrors(boolean b);

}
