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

package ru.avicomp.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.SWRLBuiltInsVocabulary;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.internal.InternalObjectFactory;
import ru.avicomp.ontapi.internal.ONTObject;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.OntModels;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A {@link SWRLAtom} implementation that is also {@link ONTObject}.
 * Created by @ssz on 22.08.2019.
 *
 * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLAtomImpl
 * @see OntSWRL.Atom
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTSWRLAtomIml<ONT extends OntSWRL.Atom, OWL extends SWRLAtom>
        extends ONTExpressionImpl<ONT>
        implements SWRLAtom, ONTObject<OWL> {

    protected ONTSWRLAtomIml(BlankNodeId n, Supplier<OntGraphModel> m) {
        super(n, m);
    }

    /**
     * Wraps the given {@link OntSWRL.Atom atom} as {@link SWRLAtom} object,
     * which is an {@link ONTObject} at the same time.
     *
     * @param atom  {@link OntSWRL.Atom}, not {@code null}, must be anonymous
     * @param model a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTSWRLAtomIml} instance
     */
    @SuppressWarnings("unchecked")
    public static ONTSWRLAtomIml create(OntSWRL.Atom atom, Supplier<OntGraphModel> model) {
        Class<? extends OntSWRL.Atom> type = OntModels.getOntType(atom);
        BlankNodeId id = atom.asNode().getBlankNodeId();
        ONTSWRLAtomIml res = create(id, type, model);
        res.content.put(res, res.collectContent(atom, res.getObjectFactory()));
        return res;
    }

    /**
     * Creates a {@link SWRLAtom} instance for the given {@code id} and {@code type}.
     *
     * @param id    {@link BlankNodeId}, not {@code null}
     * @param type  {@code Class}-type of {@link OntSWRL.Atom}, not {@code null}
     * @param model {@link OntGraphModel}-provider, not {@code null}
     * @return {@link ONTSWRLAtomIml}
     */
    public static ONTSWRLAtomIml create(BlankNodeId id,
                                        Class<? extends OntSWRL.Atom> type,
                                        Supplier<OntGraphModel> model) {
        if (type == OntSWRL.Atom.BuiltIn.class) {
            return new B(id, model);
        }
        if (type == OntSWRL.Atom.OntClass.class) {
            return new C(id, model);
        }
        if (type == OntSWRL.Atom.DataRange.class) {
            return new D(id, model);
        }
        if (type == OntSWRL.Atom.ObjectProperty.class) {
            return new OP(id, model);
        }
        if (type == OntSWRL.Atom.DataProperty.class) {
            return new DP(id, model);
        }
        if (type == OntSWRL.Atom.DifferentIndividuals.class) {
            return new DI(id, model);
        }
        if (type == OntSWRL.Atom.SameIndividuals.class) {
            return new SI(id, model);
        }
        throw new OntApiException.IllegalState();
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWL getOWLObject() {
        return (OWL) this;
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLBuiltInAtomImpl
     * @see OntSWRL.Atom.BuiltIn
     */
    public static class B extends ONTSWRLAtomIml<OntSWRL.Atom.BuiltIn, SWRLBuiltInAtom> implements SWRLBuiltInAtom {

        protected B(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.BuiltIn asResource() {
            return as(OntSWRL.Atom.BuiltIn.class);
        }

        @Override
        public IRI getPredicate() {
            return (IRI) getContent()[0];
        }

        @Override
        public boolean isCoreBuiltIn() {
            return SWRLBuiltInsVocabulary.getBuiltIn(getPredicate()) != null;
        }

        @Override
        public Stream<SWRLArgument> allArguments() {
            return objects().map(x -> (SWRLDArgument) x.getOWLObject());
        }

        @Override
        public Stream<SWRLDArgument> arguments() {
            return objects().map(x -> (SWRLDArgument) x.getOWLObject());
        }

        @SuppressWarnings("unchecked")
        @Override
        public ExtendedIterator<ONTObject<? extends OWLObject>> listComponents() {
            Iterator res = Arrays.asList(getContent()).iterator();
            res.next(); // skip predicate (builtin) -- its spec is already included
            return (ExtendedIterator<ONTObject<? extends OWLObject>>) Iter.create(res);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected Object[] collectContent(OntSWRL.Atom.BuiltIn atom, InternalObjectFactory of) {
            IRI predicate = of.toIRI(atom.getPredicate().getURI());
            List res = OntModels.listMembers(atom.getArgList()).mapWith(of::get).toList();
            res.add(0, predicate);
            return res.toArray();
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLClassAtomImpl
     * @see OntSWRL.Atom.OntClass
     */
    public static class C
            extends U<OntCE, OntSWRL.IArg, OntSWRL.Atom.OntClass, OWLClassExpression, SWRLIArgument>
            implements SWRLClassAtom {

        protected C(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.OntClass asResource() {
            return as(OntSWRL.Atom.OntClass.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.OntClass obj, InternalObjectFactory of) {
            return new Object[]{of.get(obj.getPredicate()), of.get(obj.getArg())};
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLDataRangeAtomImpl
     * @see OntSWRL.Atom.DataRange
     */
    public static class D
            extends U<OntDR, OntSWRL.DArg, OntSWRL.Atom.DataRange, OWLDataRange, SWRLDArgument>
            implements SWRLDataRangeAtom {

        protected D(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.DataRange asResource() {
            return as(OntSWRL.Atom.DataRange.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.DataRange obj, InternalObjectFactory of) {
            return new Object[]{of.get(obj.getPredicate()), of.get(obj.getArg())};
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLSameIndividualAtomImpl
     * @see OntSWRL.Atom.SameIndividuals
     */
    public static class SI extends Bi<OntNOP,
            OntSWRL.IArg,
            OntSWRL.IArg,
            OntSWRL.Atom.SameIndividuals,
            OWLObjectProperty,
            SWRLIArgument,
            SWRLIArgument,
            SWRLSameIndividualAtom>
            implements SWRLSameIndividualAtom, ONTObject<SWRLSameIndividualAtom> {

        protected SI(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.SameIndividuals asResource() {
            return as(OntSWRL.Atom.SameIndividuals.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.SameIndividuals obj, InternalObjectFactory of) {
            return new Object[]{of.get(obj.getPredicate()), of.get(obj.getFirstArg()), of.get(obj.getSecondArg())};
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLDifferentIndividualsAtomImpl
     * @see OntSWRL.Atom.DifferentIndividuals
     */
    public static class DI extends Bi<OntNOP,
            OntSWRL.IArg,
            OntSWRL.IArg,
            OntSWRL.Atom.DifferentIndividuals,
            OWLObjectProperty,
            SWRLIArgument,
            SWRLIArgument,
            SWRLDifferentIndividualsAtom>
            implements SWRLDifferentIndividualsAtom, ONTObject<SWRLDifferentIndividualsAtom> {

        protected DI(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.DifferentIndividuals asResource() {
            return as(OntSWRL.Atom.DifferentIndividuals.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.DifferentIndividuals obj, InternalObjectFactory of) {
            return new Object[]{of.get(obj.getPredicate()), of.get(obj.getFirstArg()), of.get(obj.getSecondArg())};
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLDataPropertyAtomImpl
     * @see OntSWRL.Atom.DataProperty
     */
    public static class DP extends Bi<OntNDP,
            OntSWRL.IArg,
            OntSWRL.DArg,
            OntSWRL.Atom.DataProperty,
            OWLDataProperty,
            SWRLIArgument,
            SWRLDArgument,
            SWRLDataPropertyAtom>
            implements SWRLDataPropertyAtom, ONTObject<SWRLDataPropertyAtom> {

        protected DP(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.DataProperty asResource() {
            return as(OntSWRL.Atom.DataProperty.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.DataProperty obj, InternalObjectFactory of) {
            return new Object[]{of.get(obj.getPredicate()), of.get(obj.getFirstArg()), of.get(obj.getSecondArg())};
        }
    }

    /**
     * @see ru.avicomp.ontapi.owlapi.objects.swrl.SWRLObjectPropertyAtomImpl
     * @see OntSWRL.Atom.ObjectProperty
     */
    public static class OP extends Bi<OntOPE,
            OntSWRL.IArg,
            OntSWRL.IArg,
            OntSWRL.Atom.ObjectProperty,
            OWLObjectPropertyExpression,
            SWRLIArgument,
            SWRLIArgument,
            SWRLObjectPropertyAtom>
            implements SWRLObjectPropertyAtom, ONTObject<SWRLObjectPropertyAtom> {

        protected OP(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.ObjectProperty asResource() {
            return as(OntSWRL.Atom.ObjectProperty.class);
        }

        @Override
        public SWRLObjectPropertyAtom getSimplified() {
            OWLObjectPropertyExpression prop = getPredicate().getSimplified();
            if (prop.equals(getPredicate())) {
                return this;
            }
            DataFactory df = getDataFactory();
            if (prop.isAnonymous()) {
                return df.getSWRLObjectPropertyAtom(prop.getInverseProperty().getSimplified(),
                        getSecondArgument(), getFirstArgument());
            }
            return df.getSWRLObjectPropertyAtom(prop, getFirstArgument(), getSecondArgument());
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.ObjectProperty obj, InternalObjectFactory of) {
            return new Object[]{of.get(obj.getPredicate()), of.get(obj.getFirstArg()), of.get(obj.getSecondArg())};
        }
    }

    protected abstract static class Bi<ONT_P extends OntObject,
            ONT_F extends OntSWRL.Arg,
            ONT_S extends OntSWRL.Arg,
            ONT_R extends OntSWRL.Atom.Binary<ONT_P, ONT_F, ONT_S>,
            OWL_P extends OWLObject & SWRLPredicate,
            OWL_F extends SWRLArgument,
            OWL_S extends SWRLArgument,
            OWL_R extends SWRLBinaryAtom<OWL_F, OWL_S>>
            extends ONTSWRLAtomIml<ONT_R, OWL_R> {

        protected Bi(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OWL_P getPredicate() {
            return getONTPredicate().getOWLObject();
        }

        @Override
        public Stream<SWRLArgument> allArguments() {
            return Stream.of(getFirstArgument(), getSecondArgument());
        }

        public OWL_F getFirstArgument() {
            return getFirstONTArgument().getOWLObject();
        }

        public OWL_S getSecondArgument() {
            return getSecondONTArgument().getOWLObject();
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWL_P> getONTPredicate() {
            return (ONTObject<? extends OWL_P>) getContent()[0];
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWL_F> getFirstONTArgument() {
            return (ONTObject<? extends OWL_F>) getContent()[1];
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWL_S> getSecondONTArgument() {
            return (ONTObject<? extends OWL_S>) getContent()[2];
        }

        @SuppressWarnings("unchecked")
        @Override
        public ExtendedIterator<ONTObject<? extends OWLObject>> listComponents() {
            List res = Arrays.asList(getContent());
            return (ExtendedIterator<ONTObject<? extends OWLObject>>) Iter.create(res.iterator());
        }
    }

    protected abstract static class U<ONT_P extends OntObject,
            ONT_A extends OntSWRL.Arg,
            ONT_R extends OntSWRL.Atom.Unary<ONT_P, ONT_A>,
            OWL_P extends OWLObject & SWRLPredicate,
            OWL_A extends SWRLArgument> extends ONTSWRLAtomIml<ONT_R, SWRLUnaryAtom<OWL_A>> {

        protected U(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OWL_P getPredicate() {
            return getONTPredicate().getOWLObject();
        }

        @Override
        public Stream<SWRLArgument> allArguments() {
            return Stream.of(getArgument());
        }

        public OWL_A getArgument() {
            return getONTArgument().getOWLObject();
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWL_A> getONTArgument() {
            return (ONTObject<? extends OWL_A>) getContent()[1];
        }

        @SuppressWarnings("unchecked")
        public ONTObject<? extends OWL_P> getONTPredicate() {
            return (ONTObject<? extends OWL_P>) getContent()[0];
        }

        @SuppressWarnings("unchecked")
        @Override
        public ExtendedIterator<ONTObject<? extends OWLObject>> listComponents() {
            List res = Arrays.asList(getContent());
            return (ExtendedIterator<ONTObject<? extends OWLObject>>) Iter.create(res.iterator());
        }
    }

}
