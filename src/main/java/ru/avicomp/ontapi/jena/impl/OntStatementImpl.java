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

import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.PropertyImpl;
import org.apache.jena.rdf.model.impl.StatementImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.WrappedIterator;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link OntStatement Ontology Statement}.
 * This is an extended Jena {@link StatementImpl} with possibility to add, delete and find annotations
 * in the same form of {@code OntStatement}.
 * Annotations can be plain (annotation assertion) or bulk (anonymous resource with
 * {@code rdf:type} {@link OWL#Axiom owl:Axiom} or {@link OWL#Annotation owl:Annotation},
 * for more details see {@link OntAnnotation}).
 * The examples of how to write bulk-annotations in RDF-graph see here:
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>.
 * <p>
 * Created by @szuev on 12.11.2016.
 *
 * @see OntAnnotationImpl
 */
@SuppressWarnings("WeakerAccess")
public class OntStatementImpl extends StatementImpl implements OntStatement {

    public OntStatementImpl(Statement statement) throws ClassCastException, NullPointerException {
        this(statement.getSubject(), statement.getPredicate(), statement.getObject(),
                (OntGraphModel) statement.getModel());
    }

    public OntStatementImpl(Resource subject, Property predicate, RDFNode object, OntGraphModel model) {
        super(subject, predicate, object, (ModelCom) model);
    }

    /**
     * Creates an OntStatement impl from the given Triple.
     * The OntStatement has subject, predicate, and object corresponding to those of Triple.
     *
     * @param t {@link Triple} not null
     * @param m {@link OntGraphModelImpl} model
     * @return {@link OntStatementImpl} fresh instance
     */
    public static OntStatementImpl createOntStatementImpl(Triple t, OntGraphModelImpl m) {
        return createOntStatementImpl(new OntObjectImpl(t.getSubject(), m), t.getPredicate(), t.getObject(), m);
    }

    /**
     * Creates an OntStatement impl with the given SPO.
     *
     * @param s {@link Resource} subject
     * @param p {@link Node Graph RDF URI Node} predicate
     * @param o {@link Node Graph RDF Node} object
     * @param m {@link OntGraphModelImpl} model
     * @return {@link OntStatementImpl} fresh instance
     */
    public static OntStatementImpl createOntStatementImpl(Resource s, Node p, Node o, OntGraphModelImpl m) {
        return createOntStatementImpl(s, new PropertyImpl(p, m), o, m);
    }

    /**
     * Creates an OntStatement impl with the given SPO.
     *
     * @param s {@link Resource} subject
     * @param p {@link Property} predicate
     * @param o {@link Node Graph RDF Node} object
     * @param m {@link OntGraphModelImpl} model
     * @return {@link OntStatementImpl} fresh instance
     */
    public static OntStatementImpl createOntStatementImpl(Resource s, Property p, Node o, OntGraphModelImpl m) {
        return createOntStatementImpl(s, p, createObject(o, m), m);
    }

    /**
     * Creates an OntStatement impl with the given SPO.
     *
     * @param s {@link Resource} subject
     * @param p {@link Property} predicate
     * @param o {@link RDFNode Model RDF Node} object
     * @param m {@link OntGraphModelImpl} model
     * @return {@link OntStatementImpl} fresh instance
     */
    public static OntStatementImpl createOntStatementImpl(Resource s, Property p, RDFNode o, OntGraphModelImpl m) {
        return new OntStatementImpl(s, p, o, m);
    }

    /**
     * Creates a read-only wrapper for the given ont-statement with in-memory caches.
     *
     * @param delegate {@link OntStatement}
     * @return {@link CachedStatementImpl}
     */
    public static CachedStatementImpl createCachedOntStatementImpl(OntStatement delegate) {
        return delegate instanceof CachedStatementImpl ?
                (CachedStatementImpl) delegate : new CachedStatementImpl(delegate);
    }

    /**
     * Creates an ont-statement that does not support sub-annotations.
     * The method does not change the model.
     *
     * @param s {@link Resource} subject
     * @param p {@link Property} predicate
     * @param o {@link RDFNode} object
     * @param m {@link OntGraphModelImpl} model
     * @return {@link OntStatementImpl}
     */
    public static OntStatementImpl createNotAnnotatedOntStatementImpl(Resource s,
                                                                      Property p,
                                                                      RDFNode o,
                                                                      OntGraphModelImpl m) {
        return new OntStatementImpl(s, p, o, m) {
            @Override
            public OntStatement addAnnotation(OntNAP property, RDFNode value) {
                throw new OntJenaException.Unsupported("Sub-annotations are not supported (attempt to annotate " +
                        Models.toString(this) + " with predicate " + m.shortForm(property.getURI()) +
                        " and value " + value + ")");
            }
        };
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @Override
    public boolean isRoot() {
        return isRootStatement();
    }

    public boolean isRootStatement() {
        return false;
    }

    public OntStatement asRootStatement() {
        return isRootStatement() ? this : new OntStatementImpl(getSubject(), getPredicate(), getObject(), getModel()) {

            @Override
            public boolean isRootStatement() {
                return true;
            }
        };
    }

    @Override
    public boolean isLocal() {
        return !getModel().getGraph().getUnderlying().hasSubGraphs() || getModel().isLocal(this);
    }

    public boolean isAnnotationRootStatement() {
        return subject.isAnon() && RDF.type.equals(predicate) &&
                (OWL.Axiom.equals(object) || OWL.Annotation.equals(object));
    }

    @Override
    public OntObject getSubject() {
        return subject instanceof OntObject ? (OntObject) subject : subject.as(OntObject.class);
    }

    public Node getSubjectNode() {
        return subject.asNode();
    }

    @Override
    public <N extends RDFNode> N getSubject(Class<N> type) {
        return subject.as(type);
    }

    /**
     * {@inheritDoc}
     * In case of {@code true}, this method also caches {@link OntAnnotation} in the model.
     *
     * @return boolean
     */
    @Override
    public boolean isBulkAnnotation() {
        return subject.isAnon() && getModel().findNodeAs(subject.asNode(), OntAnnotation.class) != null;
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        OntJenaException.notNull(property, "Null property.");
        OntJenaException.notNull(value, "Null value.");
        if (isRootStatement()) {
            model.add(getSubject(), OntJenaException.notNull(property, "Null property."),
                    OntJenaException.notNull(value, "Null value."));
            return getModel().createStatement(getSubject(), property, value);
        }
        return asAnnotationResource()
                .orElseGet(() -> OntAnnotationImpl.createAnnotation(getModel(),
                        OntStatementImpl.this, getAnnotationResourceType()))
                .addAnnotation(property, value);
    }

    @Override
    public Stream<OntStatement> annotations() {
        return Iter.asStream(listAnnotations());
    }

    /**
     * Lists all annotation assertion statements related to this one.
     *
     * @return {@link ExtendedIterator} of {@link OntStatement}s
     * @see #annotations()
     * @since 1.3.0
     */
    public ExtendedIterator<OntStatement> listAnnotations() {
        ExtendedIterator<OntStatement> res = Iter.flatMap(listAnnotationResources(),
                a -> ((OntAnnotationImpl) a).listAssertions());
        if (isRootStatement()) {
            return listSubjectAssertions().andThen(res);
        }
        return res;
    }

    protected ExtendedIterator<OntStatement> listSubjectAssertions() {
        return ((OntObjectImpl) getSubject()).listAssertions();
    }

    @Override
    public OntStatementImpl deleteAnnotation(OntNAP property, RDFNode value) {
        OntJenaException.notNull(property, "Null property.");
        OntJenaException.notNull(value, "Null value.");
        OntGraphModelImpl model = getModel();
        if (isRootStatement()) {
            Set<OntStatement> assertions = model.statements(getSubject(), property, value).collect(Collectors.toSet());
            if (isAnnotationRootStatement()) {
                // if it is anon bulk annotation root statement,
                // deletion can break down the structure of annotation object
                throw new OntJenaException("Direct removing assertions from Annotation resource is prohibited");
            }
            assertions.forEach(model::remove);
        }
        Set<OntStatement> candidates = annotationResources()
                .flatMap(OntAnnotation::assertions)
                .filter(s -> Objects.equals(property, s.getPredicate()))
                .filter(s -> Objects.equals(value, s.getObject()))
                .collect(Collectors.toSet());
        if (candidates.isEmpty()) {
            return this;
        }
        Set<OntStatement> delete = candidates.stream()
                .filter(s -> !s.hasAnnotations()).collect(Collectors.toSet());
        if (delete.isEmpty()) {
            throw new OntJenaException("Can't delete [*, " + property + ", " + value + "]: " +
                    "candidates have their own annotations which should be deleted first.");
        }

        delete.forEach(model::remove);
        // delete empty owl:Axiom or owl:Annotation sections
        Set<OntAnnotation> empty = annotationResources()
                .filter(f -> Objects.equals(f.listProperties().toSet().size(), OntAnnotationImpl.SPEC.size()))
                .collect(Collectors.toSet());
        empty.forEach(a -> {
            model.removeAll(a, null, null);
            // anon resource no need anymore:
            model.getNodeCache().remove(a.asNode());
        });
        return this;
    }

    @Override
    public Stream<OntAnnotation> annotationResources() {
        return Iter.asStream(listAnnotationResources());
    }

    /**
     * Returns the iterator of annotation objects attached to this statement.
     *
     * @return {@link ExtendedIterator} of {@link OntAnnotation}s
     * @see #annotationResources()
     */
    public ExtendedIterator<OntAnnotation> listAnnotationResources() {
        return listAnnotationResources(getModel(), getAnnotationResourceType(), this)
                .mapWith(this::wrapAsOntAnnotation);
    }

    /**
     * Returns the {@code List} of annotations sorted by the some internal order.
     *
     * @return List of {@link OntAnnotation}s
     * @see #listAnnotationResources()
     * @see OntAnnotationImpl#DEFAULT_ANNOTATION_COMPARATOR
     */
    @Override
    public List<OntAnnotation> getAnnotationList() {
        List<OntAnnotation> res = getAnnotationResourcesAsList();
        res.sort(OntAnnotationImpl.DEFAULT_ANNOTATION_COMPARATOR);
        return res;
    }

    protected List<OntAnnotation> getAnnotationResourcesAsList() {
        return listAnnotationResources().toList();
    }

    /**
     * Returns the {@code rdf:type} of the attached annotation objects.
     *
     * @return {@link OWL#Axiom {@code owl:Axiom}} or {@link OWL#Annotation {@code owl:Annotation}}
     */
    protected Resource getAnnotationResourceType() {
        return detectAnnotationRootType(subject);
    }

    /**
     * Returns annotation objects corresponding to the given statement and rdf-type.
     * For internal usage.
     *
     * @param m {@link Model}
     * @param t {@link OWL#Axiom owl:Axiom} or {@link OWL#Annotation owl:Annotation}
     * @param s {@link Statement}, the base statement
     * @return {@link ExtendedIterator} of {@link OntAnnotation}s
     */
    public static ExtendedIterator<Resource> listAnnotationResources(Model m, Resource t, Statement s) {
        return listAnnotationResources(m, t, s.getSubject(), s.getPredicate(), s.getObject());
    }

    /**
     * Lists all (bulk) annotation anonymous resources form the specified model, type and SPO.
     *
     * @param m {@link Model}
     * @param t {@link Resource} either {@link OWL#Axiom owl:Axiom} or {@link OWL#Annotation owl:Annotation}
     * @param s {@link Resource} subject
     * @param p {@link Property} predicate
     * @param o {@link RDFNode} object
     * @return {@link ExtendedIterator} of {@link Resource}s
     */
    public static ExtendedIterator<Resource> listAnnotationResources(Model m,
                                                                     Resource t,
                                                                     Resource s,
                                                                     Property p,
                                                                     RDFNode o) {
        return m.listStatements(null, OWL.annotatedSource, s)
                .filterKeep(x -> hasSpecProperties(x, t, p, o))
                .mapWith(Statement::getSubject);
    }

    private static boolean hasSpecProperties(Statement s, Resource t, Property p, RDFNode o) {
        Resource r = s.getSubject();
        return r.hasProperty(RDF.type, t)
                && r.hasProperty(OWL.annotatedProperty, p)
                && r.hasProperty(OWL.annotatedTarget, o);
    }

    /**
     * Determines the annotation type.
     * Root annotations (including some anon-axioms bodies) go with the type owl:Axiom {@link OWL#Axiom},
     * sub-annotations have type owl:Annotation.
     *
     * @param s {@link Resource} the subject resource to test
     * @return {@link OWL#Axiom} or {@link OWL#Annotation}
     */
    protected static Resource detectAnnotationRootType(Resource s) {
        Model m = s.getModel();
        if (s.isAnon() && OntAnnotationImpl.ROOT_TYPES.stream().anyMatch(t -> m.contains(s, RDF.type, t))) {
            return OWL.Annotation;
        }
        return OWL.Axiom;
    }

    /**
     * Splits he statement into several equivalent ones but with disjoint annotations.
     * Warning: this method stores annotation-resources to memory.
     *
     * @return Stream of {@link OntStatement}s
     */
    public Stream<OntStatement> split() {
        return Iter.asStream(listSplitStatements());
    }

    public ExtendedIterator<OntStatement> listSplitStatements() {
        List<OntAnnotation> res = getAnnotationList();
        if (res.size() < 2) {
            return Iter.of(this);
        }
        if (isRootStatement()) {
            OntStatement first = createRootStatement(res.remove(0));
            return Iter.of(first).andThen(WrappedIterator.create(res.iterator()).mapWith(OntAnnotation::getBase));
        }
        return WrappedIterator.create(res.iterator()).mapWith(OntAnnotation::getBase);
    }

    protected OntStatementImpl createRootStatement(OntAnnotation resource) {
        return new OntStatementImpl(this) {
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

    protected OntStatementImpl createBaseStatement(OntAnnotationImpl resource) {
        return new OntStatementImpl(this) {
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

    /**
     * Wraps the given resource as {@link OntAnnotationImpl Ontology Annotation Implementation} with reference to itself.
     * The base statement of this annotation resource equals to this statement, but with several restrictions:
     * 1) all its annotations are assertions of the annotation resource,
     * 2) it is not possible to list all other annotation resources with except of the given resource.
     *
     * @param annotation {@link Resource}
     * @return {@link OntAnnotationImpl}
     */
    protected OntAnnotationImpl wrapAsOntAnnotation(Resource annotation) {
        return new OntAnnotationImpl(annotation.asNode(), getModel()) {
            @Override
            public OntStatement getBase() {
                return createBaseStatement(this);
            }
        };
    }
}
