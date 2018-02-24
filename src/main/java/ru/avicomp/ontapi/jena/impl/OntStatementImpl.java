/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import org.apache.jena.graph.FrontsNode;
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
 * This is extended Jena {@link Statement} with possibility to add annotations in the same form of ont-statement.
 * Annotations could be plain (annotation assertion) or bulk (anonymous resource with rdf:type owl:Axiom or owl:Annotation).
 * The examples of how to write bulk-annotations in RDF-graph see here:
 * <a href='https://www.w3.org/TR/owl2-mapping-to-rdf/#Translation_of_Annotations'>2.2 Translation of Annotations</a>
 * <p>
 * Created by @szuev on 12.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntStatementImpl extends StatementImpl implements OntStatement {

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
        return super.getSubject().as(OntObject.class);
    }

    @Override
    public OntStatement addAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Resource type = getAnnotationRootType(getSubject());
        Resource root = findAnnotationObject(this, type).orElseGet(() -> createAnnotationObject(OntStatementImpl.this, type));
        root.addProperty(property, value);
        return new OntStatementImpl(root, property, value, getModel());
    }

    @Override
    public Stream<OntStatement> annotations() {
        return asAnnotationResource().map(OntAnnotation::assertions).orElse(Stream.empty());
    }

    @Override
    public boolean hasAnnotations() {
        Optional<OntAnnotation> root = asAnnotationResource();
        return root.isPresent() && root.get().assertions().findFirst().isPresent();
    }

    @Override
    public void deleteAnnotation(OntNAP property, RDFNode value) {
        checkAnnotationInput(property, value);
        Optional<OntAnnotation> root = asAnnotationResource();
        if (!root.isPresent()) return;
        if (getModel().contains(root.get(), property, value)) {
            OntStatement res = new OntStatementImpl(root.get(), property, value, getModel());
            if (res.hasAnnotations()) {
                throw new OntJenaException("Can't delete " + res + ": it has children");
            }
            getModel().removeAll(root.get(), property, value);
        }
        if (root.get().assertions().count() == 0) { // if no children remove whole parent section.
            getModel().removeAll(root.get(), null, null);
        }
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
    public Optional<OntAnnotation> asAnnotationResource() {
        return findAnnotationObject(this, getAnnotationRootType(getSubject()));
    }

    /**
     * Finds the annotation object corresponding the given statement and rdfs-type
     *
     * @param base base ont-statement
     * @param type owl:Axiom or owl:Annotation
     * @return Optional around the {@link OntAnnotation}
     */
    protected static Optional<OntAnnotation> findAnnotationObject(OntStatementImpl base, Resource type) {
        try (Stream<Resource> subjects = Iter.asStream(base.getModel().listSubjectsWithProperty(OWL.annotatedSource, base.getSubject()))) {
            return subjects.filter(r -> r.hasProperty(RDF.type, type))
                    .filter(r -> r.hasProperty(OWL.annotatedProperty, base.getPredicate()))
                    .filter(r -> r.hasProperty(OWL.annotatedTarget, base.getObject()))
                    //.map(r -> r.as(OntAnnotation.class))
                    .map(FrontsNode::asNode)
                    .map(r -> base.getModel().getNodeAs(r, OntAnnotation.class))
                    .findFirst();
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
     * Root annotations (including some anon-axioms bodies) go with the type owl:Axiom {@link OWL#Axiom}
     *
     * @param subject {@link Resource} the subject resource to test
     * @return {@link OWL#Axiom} or {@link OWL#Annotation}
     */
    public static Resource getAnnotationRootType(Resource subject) {
        return subject.isAnon() && subject.canAs(OntAnnotation.class) ? OWL.Annotation : OWL.Axiom;
    }

    /**
     * The class-implementation of the root statement.
     * The new annotations comes in the form of plain annotation-assertions
     * while in the base {@link OntStatement} the would be {@link OntAnnotation} resource.
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
            return new OntStatementImpl(getSubject(), property, value, getModel());
        }

        @Override
        public Stream<OntStatement> annotations() {
            Stream<OntStatement> res = Iter.asStream(getModel()
                    .listStatements(getSubject(), null, (RDFNode) null))
                    .filter(s -> s.getPredicate().canAs(OntNAP.class))
                    .map(s -> new OntStatementImpl(s.getSubject(), s.getPredicate().as(OntNAP.class), s.getObject(), getModel()));
            return Stream.concat(res, super.annotations());
        }

        @Override
        public boolean hasAnnotations() {
            try (Stream<Statement> statements = Iter.asStream(getSubject().listProperties())) {
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
