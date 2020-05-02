/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2020, The University of Manchester, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.jena.impl.Entities;
import com.github.owlcs.ontapi.jena.model.*;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;

import java.util.function.Supplier;

/**
 * An interface describing an {@link InternalObjectFactory} that has a link to a specific model.
 * For internal use only.
 * It is to produce following {@link ONTObject}s implementations:
 * <ul>
 * <li>{@link com.github.owlcs.ontapi.internal.objects.ONTObjectImpl}</li>
 * <li>{@link com.github.owlcs.ontapi.internal.objects.ONTLiteralImpl}</li>
 * <li>{@link com.github.owlcs.ontapi.internal.objects.ONTAnonymousIndividualImpl}</li>
 * </ul>
 * <p>
 * Created by @ssz on 02.05.2020.
 */
public interface ModelObjectFactory extends InternalObjectFactory {

    Supplier<OntModel> model();

    ONTObject<OWLLiteral> getLiteral(LiteralLabel label);

    ONTObject<OWLAnonymousIndividual> getAnonymousIndividual(BlankNodeId id);

    ONTObject<OWLNamedIndividual> getNamedIndividual(String uri);

    ONTObject<OWLObjectProperty> getObjectProperty(String uri);

    ONTObject<OWLDataProperty> getDataProperty(String uri);

    ONTObject<OWLAnnotationProperty> getAnnotationProperty(String uri);

    ONTObject<OWLDatatype> getDatatype(String uri);

    ONTObject<OWLClass> getClass(String uri);

    ONTObject<? extends SWRLIArgument> getSWRLArgument(String uri);

    ONTObject<? extends SWRLIArgument> getSWRLArgument(BlankNodeId id);

    ONTObject<? extends SWRLDArgument> getSWRLArgument(LiteralLabel label);

    ONTObject<SWRLVariable> getSWRLVariable(String uri);

    default OntModel getModel() {
        return model().get();
    }

    @Override
    default ONTObject<OWLAnonymousIndividual> getIndividual(OntIndividual.Anonymous i) {
        return getAnonymousIndividual(i.asNode().getBlankNodeId());
    }

    @Override
    default ONTObject<OWLObjectProperty> getProperty(OntObjectProperty.Named p) {
        return getObjectProperty(p.getURI());
    }

    @Override
    default ONTObject<OWLDataProperty> getProperty(OntDataProperty p) {
        return getDataProperty(p.getURI());
    }

    @Override
    default ONTObject<OWLAnnotationProperty> getProperty(OntAnnotationProperty p) {
        return getAnnotationProperty(p.getURI());
    }

    @Override
    default ONTObject<OWLDatatype> getDatatype(OntDataRange.Named dt) {
        return getDatatype(dt.getURI());
    }

    @Override
    default ONTObject<OWLClass> getClass(OntClass.Named ce) {
        return getClass(ce.getURI());
    }

    @Override
    default ONTObject<OWLLiteral> getLiteral(Literal literal) {
        return getLiteral(literal.asNode().getLiteral());
    }

    @Override
    default ONTObject<? extends OWLObjectPropertyExpression> getProperty(OntObjectProperty property) {
        if (OntApiException.notNull(property, "Null object property.").isAnon()) {
            return getProperty((OntObjectProperty.Inverse) property);
        }
        return getObjectProperty(property.asNode().getURI());
    }

    @Override
    default ONTObject<OWLNamedIndividual> getIndividual(OntIndividual.Named i) {
        return getNamedIndividual(i.getURI());
    }

    @Override
    default ONTObject<? extends OWLIndividual> getIndividual(OntIndividual individual) {
        return getIndividual(OntApiException.notNull(individual, "Null individual").asNode());
    }

    /**
     * Creates an {@link OWLIndividual} wrapped as {@link ONTObject} for the given {@code node}.
     *
     * @param node {@link Node}, not {@code null}
     * @return {@link ONTObject} with {@link OWLIndividual}
     */
    default ONTObject<? extends OWLIndividual> getIndividual(Node node) {
        if (node.isBlank()) {
            return getAnonymousIndividual(node.getBlankNodeId());
        }
        return getNamedIndividual(node.getURI());
    }

    /**
     * Gets an {@link OWLEntity} as {@link ONTObject} from the {@link OntEntity}.
     *
     * @param uri  String, not {@code null}
     * @param type {@link Entities}, not {@code null}
     * @return {@link ONTObject} with {@link OntEntity}
     * @see InternalObjectFactory#getEntity(OntEntity)
     */
    default ONTObject<? extends OWLEntity> getEntity(String uri, Entities type) {
        switch (type) {
            case CLASS:
                return getClass(uri);
            case DATATYPE:
                return getDatatype(uri);
            case INDIVIDUAL:
                return getNamedIndividual(uri);
            case OBJECT_PROPERTY:
                return getObjectProperty(uri);
            case DATA_PROPERTY:
                return getDataProperty(uri);
            case ANNOTATION_PROPERTY:
                return getAnnotationProperty(uri);
        }
        throw new OntApiException.IllegalArgument("Unsupported type " + type);
    }

    @Override
    default ONTObject<? extends SWRLIArgument> getSWRLArgument(OntSWRL.IArg arg) {
        if (arg.isAnon()) {
            // treat any b-node as anonymous individual (whatever)
            return getSWRLArgument(arg.asNode().getBlankNodeId());
        }
        if (arg.canAs(OntSWRL.Variable.class)) {
            return getSWRLVariable(arg.as(OntSWRL.Variable.class));
        }
        return getSWRLArgument(arg.asNode().getURI());
    }

    @Override
    default ONTObject<? extends SWRLDArgument> getSWRLArgument(OntSWRL.DArg arg) {
        return arg.isLiteral() ?
                getSWRLArgument(arg.asNode().getLiteral()) :
                getSWRLVariable(arg.as(OntSWRL.Variable.class));
    }

    @Override
    default ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable v) {
        return getSWRLVariable(v.getURI());
    }
}
