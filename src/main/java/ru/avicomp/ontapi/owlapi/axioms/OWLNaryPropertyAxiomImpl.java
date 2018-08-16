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
package ru.avicomp.ontapi.owlapi.axioms;

import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLNaryPropertyAxiom;
import org.semanticweb.owlapi.model.OWLPropertyExpression;
import ru.avicomp.ontapi.jena.utils.Iter;

import java.util.*;
import java.util.stream.Stream;

/**
 * @param <P> the property expression
 * @author Matthew Horridge, The University Of Manchester, Bio-Health Informatics Group
 * @since 1.2.0
 */
public abstract class OWLNaryPropertyAxiomImpl<P extends OWLPropertyExpression> extends OWLPropertyAxiomImpl implements OWLNaryPropertyAxiom<P> {

    protected final List<P> properties;

    /**
     * @param properties  properties
     * @param annotations annotations
     */
    public OWLNaryPropertyAxiomImpl(Collection<? extends P> properties, Collection<OWLAnnotation> annotations) {
        this(Objects.requireNonNull(properties, "properties cannot be null").stream(), annotations);
    }

    /**
     * @param properties  properties
     * @param annotations annotations
     */
    @SuppressWarnings("unchecked")
    private OWLNaryPropertyAxiomImpl(Stream<? extends P> properties, Collection<OWLAnnotation> annotations) {
        super(annotations);
        this.properties = Objects.requireNonNull(properties, "properties cannot be null").filter(Objects::nonNull).distinct().sorted().collect(Iter.toUnmodifiableList());
    }

    @Override
    public Stream<P> properties() {
        return properties.stream();
    }

    @Override
    public List<P> getOperandsAsList() {
        return properties;
    }

    @Override
    public Set<P> getPropertiesMinus(P property) {
        Set<P> props = new LinkedHashSet<>(properties);
        props.remove(property);
        return props;
    }
}
