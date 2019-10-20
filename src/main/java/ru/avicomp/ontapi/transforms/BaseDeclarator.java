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

package ru.avicomp.ontapi.transforms;

import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.transforms.vocabulary.AVC;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The collection of base methods for {@link ManifestDeclarator} and {@link ReasonerDeclarator}
 */
@SuppressWarnings("WeakerAccess")
public abstract class BaseDeclarator extends Transform {
    private static final List<Property> RESTRICTION_PROPERTY_MARKERS = Stream.of(OWL.onProperty, OWL.allValuesFrom,
            OWL.someValuesFrom, OWL.hasValue, OWL.onClass,
            OWL.onDataRange, OWL.cardinality, OWL.qualifiedCardinality,
            OWL.maxCardinality, OWL.maxQualifiedCardinality, OWL.minCardinality,
            OWL.maxQualifiedCardinality, OWL.onProperties).collect(Iter.toUnmodifiableList());

    private static final List<Property> ANONYMOUS_CLASS_MARKERS = Stream.of(OWL.intersectionOf, OWL.oneOf,
            OWL.unionOf, OWL.complementOf).collect(Iter.toUnmodifiableList());

    protected BaseDeclarator(Graph graph) {
        super(graph);
    }

    protected ExtendedIterator<Resource> subjectAndObjects(Statement s) {
        return Iter.concat(Iter.of(s.getSubject()), s.getObject().as(RDFList.class).iterator()
                .filterKeep(RDFNode::isResource)
                .mapWith(RDFNode::asResource));
    }

    protected ExtendedIterator<Resource> members(Resource subject, Property predicate) {
        return members(subject, predicate, Resource.class);
    }

    @SuppressWarnings("SameParameterValue")
    protected <X extends RDFNode> ExtendedIterator<X> members(Resource subject,
                                                              Property predicate,
                                                              Class<X> type) {
        return Iter.flatMap(subject.listProperties(predicate)
                        .mapWith(Statement::getObject).filterKeep(s -> s.canAs(RDFList.class)),
                m -> m.as(RDFList.class).iterator())
                .filterKeep(x -> x.canAs(type))
                .mapWith(x -> x.as(type));
    }

    protected Resource getObjectResource(Resource subject, Property predicate) {
        Statement res = subject.getProperty(predicate);
        return res != null && res.getObject().isResource() ? res.getObject().asResource() : null;
    }

    @SuppressWarnings("SameParameterValue")
    protected Literal getObjectLiteral(Resource subject, Property predicate) {
        Statement res = subject.getProperty(predicate);
        return res != null && res.getObject().isLiteral() ? res.getObject().asLiteral() : null;
    }

    protected boolean isClassExpression(Resource candidate) {
        return builtins.classes().contains(candidate) || hasType(candidate, OWL.Class) || hasType(candidate, OWL.Restriction);
    }

    protected boolean isClass(Resource candidate) {
        return (candidate.isURIResource() && hasType(candidate, OWL.Class)) || builtins.classes().contains(candidate);
    }

    protected boolean isDataRange(Resource candidate) {
        return builtins.datatypes().contains(candidate) || hasType(candidate, RDFS.Datatype);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    protected boolean isObjectPropertyExpression(Resource candidate) {
        return builtins.objectProperties().contains(candidate)
                || hasType(candidate, OWL.ObjectProperty)
                || candidate.hasProperty(OWL.inverseOf);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    protected boolean isDataProperty(Resource candidate) {
        return builtins.datatypeProperties().contains(candidate) || hasType(candidate, OWL.DatatypeProperty);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    protected boolean isAnnotationProperty(Resource candidate) {
        return builtins.annotationProperties().contains(candidate) || hasType(candidate, OWL.AnnotationProperty);
    }

    protected boolean isIndividual(Resource candidate) {
        return hasType(candidate, OWL.NamedIndividual) || hasType(candidate, AVC.AnonymousIndividual);
    }

    protected BaseDeclarator declareObjectProperty(Resource resource) {
        declareObjectProperty(resource, builtins.objectProperties());
        return this;
    }

    protected BaseDeclarator declareDataProperty(Resource resource) {
        declareDataProperty(resource, builtins.datatypeProperties());
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    protected BaseDeclarator declareAnnotationProperty(Resource resource) {
        declareAnnotationProperty(resource, builtins.annotationProperties());
        return this;
    }

    protected void declareObjectProperty(Resource resource, Set<? extends Resource> builtIn) {
        if (resource.isAnon()) {
            undeclare(resource, OWL.ObjectProperty);
            return;
        }
        declare(resource, OWL.ObjectProperty, builtIn);
    }

    protected void declareDataProperty(Resource resource, Set<? extends Resource> builtIn) {
        declare(resource, OWL.DatatypeProperty, builtIn);
    }

    protected void declareAnnotationProperty(Resource resource, Set<? extends Resource> builtIn) {
        declare(resource, OWL.AnnotationProperty, builtIn);
    }

    protected BaseDeclarator declareIndividual(Resource resource) {
        if (resource.isAnon()) {
            // test data from owl-api-contact contains such things also:
            undeclare(resource, OWL.NamedIndividual);
            // the temporary declaration:
            declare(resource, AVC.AnonymousIndividual);
        } else {
            declare(resource, OWL.NamedIndividual);
        }
        return this;
    }

    protected BaseDeclarator declareDatatype(Resource resource) {
        declare(resource, RDFS.Datatype, builtins.datatypes());
        return this;
    }

    protected boolean declareClass(Resource resource) {
        if (builtins.classes().contains(resource)) {
            return true;
        }
        if (builtins.datatypes().contains(resource)) {
            return false;
        }
        Resource type = resource.isURIResource() ? OWL.Class :
                containsClassExpressionProperty(resource) ? OWL.Class :
                        containsRestrictionProperty(resource) ? OWL.Restriction : null;
        if (type != null) {
            declare(resource, type);
            return true;
        }
        return false;
    }

    protected boolean containsClassExpressionProperty(Resource candidate) {
        return hasAnyPredicate(candidate, ANONYMOUS_CLASS_MARKERS);
    }

    protected boolean containsRestrictionProperty(Resource candidate) {
        return hasAnyPredicate(candidate, RESTRICTION_PROPERTY_MARKERS);
    }

    protected void declare(Resource subject, Resource type, Set<? extends Resource> forbidden) {
        if (type == null || forbidden.contains(subject)) {
            return;
        }
        declare(subject, type);
    }

    @Override
    protected BaseDeclarator declare(Resource s, Resource t) {
        super.declare(s, t);
        return this;
    }

}
