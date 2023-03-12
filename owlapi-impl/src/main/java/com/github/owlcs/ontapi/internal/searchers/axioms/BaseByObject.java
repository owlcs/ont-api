/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2023, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal.searchers.axioms;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.ByObjectSearcher;
import com.github.owlcs.ontapi.internal.WriteHelper;
import com.github.owlcs.ontapi.internal.objects.ONTExpressionImpl;
import com.github.owlcs.ontapi.internal.searchers.WithRootStatement;
import com.github.owlcs.ontapi.jena.model.OntEntity;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.semanticweb.owlapi.model.OWLAnnotationSubject;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;

/**
 * Created by @ssz on 18.04.2020.
 */
@SuppressWarnings("SameParameterValue")
abstract class BaseByObject<A extends OWLAxiom, O extends OWLObject> extends WithRootStatement implements ByObjectSearcher<A, O> {

    static Resource getRDFType(OWLEntity entity) {
        return WriteHelper.getRDFType(entity);
    }

    static Class<? extends OntEntity> getClassType(OWLEntity entity) {
        return WriteHelper.getEntityType(entity);
    }

    static Resource asResource(OWLEntity entity) {
        return WriteHelper.toResource(entity.getIRI());
    }

    static Resource asResource(OWLIndividual individual) {
        return WriteHelper.toResource(individual);
    }

    static Resource asResource(OWLAnnotationSubject subject) {
        return WriteHelper.toResource(subject);
    }

    static Resource asResource(OWLClass clazz) {
        return asResource((OWLEntity) clazz);
    }

    static Resource asResource(OWLClassExpression clazz) {
        if (clazz.isOWLClass()) return asResource(clazz.asOWLClass());
        if (clazz instanceof ONTExpressionImpl) {
            return new ResourceImpl(((ONTExpressionImpl<?>) clazz).asNode(), null);
        }
        throw new OntApiException.Unsupported("Unsupported class-expression " + clazz);
    }

    static Resource asResource(OWLObjectPropertyExpression property) {
        if (property.isOWLObjectProperty()) return asResource((OWLEntity) property.asOWLObjectProperty());
        if (property instanceof ONTExpressionImpl) {
            return new ResourceImpl(((ONTExpressionImpl<?>) property).asNode(), null);
        }
        throw new OntApiException.Unsupported("Unsupported object-expression " + property);
    }

    public static boolean isSupported(OWLClassExpression clazz) {
        return clazz.isOWLClass() || clazz instanceof ONTExpressionImpl;
    }

    public static boolean isSupported(OWLObjectPropertyExpression property) {
        return property.isOWLObjectProperty() || property instanceof ONTExpressionImpl;
    }
}
