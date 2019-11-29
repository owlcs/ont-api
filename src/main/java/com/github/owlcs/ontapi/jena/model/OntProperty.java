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

package com.github.owlcs.ontapi.jena.model;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;

/**
 * Named Ontology property: {@link OntNAP}, {@link OntNDP} and {@link OntNOP}.
 * <p>
 * Created by @ssz on 20.01.2019.
 *
 * @param <P> subtype of {@link OntProperty}
 * @since 1.4.0
 */
public interface OntProperty<P extends OntProperty<P>> extends OntEntity, Property {

    /**
     * @see Property#isProperty()
     */
    @Override
    default boolean isProperty() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default P addComment(String txt) {
        return addComment(txt, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default P addComment(String txt, String lang) {
        return annotate(getModel().getRDFSComment(), txt, lang);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default P addLabel(String txt) {
        return addLabel(txt, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default P addLabel(String txt, String lang) {
        return annotate(getModel().getRDFSLabel(), txt, lang);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default P annotate(OntNAP predicate, String txt, String lang) {
        return annotate(predicate, getModel().createLiteral(txt, lang));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    default P annotate(OntNAP predicate, RDFNode value) {
        addAnnotation(predicate, value);
        return (P) this;
    }
}
