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
 *
 */

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.jena.rdf.model.impl.StatementImpl;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * An Ont Statement.
 * This is an extended Jena {@link Statement} with possibility to add annotations in the same form of ont-statements.
 * Annotations can be plain (annotation assertion) or bulk (anonymous resource with rdf:type owl:Axiom or owl:Annotation, see {@link OntAnnotation}).
 * The examples of how to write bulk-annotations in RDF-graph see here:
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>.
 * <p>
 * Created by @szuev on 12.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntStatementImpl extends StatementImpl implements OntStatement {
    private int hashCode;
    private Resource annotationResourceType;

    public OntStatementImpl(Resource subject, Property predicate, RDFNode object, OntGraphModel model) {
        super(subject, predicate, object, (ModelCom) model);
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) super.getModel();
    }

    @Override
    public boolean isRoot() {
        return false;
    }

    @Override
    public boolean isLocal() {
        return !getModel().getGraph().getUnderlying().hasSubGraphs() || getModel().isInBaseModel(this);
    }

    @Override
    public OntObject getSubject() {
        return subject instanceof OntObject ? (OntObject) subject : subject.as(OntObject.class);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Resource root = asAnnotationResource().orElseGet(() -> createAnnotationObject(OntStatementImpl.this, getAnnotationResourceType()));
        root.addProperty(property, value);
        return getModel().createOntStatement(false, root, property, value);
    }

    @Override
    public Stream<OntStatement> annotations() {
        return asAnnotationResource().map(OntAnnotation::assertions).orElseGet(Stream::empty);
    }

    @Override
    public int hashCode() {
        return hashCode != 0 ? hashCode : (hashCode = super.hashCode());
    }

    @Override
    public boolean hasAnnotations() {
        Optional<OntAnnotation> root = asAnnotationResource();
        if (root.isPresent()) {
            try (Stream<OntStatement> assertions = root.get().assertions()) {
                return assertions.findAny().isPresent();
            }
        }
        return false;
    }

    @Override
    public void deleteAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Optional<OntAnnotation> root = asAnnotationResource();
        if (!root.isPresent()) return;
        if (getModel().contains(root.get(), property, value)) {
            OntStatement res = getModel().createOntStatement(false, root.get(), property, value);
            if (res.hasAnnotations()) {
                throw new OntJenaException("Can't delete " + res + ": it has children");
            }
            getModel().removeAll(root.get(), property, value);
        }
        if (root.get().assertions().findAny().isPresent())
            return;
        // if no children remove whole parent section.
        getModel().removeAll(root.get(), null, null);
    }

    protected void checkAnnotationInput(OntNAP property, RDFNode value) {
        OntJenaException.notNull(property, "Null property.");
        if (OntJenaException.notNull(value, "Null value.").isResource()) {
            if (value.isURIResource()) return;
            if (value.canAs(OntIndividual.Anonymous.class)) return;
            throw new OntJenaException("Incorrect resource specified " + value + ": should be either uri-resource or anonymous individual.");
        } else if (value.isLiteral()) {
            return;
        }
        throw new OntJenaException("Should never happen.");
    }

    @Override
    public Stream<OntAnnotation> annotationResources() {
        return annotationObjects(this, getAnnotationResourceType());
    }

    /**
     * Returns annotation objects corresponding the given statement and rdfs-type
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return Stream of {@link OntAnnotation}
     */
    protected static Stream<OntAnnotation> annotationObjects(OntStatementImpl base, Resource type) {
        return Iter.asStream(base.getModel().listSubjectsWithProperty(OWL.annotatedSource, base.getSubject()))
                .filter(r -> r.hasProperty(RDF.type, type))
                .filter(r -> r.hasProperty(OWL.annotatedProperty, base.getPredicate()))
                .filter(r -> r.hasProperty(OWL.annotatedTarget, base.getObject()))
                .map(r -> new AttachedAnnotationImpl(r, base));
    }

    /**
     * An {@link OntAnnotationImpl} with reference to itself.
     */
    private static class AttachedAnnotationImpl extends OntAnnotationImpl {
        private final OntStatementImpl base;

        public AttachedAnnotationImpl(Resource subject, OntStatementImpl base) {
            super(subject.asNode(), base.getModel());
            this.base = base;
        }

        @Override
        public OntStatement getBase() {
            return new OntStatementImpl(base.getSubject(), base.getPredicate(), base.getObject(), getModel()) {
                @Override
                public boolean hasAnnotations() {
                    return true;
                }

                @Override
                public Optional<OntAnnotation> asAnnotationResource() {
                    return Optional.of(AttachedAnnotationImpl.this);
                }
            };
        }
    }

    /**
     * Creates the new annotation section (resource).
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return {@link OntAnnotation} the anonymous resource with specified type.
     */
    protected static OntAnnotation createAnnotationObject(OntStatementImpl base, Resource type) {
        Resource res = base.getModel().createResource();
        res.addProperty(RDF.type, type);
        res.addProperty(OWL.annotatedSource, base.getSubject());
        res.addProperty(OWL.annotatedProperty, base.getPredicate());
        res.addProperty(OWL.annotatedTarget, base.getObject());
        return res.as(OntAnnotation.class);
    }

    /**
     * Determines the annotation type.
     * Root annotations (including some anon-axioms bodies) go with the type owl:Axiom {@link OWL#Axiom},
     * sub-annotations have type owl:Annotation.
     *
     * @param subject {@link Resource} the subject resource to test
     * @return {@link OWL#Axiom} or {@link OWL#Annotation}
     */
    protected static Resource detectAnnotationRootType(OntObject subject) {
        if (subject.isAnon() &&
                subject.types().anyMatch(t -> OWL.Axiom.equals(t) || OWL.Annotation.equals(t) || OntAnnotationImpl.EXTRA_ROOT_TYPES.contains(t))) {
            return OWL.Annotation;
        }
        return OWL.Axiom;
    }

    /**
     * Returns the rdf:type of attached annotation objects.
     *
     * @return {@link OWL#Axiom {@code owl:Axiom}} or {@link OWL#Annotation {@code owl:Annotation}}
     */
    protected Resource getAnnotationResourceType() {
        return annotationResourceType == null ? annotationResourceType = detectAnnotationRootType(getSubject()) : annotationResourceType;
    }

    /**
     * The class-implementation of the root statement.
     * New annotations comes in the form of plain annotation assertions
     * while in the base {@link OntStatement} there would be {@link OntAnnotation} resources.
     *
     * @see OntObject#getRoot
     */
    public static class RootImpl extends OntStatementImpl {

        public RootImpl(Resource subject, Property predicate, RDFNode object, OntGraphModel model) {
            super(subject, predicate, object, model);
        }

        @Override
        public boolean isRoot() {
            return true;
        }

        @Override
        public OntStatement addAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            getModel().add(getSubject(), property, value);
            return getModel().createOntStatement(false, getSubject(), property, value);
        }

        private Stream<Statement> properties() {
            return Iter.asStream(getSubject().listProperties());
        }

        @Override
        public Stream<OntStatement> annotations() {
            Stream<OntStatement> res = properties()
                    .filter(s -> s.getPredicate().canAs(OntNAP.class))
                    .map(s -> getModel().createOntStatement(false, s.getSubject(), s.getPredicate(), s.getObject()));
            return Stream.concat(res, super.annotations());
        }

        @Override
        public boolean hasAnnotations() {
            try (Stream<Statement> statements = properties()) {
                return statements.map(Statement::getPredicate).anyMatch(p -> p.canAs(OntNAP.class)) || super.hasAnnotations();
            }
        }

        @Override
        public void deleteAnnotation(OntNAP property, RDFNode value) {
            checkAnnotationInput(property, value);
            getModel().removeAll(getSubject(), property, value);
            super.deleteAnnotation(property, value);
        }
    }

}
