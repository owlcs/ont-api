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

package com.github.owlcs.ontapi;

import com.github.owlcs.ontapi.config.OntLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyID;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;

/**
 * A technical interface to provide an {@link Adapter} instance.
 * Created by @ssz on 03.06.2019.
 */
interface HasAdapter {

    /**
     * Gets an adapter instance.
     *
     * @return {@link Adapter}, must not be {@code null}
     */
    default Adapter getAdapter() {
        return OWLAdapter.get();
    }

    /**
     * An adapter to represent or transform OWL-API types into the ONT-API compatible form.
     */
    interface Adapter {

        /**
         * Performs mapping {@link OWLOntologyID} to {@link ID}.
         *
         * @param id {@link OWLOntologyID}, not {@code null}
         * @return {@link ID}
         */
        ID asONT(OWLOntologyID id);

        /**
         * Performs mapping {@link OWLOntologyLoaderConfiguration} to {@link OntLoaderConfiguration}.
         *
         * @param conf {@link OWLOntologyLoaderConfiguration}, not {@code null}
         * @return {@link OntLoaderConfiguration}
         */
        OntLoaderConfiguration asONT(OWLOntologyLoaderConfiguration conf);

        /**
         * Performs mapping {@link OWLOntologyManager} to {@link OntologyManager}.
         * @param manager {@link OWLOntologyManager}, not {@code null}
         * @return {@link OntologyManager}
         */
        OntologyManager asONT(OWLOntologyManager manager);

    }
}
