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

package com.github.owlcs.ontapi.internal.objects;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.model.OntModel;
import com.github.owlcs.ontapi.jena.model.OntNAP;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDFS;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;

import java.util.Set;
import java.util.function.Supplier;

/**
 * An {@link OWLAnnotationProperty} implementation that is also {@link ONTObject}.
 * Created by @ssz on 09.08.2019.
 *
 * @see com.github.owlcs.ontapi.owlapi.objects.entity.OWLAnnotationPropertyImpl
 * @since 2.0.0
 */
public class ONTAnnotationPropertyImpl extends ONTEntityImpl<OWLAnnotationProperty> implements OWLAnnotationProperty {

    public ONTAnnotationPropertyImpl(String uri, Supplier<OntModel> m) {
        super(uri, m);
    }

    /**
     * Using the {@code factory} finds or creates an {@link OWLAnnotationProperty} instance.
     *
     * @param uri     {@code String}, not {@code null}
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @param model   a {@code Supplier} with a {@link OntModel},
     *                which is only used in case the {@code factory} has no reference to a model
     * @return an {@link ONTObject} that is {@link OWLAnnotationProperty}
     */
    public static ONTObject<OWLAnnotationProperty> find(String uri,
                                                        InternalObjectFactory factory,
                                                        Supplier<OntModel> model) {
        if (factory instanceof ModelObjectFactory) {
            return ((ModelObjectFactory) factory).getAnnotationProperty(uri);
        }
        return factory.getProperty(OntApiException.mustNotBeNull(model.get().getAnnotationProperty(uri)));
    }

    @Override
    public OntNAP asRDFNode() {
        return as(OntNAP.class);
    }

    @Override
    public Set<OWLAnnotationProperty> getAnnotationPropertySet() {
        return createSet(this);
    }

    @Override
    public boolean isComment() {
        return equals(RDFS.comment);
    }

    @Override
    public boolean isLabel() {
        return equals(RDFS.label);
    }

    @Override
    public boolean isDeprecated() {
        return equals(OWL.deprecated);
    }

    @Override
    public boolean isAnnotationProperty() {
        return true;
    }
}
