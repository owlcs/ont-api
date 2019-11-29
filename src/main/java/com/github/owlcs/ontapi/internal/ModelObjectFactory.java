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

package com.github.owlcs.ontapi.internal;

import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.objects.*;
import com.github.owlcs.ontapi.jena.impl.Entities;
import com.github.owlcs.ontapi.jena.model.*;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An extended Object Factory impl which maps {@link com.github.owlcs.ontapi.jena.model.OntObject OntObject}s
 * to {@link org.semanticweb.owlapi.model.OWLObject OWLObject}s directly having no cache.
 * Unlike {@link SimpleObjectFactory},
 * every object created by this factory is {@link ONTObject} with a reference to a concrete model inside.
 * <p>
 * Created by @ssz on 07.08.2019.
 *
 * @since 2.0.0
 */
public class ModelObjectFactory implements InternalObjectFactory {
    protected final Supplier<OntGraphModel> model;
    protected final DataFactory factory;

    public ModelObjectFactory(DataFactory factory, Supplier<OntGraphModel> model) {
        this.factory = Objects.requireNonNull(factory);
        this.model = Objects.requireNonNull(model);
    }

    @Override
    public DataFactory getOWLDataFactory() {
        return factory;
    }

    @Override
    public ONTObject<OWLAnnotation> getAnnotation(OntStatement s) {
        return ONTAnnotationImpl.create(s, this, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends OWLClassExpression> getClass(OntCE ce) {
        if (ce.isURIResource())
            return getClass((OntClass) ce);
        return (ONTObject<? extends OWLClassExpression>) ONTAnonymousClassExpressionImpl.create(ce, this, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends OWLDataRange> getDatatype(OntDR dr) {
        if (dr.isURIResource())
            return getDatatype((OntDT) dr);
        return (ONTObject<? extends OWLDataRange>) ONTAnonymousDataRangeImpl.create(dr, this, model);
    }

    @Override
    public ONTObject<OWLObjectInverseOf> getProperty(OntOPE.Inverse iop) {
        return ONTObjectInverseOfImpl.create(iop, this, model);
    }

    @Override
    public ONTObject<OWLFacetRestriction> getFacetRestriction(OntFR fr) {
        return ONTFacetRestrictionImpl.create(fr, this, model);
    }

    @Override
    public ONTObject<OWLClass> getClass(OntClass ce) {
        return getClass(ce.getURI());
    }

    @Override
    public ONTObject<OWLAnonymousIndividual> getIndividual(OntIndividual.Anonymous i) {
        return getAnonymousIndividual(i.asNode().getBlankNodeId());
    }

    @Override
    public ONTObject<OWLNamedIndividual> getIndividual(OntIndividual.Named i) {
        return getNamedIndividual(i.getURI());
    }

    @Override
    public ONTObject<OWLDatatype> getDatatype(OntDT dt) {
        return getDatatype(dt.getURI());
    }

    @Override
    public ONTObject<OWLAnnotationProperty> getProperty(OntNAP p) {
        return getAnnotationProperty(p.getURI());
    }

    @Override
    public ONTObject<OWLObjectProperty> getProperty(OntNOP p) {
        return getObjectProperty(p.getURI());
    }

    @Override
    public ONTObject<OWLDataProperty> getProperty(OntNDP p) {
        return getDataProperty(p.getURI());
    }

    @Override
    public ONTObject<SWRLVariable> getSWRLVariable(OntSWRL.Variable v) {
        return getSWRLVariable(v.getURI());
    }

    @Override
    public ONTObject<? extends SWRLIArgument> getSWRLArgument(OntSWRL.IArg arg) {
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
    public ONTObject<? extends SWRLDArgument> getSWRLArgument(OntSWRL.DArg arg) {
        return arg.isLiteral() ?
                getSWRLArgument(arg.asNode().getLiteral()) :
                getSWRLVariable(arg.as(OntSWRL.Variable.class));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends SWRLAtom> getSWRLAtom(OntSWRL.Atom<?> atom) {
        return (ONTObject<? extends SWRLAtom>) ONTSWRLAtomImpl.create(atom, this, model);
    }

    @Override
    public ONTObject<OWLLiteral> getLiteral(Literal literal) {
        return getLiteral(literal.asNode().getLiteral());
    }

    @Override
    public ONTObject<IRI> getIRI(String uri) {
        return ONTIRI.create(uri);
    }

    @Override
    public IRI toIRI(String uri) {
        return ONTIRI.create(uri);
    }

    public ONTObject<OWLClass> getClass(String uri) {
        return new ONTClassImpl(uri, model);
    }

    public ONTObject<OWLAnonymousIndividual> getAnonymousIndividual(BlankNodeId id) {
        return new ONTAnonymousIndividualImpl(id, model);
    }

    public ONTObject<OWLNamedIndividual> getNamedIndividual(String uri) {
        return new ONTNamedIndividualImpl(uri, model);
    }

    public ONTObject<OWLDatatype> getDatatype(String uri) {
        return new ONTDatatypeImpl(uri, model);
    }

    public ONTObject<OWLAnnotationProperty> getAnnotationProperty(String uri) {
        return new ONTAnnotationPropertyImpl(uri, model);
    }

    public ONTObject<OWLObjectProperty> getObjectProperty(String uri) {
        return new ONTObjectPropertyImpl(uri, model);
    }

    public ONTObject<OWLDataProperty> getDataProperty(String uri) {
        return new ONTDataPropertyImpl(uri, model);
    }

    public ONTObject<OWLLiteral> getLiteral(LiteralLabel label) {
        return new ONTLiteralImpl(label, model);
    }

    public ONTObject<SWRLVariable> getSWRLVariable(String uri) {
        return new ONTSWRLVariable(uri, model);
    }

    /**
     * Gets an {@link OWLEntity} as {@link ONTObject} from the {@link OntEntity}.
     *
     * @param uri  String, not {@code null}
     * @param type {@link Entities}, not {@code null}
     * @return {@link ONTObject} with {@link OntEntity}
     * @see InternalObjectFactory#getEntity(OntEntity)
     */
    public ONTObject<? extends OWLEntity> getEntity(String uri, Entities type) {
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
    public ONTObject<? extends OWLObjectPropertyExpression> getProperty(OntOPE property) {
        if (OntApiException.notNull(property, "Null object property.").isAnon()) {
            return getProperty((OntOPE.Inverse) property);
        }
        return getObjectProperty(property.asNode().getURI());
    }

    @Override
    public ONTObject<? extends OWLIndividual> getIndividual(OntIndividual individual) {
        return getIndividual(OntApiException.notNull(individual, "Null individual").asNode());
    }

    /**
     * Creates an {@link OWLIndividual} wrapped as {@link ONTObject} for the given {@code node}.
     *
     * @param node {@link Node}, not {@code null}
     * @return {@link ONTObject} with {@link OWLIndividual}
     */
    public ONTObject<? extends OWLIndividual> getIndividual(Node node) {
        if (node.isBlank()) {
            return getAnonymousIndividual(node.getBlankNodeId());
        }
        return getNamedIndividual(node.getURI());
    }

    /**
     * Creates a {@link SWRLDArgument} wrapped as {@link ONTObject} for the given {@code label}.
     *
     * @param label {@link LiteralLabel}, not {@code null}
     * @return {@link ONTObject} with {@link SWRLDArgument}
     */
    public ONTObject<? extends SWRLDArgument> getSWRLArgument(LiteralLabel label) {
        return new ONTSWRLLiteralImpl(label, model);
    }

    /**
     * Creates a {@link SWRLIArgument} wrapped as {@link ONTObject} with the given {@code uri}.
     *
     * @param uri {@code String}, not {@code null}
     * @return {@link ONTObject} with {@link SWRLIArgument}
     */
    public ONTObject<? extends SWRLIArgument> getSWRLArgument(String uri) {
        return new ONTSWRLIndividualImpl(uri, model);
    }

    /**
     * Creates a {@link SWRLIArgument} wrapped as {@link ONTObject} for the given blank node {@code id}.
     *
     * @param id {@link BlankNodeId}, not {@code null}
     * @return {@link ONTObject} with {@link SWRLDArgument}
     */
    public ONTObject<? extends SWRLIArgument> getSWRLArgument(BlankNodeId id) {
        return new ONTSWRLIndividualImpl(id, model);
    }

}
