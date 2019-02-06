/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2019, Avicomp Services, AO
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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntAnnotation;
import ru.avicomp.ontapi.jena.model.OntNAP;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * An {@code OntStatementImpl} with in-memory caches.
 * Can be useful in case some repetitive read operations with annotations are expected.
 * It is read only {@link OntStatement Ontology Statement} implementation: modifying is not allowed.
 * Note: the graph may change during working with instance of this class.
 * <p>
 * Created by @szuev on 13.03.2018.
 * @see CachedAnnotationImpl
 */
@SuppressWarnings("WeakerAccess")
public class CachedStatementImpl extends OntStatementImpl {

    private Resource annotationResourceType;
    private List<OntAnnotation> annotationResources;
    private Set<OntStatement> assertionStatements;
    private final boolean isRoot;

    public CachedStatementImpl(Statement delegate) {
        super(delegate);
        this.isRoot = delegate instanceof OntStatementImpl && ((OntStatementImpl) delegate).isRootStatement();
    }

    @Override
    public boolean isRootStatement() {
        return isRoot;
    }

    @Override
    public OntStatement asRootStatement() {
        if (isRootStatement()) return this;
        throw new OntJenaException.Unsupported("Currently #asRoot transformation is not supported for " + Models.toString(this));
    }

    @Override
    protected ExtendedIterator<OntStatement> listSubjectAssertions() {
        return WrappedIterator.create(getAssertionStatementsAsSet().iterator());
    }

    @Override
    public ExtendedIterator<OntAnnotation> listAnnotationResources() {
        return WrappedIterator.create(getAnnotationList().iterator());
    }

    @Override
    public List<OntAnnotation> getAnnotationList() {
        if (annotationResources != null) {
            return annotationResources;
        }
        return annotationResources = super.getAnnotationList();
    }

    @Override
    protected List<OntAnnotation> getAnnotationResourcesAsList() {
        return super.listAnnotationResources().toList();
    }

    protected Set<OntStatement> getAssertionStatementsAsSet() {
        if (assertionStatements != null) {
            return assertionStatements;
        }
        return assertionStatements = super.listSubjectAssertions()
                .mapWith(x -> (OntStatement) new CachedStatementImpl(x)).toSet();
    }

    @Override
    public boolean hasAnnotations() {
        if (isNotEmpty(annotationResources)) {
            return true;
        }
        if (isNotEmpty(assertionStatements)) {
            return true;
        }
        return !listAnnotations().toSet().isEmpty();
    }

    private static boolean isNotEmpty(Collection<?> list) {
        return list != null && !list.isEmpty();
    }

    @Override
    protected Resource getAnnotationResourceType() {
        if (annotationResourceType != null) {
            return annotationResourceType;
        }
        return annotationResourceType = detectAnnotationRootType(subject);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        return noModify();
    }

    @Override
    public CachedStatementImpl deleteAnnotation(OntNAP property, RDFNode value) {
        return noModify();
    }

    protected <R> R noModify() {
        throw new OntJenaException.Unsupported(Models.toString(this) + ": modifying is not allowed.");
    }

    @Override
    protected OntStatementImpl createRootStatement(OntAnnotation resource) {
        return new CachedStatementImpl(this) {
            @Override
            public boolean isRootStatement() {
                return true;
            }

            @Override
            public List<OntAnnotation> getAnnotationList() {
                return Collections.singletonList(resource);
            }

            @Override
            public ExtendedIterator<OntAnnotation> listAnnotationResources() {
                return Iter.of(resource);
            }
        };
    }

    @Override
    protected OntStatementImpl createBaseStatement(OntAnnotationImpl resource) {
        return new CachedStatementImpl(this) {
            @Override
            public ExtendedIterator<OntStatement> listAnnotations() {
                return resource.listAssertions();
            }

            @Override
            public List<OntAnnotation> getAnnotationList() {
                return Collections.singletonList(resource);
            }

            @Override
            public ExtendedIterator<OntAnnotation> listAnnotationResources() {
                return Iter.of(resource);
            }
        };
    }

    @Override
    protected OntAnnotationImpl wrapAsOntAnnotation(Resource annotation) {
        return new CachedAnnotationImpl(annotation.asNode(), getModel()) {
            @Override
            public OntStatement getBase() {
                return createBaseStatement(this);
            }

            @Override
            protected <R> R noModify() {
                return CachedStatementImpl.this.noModify();
            }
        };
    }

}
