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

package com.github.owlcs.ontapi.internal.objects;

import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.impl.LiteralLabel;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.SWRLBuiltInsVocabulary;
import com.github.owlcs.ontapi.DataFactory;
import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.internal.InternalObjectFactory;
import com.github.owlcs.ontapi.internal.ModelObjectFactory;
import com.github.owlcs.ontapi.internal.ONTObject;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.OntModels;
import com.github.owlcs.ontapi.owlapi.OWLObjectImpl;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A {@link SWRLAtom} implementation that is also {@link ONTObject}.
 * Created by @ssz on 22.08.2019.
 *
 * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLAtomImpl
 * @see OntSWRL.Atom
 * @since 1.4.3
 */
@SuppressWarnings("WeakerAccess")
public abstract class ONTSWRLAtomImpl<ONT extends OntSWRL.Atom, OWL extends SWRLAtom>
        extends ONTExpressionImpl<ONT>
        implements SWRLAtom, ModelObject<OWL> {

    protected ONTSWRLAtomImpl(BlankNodeId n, Supplier<OntGraphModel> m) {
        super(n, m);
    }

    /**
     * Wraps the given {@link OntSWRL.Atom atom} as {@link SWRLAtom} object,
     * which is an {@link ONTObject} at the same time.
     *
     * @param atom  {@link OntSWRL.Atom}, not {@code null}, must be anonymous
     * @param factory {@link InternalObjectFactory}, not {@code null}
     * @param model a provider of non-null {@link OntGraphModel}, not {@code null}
     * @return {@link ONTSWRLAtomImpl} instance
     */
    @SuppressWarnings("unchecked")
    public static ONTSWRLAtomImpl create(OntSWRL.Atom atom,
                                         InternalObjectFactory factory,
                                         Supplier<OntGraphModel> model) {
        Class<? extends OntSWRL.Atom> type = OntModels.getOntType(atom);
        BlankNodeId id = atom.asNode().getBlankNodeId();
        ONTSWRLAtomImpl res = create(id, type, model);
        res.putContent(res.initContent(atom, factory));
        return res;
    }

    /**
     * Creates a {@link SWRLAtom} instance for the given {@code id} and {@code type}.
     *
     * @param id    {@link BlankNodeId}, not {@code null}
     * @param type  {@code Class}-type of {@link OntSWRL.Atom}, not {@code null}
     * @param model {@link OntGraphModel}-provider, not {@code null}
     * @return {@link ONTSWRLAtomImpl}
     */
    public static ONTSWRLAtomImpl create(BlankNodeId id,
                                         Class<? extends OntSWRL.Atom> type,
                                         Supplier<OntGraphModel> model) {
        if (type == OntSWRL.Atom.BuiltIn.class) {
            return new BN(id, model);
        }
        if (type == OntSWRL.Atom.OntClass.class) {
            return new CU(id, model);
        }
        if (type == OntSWRL.Atom.DataRange.class) {
            return new DU(id, model);
        }
        if (type == OntSWRL.Atom.ObjectProperty.class) {
            return new OPB(id, model);
        }
        if (type == OntSWRL.Atom.DataProperty.class) {
            return new DPB(id, model);
        }
        if (type == OntSWRL.Atom.DifferentIndividuals.class) {
            return new DIB(id, model);
        }
        if (type == OntSWRL.Atom.SameIndividuals.class) {
            return new SIB(id, model);
        }
        throw new OntApiException.IllegalState();
    }

    /**
     * Creates a content item from a Jena Resource ({@link OntSWRL.IArg}).
     * For internal usage only.
     *
     * @param argument - {@link OntSWRL.IArg}, not {@code null}
     * @param object   - a {@code Object} - the default value
     * @return a content item
     * @see #toIArgument(Object, InternalObjectFactory)
     */
    protected static Object fromIArgument(OntSWRL.IArg argument, Object object) {
        if (argument.isAnon()) {
            return argument.asNode().getBlankNodeId();
        }
        if (argument.canAs(OntSWRL.Variable.class)) {
            return object;
        }
        return argument.asNode().getURI();
    }

    /**
     * Creates a content item from a Jena Resource ({@link OntSWRL.DArg}).
     * For internal usage only.
     *
     * @param argument - {@link OntSWRL.DArg}, not {@code null}
     * @param object   - a {@code Object} - the default value
     * @return a content item
     * @see #toDArgument(Object, InternalObjectFactory)
     */
    protected static Object fromDArgument(OntSWRL.DArg argument, Object object) {
        if (argument.isLiteral()) {
            return argument.asNode().getLiteral();
        }
        return object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public OWL getOWLObject() {
        return (OWL) this;
    }

    @Override
    public boolean canContainAnnotationProperties() {
        return false;
    }

    /**
     * Lists all {@link OWLDatatype}s encapsulated in this atom.
     *
     * @return a {@code Stream} of {@link OWLDatatype}s
     */
    protected Stream<OWLDatatype> datatypes() {
        return objects().map(x -> {
            OWLObject a = x.getOWLObject();
            if (a instanceof OWLDatatype) {
                return (OWLDatatype) a;
            }
            if (a instanceof SWRLLiteralArgument) {
                return ((SWRLLiteralArgument) a).getLiteral().getDatatype();
            }
            return null;
        }).filter(Objects::nonNull);
    }

    /**
     * Lists all {@link OWLIndividual}s encapsulated in this atom.
     *
     * @return a {@code Stream} of {@link OWLIndividual}s
     */
    protected Stream<OWLIndividual> individuals() {
        return objects().map(x -> {
            OWLObject a = x.getOWLObject();
            if (a instanceof OWLIndividual) {
                return (OWLIndividual) a;
            }
            if (a instanceof SWRLIndividualArgument) {
                return ((SWRLIndividualArgument) a).getIndividual();
            }
            return null;
        }).filter(Objects::nonNull);
    }

    /**
     * Restores a content item to a {@link SWRLIArgument} wrapped as {@link ONTObject}.
     * For internal usage only.
     *
     * @param item    {@code Object} - either {@code String} (an uri for {@link SWRLIndividualArgument})
     *                or {@link BlankNodeId} (id for {@link SWRLIndividualArgument})
     *                or {@link SWRLVariable} (as {@link ONTObject}), not {@code null}
     * @param factory - {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ONTObject} with {@link SWRLIArgument}
     * @see #fromIArgument(OntSWRL.IArg, Object)
     */
    @SuppressWarnings("unchecked")
    protected ONTObject<? extends SWRLIArgument> toIArgument(Object item, InternalObjectFactory factory) {
        if (!(item instanceof String) && !(item instanceof BlankNodeId)) {
            return (ONTObject<? extends SWRLIArgument>) item;
        }
        if (factory instanceof ModelObjectFactory) {
            ModelObjectFactory mf = (ModelObjectFactory) factory;
            return item instanceof String ? mf.getSWRLArgument((String) item) : mf.getSWRLArgument((BlankNodeId) item);
        }
        Node node = item instanceof String ?
                NodeFactory.createURI((String) item) : NodeFactory.createBlankNode((BlankNodeId) item);
        return factory.getSWRLArgument(getModel().asRDFNode(node).as(OntSWRL.IArg.class));
    }

    /**
     * Restores a content item to a {@link SWRLDArgument} wrapped as {@link ONTObject}.
     * For internal usage only.
     *
     * @param item    {@code Object} - either {@link LiteralLabel} (for {@link SWRLLiteralArgument})
     *                or {@link SWRLVariable} (as {@link ONTObject}), not {@code null}
     * @param factory - {@link InternalObjectFactory}, not {@code null}
     * @return an {@link ONTObject} with {@link SWRLDArgument}
     * @see #fromDArgument(OntSWRL.DArg, Object)
     */
    @SuppressWarnings("unchecked")
    protected ONTObject<? extends SWRLDArgument> toDArgument(Object item, InternalObjectFactory factory) {
        if (!(item instanceof LiteralLabel)) {
            return (ONTObject<? extends SWRLDArgument>) item;
        }
        if (factory instanceof ModelObjectFactory) {
            ModelObjectFactory mf = (ModelObjectFactory) factory;
            return mf.getSWRLArgument((LiteralLabel) item);
        }
        Node node = NodeFactory.createLiteral((LiteralLabel) item);
        return factory.getSWRLArgument(getModel().asRDFNode(node).as(OntSWRL.DArg.class));
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLBuiltInAtomImpl
     * @see OntSWRL.Atom.BuiltIn
     */
    public static class BN extends ONTSWRLAtomImpl<OntSWRL.Atom.BuiltIn, SWRLBuiltInAtom> implements SWRLBuiltInAtom {

        protected BN(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.BuiltIn asRDFNode() {
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

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            return objects(getObjectFactory());
        }

        protected Stream<ONTObject<? extends OWLObject>> objects(InternalObjectFactory factory) {
            // skip predicate (builtin) -- its spec is already included
            List<Object> res = Arrays.asList(getContent());
            return res.stream().skip(1).map(x -> toDArgument(x, factory));
        }

        @Override
        public SWRLBuiltInAtom eraseModel() {
            return getDataFactory().getSWRLBuiltInAtom(getPredicate(),
                    arguments().map(ONTObjectImpl::eraseModel).collect(Collectors.toList()));
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.BuiltIn atom, InternalObjectFactory factory) {
            IRI predicate = factory.toIRI(atom.getPredicate().getURI());
            List<Object> res = new ArrayList<>();
            res.add(predicate);
            OntModels.listMembers(atom.getArgList())
                    .forEachRemaining(arg -> res.add(arg.isLiteral() ?
                            arg.asNode().getLiteral() : factory.getSWRLArgument(arg)));
            return res.toArray();
        }

        @Override
        protected Object[] initContent(OntSWRL.Atom.BuiltIn atom, InternalObjectFactory factory) {
            IRI predicate = factory.toIRI(atom.getPredicate().getURI());
            List<Object> res = new ArrayList<>();
            res.add(predicate);
            Iterator<OntSWRL.DArg> it = OntModels.listMembers(atom.getArgList());
            int hash = 1;
            while (it.hasNext()) {
                OntSWRL.DArg r = it.next();
                ONTObject<? extends SWRLDArgument> op = factory.getSWRLArgument(r);
                hash = WithContent.hashIteration(hash, op.hashCode());
                res.add(fromDArgument(r, op));
            }
            this.hashCode = OWLObject.hashIteration(OWLObject.hashIteration(hashIndex(), hash), predicate.hashCode());
            return res.toArray();
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            return datatypes().collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public boolean containsDatatype(OWLDatatype datatype) {
            return datatypes().anyMatch(datatype::equals);
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public Set<OWLDatatype> getDatatypeSet() {
            return datatypes().collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return false;
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLClassAtomImpl
     * @see OntSWRL.Atom.OntClass
     */
    public static class CU
            extends Unary<OntCE, OntSWRL.IArg, OntSWRL.Atom.OntClass, OWLClassExpression, SWRLIArgument, SWRLClassAtom>
            implements SWRLClassAtom {

        protected CU(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.OntClass asRDFNode() {
            return as(OntSWRL.Atom.OntClass.class);
        }

        @Override
        ONTObject<? extends OWLClassExpression> mapPredicate(OntCE predicate, InternalObjectFactory factory) {
            return factory.getClass(predicate);
        }

        @Override
        ONTObject<? extends SWRLIArgument> mapArgument(OntSWRL.IArg arg, InternalObjectFactory factory) {
            return factory.getSWRLArgument(arg);
        }

        @Override
        ONTObject<? extends OWLClassExpression> toPredicate(Object item, InternalObjectFactory factory) {
            return toCE(item, factory);
        }

        @Override
        ONTObject<? extends SWRLIArgument> toArgument(Object item, InternalObjectFactory factory) {
            return toIArgument(item, factory);
        }

        @Override
        Object fromArgument(OntSWRL.IArg arg, ONTObject<? extends SWRLIArgument> object) {
            return fromIArgument(arg, object);
        }

        @Override
        SWRLClassAtom fromFactory(OWLClassExpression c, SWRLIArgument a) {
            return getDataFactory().getSWRLClassAtom(c, a);
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLDataRangeAtomImpl
     * @see OntSWRL.Atom.DataRange
     */
    public static class DU
            extends Unary<OntDR, OntSWRL.DArg, OntSWRL.Atom.DataRange, OWLDataRange, SWRLDArgument, SWRLDataRangeAtom>
            implements SWRLDataRangeAtom {

        protected DU(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.DataRange asRDFNode() {
            return as(OntSWRL.Atom.DataRange.class);
        }

        @Override
        ONTObject<? extends OWLDataRange> mapPredicate(OntDR dr, InternalObjectFactory factory) {
            return factory.getDatatype(dr);
        }

        @Override
        ONTObject<? extends SWRLDArgument> mapArgument(OntSWRL.DArg arg, InternalObjectFactory factory) {
            return factory.getSWRLArgument(arg);
        }

        @Override
        ONTObject<? extends OWLDataRange> toPredicate(Object item, InternalObjectFactory factory) {
            return toDR(item, factory);
        }

        @Override
        ONTObject<? extends SWRLDArgument> toArgument(Object item, InternalObjectFactory factory) {
            return toDArgument(item, factory);
        }

        @Override
        Object fromArgument(OntSWRL.DArg arg, ONTObject<? extends SWRLDArgument> object) {
            return fromDArgument(arg, object);
        }

        @Override
        SWRLDataRangeAtom fromFactory(OWLDataRange d, SWRLDArgument a) {
            return getDataFactory().getSWRLDataRangeAtom(d, a);
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            return datatypes().collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public boolean containsDatatype(OWLDatatype datatype) {
            return datatypes().anyMatch(datatype::equals);
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public Set<OWLDatatype> getDatatypeSet() {
            return datatypes().collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public boolean canContainNamedIndividuals() {
            return false;
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public boolean canContainClassExpressions() {
            return false;
        }

        @Override
        public boolean canContainAnonymousIndividuals() {
            return false;
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLSameIndividualAtomImpl
     * @see OntSWRL.Atom.SameIndividuals
     */
    public static class SIB
            extends ObjectBinary<OntNOP, OWLObjectProperty, OntSWRL.Atom.SameIndividuals, SWRLSameIndividualAtom>
            implements SWRLSameIndividualAtom {

        protected SIB(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.SameIndividuals asRDFNode() {
            return as(OntSWRL.Atom.SameIndividuals.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.SameIndividuals obj, InternalObjectFactory factory) {
            return new Object[]{factory.getProperty(obj.getPredicate()),
                    factory.getSWRLArgument(obj.getFirstArg()), factory.getSWRLArgument(obj.getSecondArg())};
        }

        @Override
        SWRLSameIndividualAtom fromFactory(OWLObjectProperty p, SWRLIArgument f, SWRLIArgument s) {
            return getDataFactory().getSWRLSameIndividualAtom(f, s);
        }

        @Override
        OWLObjectProperty factoryPredicate() {
            return null;
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLDifferentIndividualsAtomImpl
     * @see OntSWRL.Atom.DifferentIndividuals
     */
    public static class DIB
            extends ObjectBinary<OntNOP, OWLObjectProperty, OntSWRL.Atom.DifferentIndividuals, SWRLDifferentIndividualsAtom>
            implements SWRLDifferentIndividualsAtom {

        protected DIB(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.DifferentIndividuals asRDFNode() {
            return as(OntSWRL.Atom.DifferentIndividuals.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.DifferentIndividuals obj, InternalObjectFactory factory) {
            return new Object[]{factory.getProperty(obj.getPredicate()),
                    factory.getSWRLArgument(obj.getFirstArg()), factory.getSWRLArgument(obj.getSecondArg())};
        }

        @Override
        SWRLDifferentIndividualsAtom fromFactory(OWLObjectProperty p, SWRLIArgument f, SWRLIArgument s) {
            return getDataFactory().getSWRLDifferentIndividualsAtom(f, s);
        }

        @Override
        OWLObjectProperty factoryPredicate() {
            return null;
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLDataPropertyAtomImpl
     * @see OntSWRL.Atom.DataProperty
     */
    public static class DPB extends Binary<OntNDP,
            OntSWRL.IArg,
            OntSWRL.DArg,
            OntSWRL.Atom.DataProperty,
            OWLDataProperty,
            SWRLIArgument,
            SWRLDArgument,
            SWRLDataPropertyAtom>
            implements SWRLDataPropertyAtom {

        protected DPB(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.DataProperty asRDFNode() {
            return as(OntSWRL.Atom.DataProperty.class);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.DataProperty obj, InternalObjectFactory factory) {
            return new Object[]{factory.getProperty(obj.getPredicate()),
                    factory.getSWRLArgument(obj.getFirstArg()), factory.getSWRLArgument(obj.getSecondArg())};
        }

        @Override
        ONTObject<? extends OWLDataProperty> mapPredicate(OntNDP p, InternalObjectFactory factory) {
            return factory.getProperty(p);
        }

        @Override
        ONTObject<? extends SWRLIArgument> mapFirstArgument(OntSWRL.IArg a, InternalObjectFactory factory) {
            return factory.getSWRLArgument(a);
        }

        @Override
        ONTObject<? extends SWRLDArgument> mapSecondArgument(OntSWRL.DArg a, InternalObjectFactory factory) {
            return factory.getSWRLArgument(a);
        }

        @Override
        ONTObject<? extends OWLDataProperty> toPredicate(Object item, InternalObjectFactory factory) {
            return toNDP(item, factory);
        }

        @Override
        ONTObject<? extends SWRLIArgument> toFirstArgument(Object item, InternalObjectFactory factory) {
            return toIArgument(item, factory);
        }

        @Override
        ONTObject<? extends SWRLDArgument> toSecondArgument(Object item, InternalObjectFactory factory) {
            return toDArgument(item, factory);
        }

        @Override
        Object fromFirstArgument(OntSWRL.IArg arg, ONTObject<? extends SWRLIArgument> object) {
            return fromIArgument(arg, object);
        }

        @Override
        Object fromSecondArgument(OntSWRL.DArg arg, ONTObject<? extends SWRLDArgument> object) {
            return fromDArgument(arg, object);
        }

        @Override
        SWRLDataPropertyAtom fromFactory(OWLDataProperty d, SWRLIArgument f, SWRLDArgument s) {
            return getDataFactory().getSWRLDataPropertyAtom(d, f, s);
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public boolean canContainObjectProperties() {
            return false;
        }

        @Override
        public boolean canContainClassExpressions() {
            return false;
        }
    }

    /**
     * @see com.github.owlcs.ontapi.owlapi.objects.swrl.SWRLObjectPropertyAtomImpl
     * @see OntSWRL.Atom.ObjectProperty
     */
    public static class OPB
            extends ObjectBinary<OntOPE, OWLObjectPropertyExpression, OntSWRL.Atom.ObjectProperty, SWRLObjectPropertyAtom>
            implements SWRLObjectPropertyAtom {

        protected OPB(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @Override
        public OntSWRL.Atom.ObjectProperty asRDFNode() {
            return as(OntSWRL.Atom.ObjectProperty.class);
        }

        @FactoryAccessor
        @Override
        public SWRLObjectPropertyAtom getSimplified() {
            OWLObjectPropertyExpression p = getPredicate();
            OWLObjectPropertyExpression ps = p.getSimplified();
            if (ps.equals(p)) {
                return eraseModel();
            }
            // p always equals ps (v5.1.11) - so the following code just in case:
            DataFactory df = getDataFactory();
            ps = eraseModel(ps);
            SWRLIArgument f = eraseModel(getFirstArgument());
            SWRLIArgument s = eraseModel(getSecondArgument());
            if (ps.isAnonymous()) {
                return df.getSWRLObjectPropertyAtom(ps.getInverseProperty(), s, f);
            }
            return df.getSWRLObjectPropertyAtom(ps, f, s);
        }

        @Override
        protected Object[] collectContent(OntSWRL.Atom.ObjectProperty obj, InternalObjectFactory factory) {
            return new Object[]{factory.getProperty(obj.getPredicate()),
                    factory.getSWRLArgument(obj.getFirstArg()), factory.getSWRLArgument(obj.getSecondArg())};
        }

        @Override
        SWRLObjectPropertyAtom fromFactory(OWLObjectPropertyExpression p, SWRLIArgument f, SWRLIArgument s) {
            return getDataFactory().getSWRLObjectPropertyAtom(p, f, s);
        }
    }

    /**
     * An abstract object binary atom.
     *
     * @param <ONT_P> - subtype of {@link OntOPE}
     * @param <OWL_P> - subtype of {@link OWLObjectPropertyExpression} that matches {@link ONT_P}
     * @param <ONT_R> - subtype of {@link OntSWRL.Atom.Binary}
     * @param <OWL_R> - subtype of {@link SWRLBinaryAtom} that matches {@link ONT_R}
     */
    protected abstract static class ObjectBinary<ONT_P extends OntOPE,
            OWL_P extends OWLObjectPropertyExpression,
            ONT_R extends OntSWRL.Atom.Binary<ONT_P, OntSWRL.IArg, OntSWRL.IArg>,
            OWL_R extends SWRLBinaryAtom<SWRLIArgument, SWRLIArgument>>
            extends Binary<ONT_P, OntSWRL.IArg, OntSWRL.IArg, ONT_R, OWL_P, SWRLIArgument, SWRLIArgument, OWL_R> {

        protected ObjectBinary(BlankNodeId n, Supplier<OntGraphModel> m) {
            super(n, m);
        }

        @SuppressWarnings("unchecked")
        @Override
        ONTObject<? extends OWL_P> mapPredicate(ONT_P p, InternalObjectFactory factory) {
            return (ONTObject<? extends OWL_P>) factory.getProperty(p);
        }

        @Override
        ONTObject<? extends SWRLIArgument> mapFirstArgument(OntSWRL.IArg a, InternalObjectFactory factory) {
            return factory.getSWRLArgument(a);
        }

        @Override
        ONTObject<? extends SWRLIArgument> mapSecondArgument(OntSWRL.IArg a, InternalObjectFactory factory) {
            return factory.getSWRLArgument(a);
        }

        @SuppressWarnings("unchecked")
        @Override
        ONTObject<? extends OWL_P> toPredicate(Object item, InternalObjectFactory factory) {
            return (ONTObject<? extends OWL_P>) toOPE(item, factory);
        }

        @Override
        ONTObject<? extends SWRLIArgument> toFirstArgument(Object item, InternalObjectFactory factory) {
            return toIArgument(item, factory);
        }

        @Override
        ONTObject<? extends SWRLIArgument> toSecondArgument(Object item, InternalObjectFactory factory) {
            return toIArgument(item, factory);
        }

        @Override
        Object fromFirstArgument(OntSWRL.IArg arg, ONTObject<? extends SWRLIArgument> object) {
            return fromIArgument(arg, object);
        }

        @Override
        Object fromSecondArgument(OntSWRL.IArg arg, ONTObject<? extends SWRLIArgument> object) {
            return fromIArgument(arg, object);
        }

        @Override
        protected Stream<OWLIndividual> individuals() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findONTFirstArgument(factory), findONTSecondArgument(factory))
                    .map(ONTObject::getOWLObject)
                    .filter(x -> x instanceof SWRLIndividualArgument)
                    .map(x -> ((SWRLIndividualArgument) x).getIndividual());
        }

        @Override
        public Set<OWLEntity> getSignatureSet() {
            Set<OWLEntity> res = createSortedSet();
            res.add(getPredicate().getNamedProperty());
            individuals().filter(AsOWLNamedIndividual::isOWLNamedIndividual)
                    .forEach(x -> res.add(x.asOWLNamedIndividual()));
            return res;
        }

        @Override
        public boolean containsObjectProperty(OWLObjectProperty property) {
            return getPredicate().getNamedProperty().equals(property);
        }

        @Override
        public boolean containsNamedIndividual(OWLNamedIndividual individual) {
            return individuals().filter(AsOWLNamedIndividual::isOWLNamedIndividual).anyMatch(individual::equals);
        }

        @Override
        public Set<OWLObjectProperty> getObjectPropertySet() {
            return createSet(getPredicate().getNamedProperty());
        }

        @Override
        public Set<OWLNamedIndividual> getNamedIndividualSet() {
            return individuals().filter(AsOWLNamedIndividual::isOWLNamedIndividual)
                    .map(AsOWLNamedIndividual::asOWLNamedIndividual)
                    .collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public Set<OWLAnonymousIndividual> getAnonymousIndividualSet() {
            return individuals().filter(x -> !x.isOWLNamedIndividual())
                    .map(OWLIndividual::asOWLAnonymousIndividual)
                    .collect(Collectors.toCollection(OWLObjectImpl::createSortedSet));
        }

        @Override
        public boolean canContainNamedClasses() {
            return false;
        }

        @Override
        public boolean canContainDatatypes() {
            return false;
        }

        @Override
        public boolean canContainDataProperties() {
            return false;
        }

        @Override
        public boolean canContainClassExpressions() {
            return false;
        }
    }

    /**
     * An abstract binary atom impl.
     *
     * @param <ONT_P> - subtype of {@link OntObject}, predicate
     * @param <ONT_F> - subtype of {@link OntSWRL.Arg}, first argument
     * @param <ONT_S> - subtype of {@link OntSWRL.Arg}, second argument
     * @param <ONT_R> - subtype of {@link OntSWRL.Atom.Binary}, the actual jena type
     * @param <OWL_P> - subtype of {@link SWRLPredicate}, that matches {@link ONT_P}
     * @param <OWL_F> - subtype of {@link SWRLArgument}, that matches {@link ONT_F}
     * @param <OWL_S> - subtype of {@link SWRLArgument}, that matches {@link ONT_S}
     * @param <OWL_R> - subtype of {@link SWRLBinaryAtom}, that matches {@link ONT_R}
     */
    protected abstract static class Binary<ONT_P extends OntObject,
            ONT_F extends OntSWRL.Arg,
            ONT_S extends OntSWRL.Arg,
            ONT_R extends OntSWRL.Atom.Binary<ONT_P, ONT_F, ONT_S>,
            OWL_P extends OWLObject & SWRLPredicate,
            OWL_F extends SWRLArgument,
            OWL_S extends SWRLArgument,
            OWL_R extends SWRLBinaryAtom<OWL_F, OWL_S>>
            extends ONTSWRLAtomImpl<ONT_R, OWL_R> {

        protected Binary(BlankNodeId n, Supplier<OntGraphModel> m) {
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

        public ONTObject<? extends OWL_P> getONTPredicate() {
            return findONTPredicate(getObjectFactory());
        }

        public ONTObject<? extends OWL_F> getFirstONTArgument() {
            return findONTFirstArgument(getObjectFactory());
        }

        public ONTObject<? extends OWL_S> getSecondONTArgument() {
            return findONTSecondArgument(getObjectFactory());
        }

        protected ONTObject<? extends OWL_F> findONTFirstArgument(InternalObjectFactory factory) {
            return toFirstArgument(getContent()[1], factory);
        }

        protected ONTObject<? extends OWL_S> findONTSecondArgument(InternalObjectFactory factory) {
            return toSecondArgument(getContent()[2], factory);
        }

        protected ONTObject<? extends OWL_P> findONTPredicate(InternalObjectFactory factory) {
            return toPredicate(getContent()[0], factory);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findONTPredicate(factory), findONTFirstArgument(factory), findONTSecondArgument(factory));
        }

        @Override
        public OWL_R eraseModel() {
            return fromFactory(factoryPredicate(), eraseModel(getFirstArgument()), eraseModel(getSecondArgument()));
        }

        OWL_P factoryPredicate() {
            return eraseModel(getPredicate());
        }

        abstract OWL_R fromFactory(OWL_P p, OWL_F f, OWL_S s);

        @Override
        protected Object[] collectContent(ONT_R obj, InternalObjectFactory factory) {
            ONT_P p = obj.getPredicate();
            ONT_F f = obj.getFirstArg();
            ONT_S s = obj.getSecondArg();
            ONTObject<? extends OWL_P> predicate = mapPredicate(p, factory);
            ONTObject<? extends OWL_F> first = mapFirstArgument(f, factory);
            ONTObject<? extends OWL_S> second = mapSecondArgument(s, factory);
            return new Object[]{fromPredicate(p, predicate), fromFirstArgument(f, first), fromSecondArgument(s, second)};
        }

        @Override
        protected Object[] initContent(ONT_R obj, InternalObjectFactory factory) {
            ONT_P p = obj.getPredicate();
            ONT_F f = obj.getFirstArg();
            ONT_S s = obj.getSecondArg();
            ONTObject<? extends OWL_P> predicate = mapPredicate(p, factory);
            ONTObject<? extends OWL_F> first = mapFirstArgument(f, factory);
            ONTObject<? extends OWL_S> second = mapSecondArgument(s, factory);
            int res = OWLObject.hashIteration(hashIndex(), first.hashCode());
            res = OWLObject.hashIteration(res, second.hashCode());
            res = OWLObject.hashIteration(res, predicate.hashCode());
            this.hashCode = res;
            return new Object[]{fromPredicate(p, predicate), fromFirstArgument(f, first), fromSecondArgument(s, second)};
        }

        Object fromPredicate(ONT_P predicate, Object object) {
            return predicate.isURIResource() ? predicate.asNode().getURI() : object;
        }

        abstract Object fromFirstArgument(ONT_F arg, ONTObject<? extends OWL_F> object);

        abstract Object fromSecondArgument(ONT_S arg, ONTObject<? extends OWL_S> object);

        abstract ONTObject<? extends OWL_P> mapPredicate(ONT_P p, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_F> mapFirstArgument(ONT_F a, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_S> mapSecondArgument(ONT_S a, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_P> toPredicate(Object item, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_F> toFirstArgument(Object item, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_S> toSecondArgument(Object item, InternalObjectFactory factory);
    }

    /**
     * An abstract unary atom impl
     *
     * @param <ONT_P> - subtype of {@link OntObject}, predicate
     * @param <ONT_A> - subtype of {@link OntSWRL.Arg}, argument
     * @param <ONT_R> - subtype of {@link OntSWRL.Atom.Unary}, the actual jena type
     * @param <OWL_P> - subtype of {@link SWRLPredicate}, that matches {@link ONT_P}
     * @param <OWL_A> - subtype of {@link SWRLArgument}, that matches {@link ONT_A}
     * @param <OWL_R> - subtype of {@link SWRLUnaryAtom}, that matches {@link ONT_R}
     */
    protected abstract static class Unary<ONT_P extends OntObject,
            ONT_A extends OntSWRL.Arg,
            ONT_R extends OntSWRL.Atom.Unary<ONT_P, ONT_A>,
            OWL_P extends OWLObject & SWRLPredicate,
            OWL_A extends SWRLArgument,
            OWL_R extends SWRLUnaryAtom<OWL_A>> extends ONTSWRLAtomImpl<ONT_R, SWRLUnaryAtom<OWL_A>> {

        protected Unary(BlankNodeId n, Supplier<OntGraphModel> m) {
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

        public ONTObject<? extends OWL_A> getONTArgument() {
            return findONTArgument(getObjectFactory());
        }

        public ONTObject<? extends OWL_P> getONTPredicate() {
            return findONTPredicate(getObjectFactory());
        }

        protected ONTObject<? extends OWL_A> findONTArgument(InternalObjectFactory factory) {
            return toArgument(getContent()[1], factory);
        }

        protected ONTObject<? extends OWL_P> findONTPredicate(InternalObjectFactory factory) {
            return toPredicate(getContent()[0], factory);
        }

        @Override
        public Stream<ONTObject<? extends OWLObject>> objects() {
            InternalObjectFactory factory = getObjectFactory();
            return Stream.of(findONTPredicate(factory), findONTArgument(factory));
        }

        @Override
        public OWL_R eraseModel() {
            return fromFactory(eraseModel(getPredicate()), eraseModel(getArgument()));
        }

        abstract OWL_R fromFactory(OWL_P p, OWL_A a);

        @Override
        protected Object[] collectContent(ONT_R obj, InternalObjectFactory factory) {
            ONT_P p = obj.getPredicate();
            ONT_A a = obj.getArg();
            ONTObject<? extends OWL_P> predicate = mapPredicate(p, factory);
            ONTObject<? extends OWL_A> argument = mapArgument(a, factory);
            return new Object[]{fromPredicate(p, predicate), fromArgument(a, argument)};
        }

        @Override
        protected Object[] initContent(ONT_R obj, InternalObjectFactory factory) {
            ONT_P p = obj.getPredicate();
            ONT_A a = obj.getArg();
            ONTObject<? extends OWL_A> argument = mapArgument(a, factory);
            ONTObject<? extends OWL_P> predicate = mapPredicate(p, factory);
            int res = OWLObject.hashIteration(hashIndex(), argument.hashCode());
            res = OWLObject.hashIteration(res, predicate.hashCode());
            this.hashCode = res;
            return new Object[]{fromPredicate(p, predicate), fromArgument(a, argument)};
        }

        Object fromPredicate(ONT_P predicate, Object object) {
            return predicate.isURIResource() ? predicate.asNode().getURI() : object;
        }

        abstract Object fromArgument(ONT_A arg, ONTObject<? extends OWL_A> object);

        abstract ONTObject<? extends OWL_P> mapPredicate(ONT_P p, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_A> mapArgument(ONT_A a, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_P> toPredicate(Object item, InternalObjectFactory factory);

        abstract ONTObject<? extends OWL_A> toArgument(Object item, InternalObjectFactory factory);

    }

}
