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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An OntStatementImpl with in-memory cache.
 * Can be useful in case some repetitive operations with annotations are expected.
 * Experimental.
 * Warning: graph may change during working with this instance.
 * <p>
 * Created by @szuev on 13.03.2018.
 */
@SuppressWarnings("WeakerAccess")
public class CachedStatementImpl extends OntStatementImpl {
    private Resource annotationResourceType;
    private List<OntAnnotation> resources;
    private Set<OntStatement> assertions;
    private final boolean root;

    public CachedStatementImpl(Statement delegate) {
        super(delegate);
        this.root = delegate instanceof OntStatementImpl && ((OntStatementImpl) delegate).isRootStatement();
    }

    @Override
    public boolean isRootStatement() {
        return root;
    }

    @Override
    public OntStatement asRootStatement() {
        if (isRootStatement()) return this;
        throw new OntJenaException.Unsupported("Currently #asRoot transformation is not supported for " + Models.toString(this));
    }

    protected void clear() {
        resources = null;
        assertions = null;
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        clear();
        return new CachedStatementImpl(super.addAnnotation(property, value));
    }

    @Override
    public Stream<OntStatement> annotations() {
        return getAssertions().stream();
    }

    public Set<OntStatement> getAssertions() {
        return assertions == null ? assertions = super.annotations().map(CachedStatementImpl::new).collect(Collectors.toSet()) : assertions;
    }

    @Override
    public void deleteAnnotation(OntNAP property, RDFNode value) {
        clear();
        super.deleteAnnotation(property, value);
    }

    @Override
    public Stream<OntAnnotation> annotationResources() {
        return getSortedAnnotations().stream();
    }

    @Override
    public List<OntAnnotation> getSortedAnnotations() {
        if (resources != null) return resources;
        return resources = findOntAnnotationResources(this, getAnnotationResourceType(), CachedOntAnnImpl::new)
                .sorted(OntAnnotationImpl.DEFAULT_ANNOTATION_COMPARATOR).collect(Collectors.toList());
    }

    @Override
    public Optional<OntAnnotation> asAnnotationResource() {
        List<OntAnnotation> res = this.getSortedAnnotations();
        return res.isEmpty() ? Optional.empty() : Optional.of(res.get(0));
    }

    @Override
    public boolean hasAnnotations() {
        return !getAssertions().isEmpty();
    }

    @Override
    protected Resource getAnnotationResourceType() {
        return annotationResourceType == null ? annotationResourceType = detectAnnotationRootType(getSubject()) : annotationResourceType;
    }

    protected class CachedOntAnnImpl extends AttachedAnnotationImpl {
        private Set<OntStatement> assertions;

        public CachedOntAnnImpl(Resource subject, OntStatementImpl base) {
            super(subject, base);
        }

        protected void clear() {
            assertions = null;
        }

        @Override
        public Stream<OntStatement> assertions() {
            return (assertions == null ? assertions = super.assertions().map(CachedStatementImpl::new).collect(Collectors.toSet()) : assertions).stream();
        }

        @Override
        public OntStatement addAnnotation(OntNAP property, RDFNode value) {
            clear();
            return new CachedStatementImpl(super.addAnnotation(property, value));
        }

        @Override
        public Resource removeProperties() {
            throw new OntJenaException.Unsupported();
        }

        @Override
        public Resource removeAll(Property p) {
            clear();
            return super.removeAll(p);
        }
    }
}
