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

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.rdf.model.Literal;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.internal.objects.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An Extended Object Factory impl which maps {@link ru.avicomp.ontapi.jena.model.OntObject OntObject}s
 * to {@link org.semanticweb.owlapi.model.OWLObject OWLObject}s directly having no cache.
 * Unlike {@link NoCacheObjectFactory} factory methods, here each returned instance is associated with a concrete model.
 * <p>
 * Created by @ssz on 07.08.2019.
 *
 * @since 1.4.3
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
    public ONTObject<OWLAnnotation> get(OntStatement s) {
        return ONTAnnotationImpl.create(s, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends OWLClassExpression> get(OntCE ce) {
        if (ce.isURIResource())
            return get((OntClass) ce);
        return (ONTObject<? extends OWLClassExpression>) ONTAnonymousClassExpressionImpl.create(ce, model);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends OWLDataRange> get(OntDR dr) {
        if (dr.isURIResource())
            return get((OntDT) dr);
        return (ONTObject<? extends OWLDataRange>) ONTAnonymousDataRangeImpl.create(dr, model);
    }

    @Override
    public ONTObject<OWLObjectInverseOf> get(OntOPE.Inverse iop) {
        return ONTObjectInverseOfImpl.create(iop, model);
    }

    @Override
    public ONTObject<OWLFacetRestriction> get(OntFR fr) {
        return ONTFacetRestrictionImpl.create(fr, model);
    }

    @Override
    public ONTObject<OWLClass> get(OntClass ce) {
        return getClass(ce.getURI());
    }

    @Override
    public ONTObject<OWLAnonymousIndividual> get(OntIndividual.Anonymous i) {
        return getAnonymousIndividual(i.asNode().getBlankNodeId());
    }

    @Override
    public ONTObject<OWLNamedIndividual> get(OntIndividual.Named i) {
        return getNamedIndividual(i.getURI());
    }

    @Override
    public ONTObject<OWLDatatype> get(OntDT dt) {
        return getDatatype(dt.getURI());
    }

    @Override
    public ONTObject<OWLAnnotationProperty> get(OntNAP p) {
        return getAnnotationProperty(p.getURI());
    }

    @Override
    public ONTObject<OWLObjectProperty> get(OntNOP p) {
        return getObjectProperty(p.getURI());
    }

    @Override
    public ONTObject<OWLDataProperty> get(OntNDP p) {
        return getDataProperty(p.getURI());
    }

    @Override
    public ONTObject<SWRLVariable> get(OntSWRL.Variable v) {
        return getSWRLVariable(v.getURI());
    }

    @Override
    public ONTObject<? extends SWRLIArgument> get(OntSWRL.IArg arg) {
        OntIndividual i;
        if (arg.isAnon()) {
            // treat any b-node as anonymous individual (whatever)
            i = OntModels.asAnonymousIndividual(arg);
        } else {
            i = arg.getAs(OntIndividual.class);
        }
        return i != null ?
                ONTSWRLIndividualImpl.create(i, model) :
                get(arg.as(OntSWRL.Variable.class));
    }

    @Override
    public ONTObject<? extends SWRLDArgument> get(OntSWRL.DArg arg) {
        return arg.isLiteral() ?
                new ONTSWRLLiteralImpl(arg.asNode().getLiteral(), model) :
                get(arg.as(OntSWRL.Variable.class));
    }

    @SuppressWarnings("unchecked")
    @Override
    public ONTObject<? extends SWRLAtom> get(OntSWRL.Atom atom) {
        return (ONTObject<? extends SWRLAtom>) ONTSWRLAtomIml.create(atom, model);
    }

    @Override
    public ONTObject<OWLLiteral> get(Literal literal) {
        return getLiteral(literal.asNode().getLiteral());
    }

    @Override
    public ONTObject<IRI> getIRI(String uri) {
        return ONTObjectImpl.create(toIRI(uri));
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

}
