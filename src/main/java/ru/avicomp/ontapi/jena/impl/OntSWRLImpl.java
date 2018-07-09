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

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Ont SWRL Object Implementation.
 * <p>
 * Created by @szuev on 18.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntSWRLImpl extends OntObjectImpl implements OntSWRL {
    private static final OntFilter VAR_SWRL_FILTER = OntFilter.URI.and(new OntFilter.HasType(SWRL.Variable));

    public static OntObjectFactory variableSWRLFactory = new CommonOntObjectFactory(new OntMaker.WithType(VariableImpl.class, SWRL.Variable),
            new OntFinder.ByType(SWRL.Variable), VAR_SWRL_FILTER);

    public static OntObjectFactory dArgSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(DArgImpl.class),
                    OntFinder.ANY_SUBJECT_AND_OBJECT, VAR_SWRL_FILTER.or(LiteralImpl.factory::canWrap));
    public static OntObjectFactory iArgSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(IArgImpl.class),
                    OntFinder.ANY_SUBJECT, VAR_SWRL_FILTER.or((n, g) -> OntObjectImpl.canAs(OntIndividual.class, n, g)));
    public static OntObjectFactory abstractArgSWRLFactory = new MultiOntObjectFactory(OntFinder.ANY_SUBJECT_AND_OBJECT, null, dArgSWRLFactory, iArgSWRLFactory);

    public static OntObjectFactory builtInAtomSWRLFactory = makeAtomFactory(BuiltInAtomImpl.class, SWRL.BuiltinAtom);
    public static OntObjectFactory classAtomSWRLFactory = makeAtomFactory(OntClassAtomImpl.class, SWRL.ClassAtom);
    public static OntObjectFactory dataRangeAtomSWRLFactory = makeAtomFactory(DataRangeAtomImpl.class, SWRL.DataRangeAtom);
    public static OntObjectFactory dataValuedAtomSWRLFactory = makeAtomFactory(DataPropertyAtomImpl.class, SWRL.DatavaluedPropertyAtom);
    public static OntObjectFactory individualAtomSWRLFactory = makeAtomFactory(ObjectPropertyAtomImpl.class, SWRL.IndividualPropertyAtom);
    public static OntObjectFactory differentIndividualsAtomSWRLFactory = makeAtomFactory(DifferentIndividualsAtomImpl.class, SWRL.DifferentIndividualsAtom);
    public static OntObjectFactory sameIndividualsAtomSWRLFactory = makeAtomFactory(SameIndividualsAtomImpl.class, SWRL.SameIndividualAtom);
    public static OntObjectFactory abstractAtomSWRLFactory = new MultiOntObjectFactory(OntFinder.TYPED, null,
            builtInAtomSWRLFactory, classAtomSWRLFactory, dataRangeAtomSWRLFactory, dataValuedAtomSWRLFactory,
            individualAtomSWRLFactory, differentIndividualsAtomSWRLFactory, sameIndividualsAtomSWRLFactory);

    public static OntObjectFactory impSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(ImpImpl.class), new OntFinder.ByType(SWRL.Imp), new OntFilter.HasType(SWRL.Imp));
    public static OntObjectFactory abstractSWRLFactory = new MultiOntObjectFactory(OntFinder.TYPED, null, abstractAtomSWRLFactory, variableSWRLFactory, impSWRLFactory);

    private static OntObjectFactory makeAtomFactory(Class<? extends AtomImpl> view, Resource type) {
        return new CommonOntObjectFactory(new OntMaker.Default(view),
                new OntFinder.ByType(type), OntFilter.BLANK.and(new OntFilter.HasType(type)));
    }

    public OntSWRLImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public static Variable createVariable(OntGraphModelImpl model, String uri) {
        return model.createOntObject(Variable.class, uri);
    }

    public static Atom.BuiltIn createBuiltInAtom(OntGraphModelImpl model, Resource predicate, Stream<DArg> arguments) {
        OntObjectImpl.checkNamed(predicate);
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.BuiltinAtom);
        model.add(res, SWRL.builtin, predicate);
        model.add(predicate, RDF.type, SWRL.Builtin); // ?
        model.add(res, SWRL.arguments, model.createList(arguments.iterator()));
        return model.getNodeAs(res.asNode(), Atom.BuiltIn.class);
    }

    public static Atom.OntClass createClassAtom(OntGraphModelImpl model, OntCE clazz, IArg arg) {
        OntJenaException.notNull(clazz, "Null class");
        OntJenaException.notNull(arg, "Null i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.ClassAtom);
        model.add(res, SWRL.classPredicate, clazz);
        model.add(res, SWRL.argument1, arg);
        return model.getNodeAs(res.asNode(), Atom.OntClass.class);
    }

    public static Atom.DataRange createDataRangeAtom(OntGraphModelImpl model, OntDR range, DArg arg) {
        OntJenaException.notNull(range, "Null data range");
        OntJenaException.notNull(arg, "Null d-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.DataRangeAtom);
        model.add(res, SWRL.dataRange, range);
        model.add(res, SWRL.argument1, arg);
        return model.getNodeAs(res.asNode(), Atom.DataRange.class);
    }

    public static Atom.DataProperty createDataPropertyAtom(OntGraphModelImpl model, OntNDP dataProperty, IArg firstArg, DArg secondArg) {
        OntJenaException.notNull(dataProperty, "Null data property");
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second d-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.DatavaluedPropertyAtom);
        model.add(res, SWRL.propertyPredicate, dataProperty);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.DataProperty.class);
    }

    public static Atom.ObjectProperty createObjectPropertyAtom(OntGraphModelImpl model, OntOPE objectProperty, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(objectProperty, "Null object property");
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.IndividualPropertyAtom);
        model.add(res, SWRL.propertyPredicate, objectProperty);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.ObjectProperty.class);
    }

    public static Atom.DifferentIndividuals createDifferentIndividualsAtom(OntGraphModelImpl model, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.DifferentIndividualsAtom);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.DifferentIndividuals.class);
    }

    public static Atom.SameIndividuals createSameIndividualsAtom(OntGraphModelImpl model, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.SameIndividualAtom);
        model.add(res, SWRL.argument1, firstArg);
        model.add(res, SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.SameIndividuals.class);
    }

    public static Imp createImp(OntGraphModelImpl model, Stream<Atom> head, Stream<Atom> body) {
        OntJenaException.notNull(head, "Null head");
        OntJenaException.notNull(body, "Null body");
        Resource res = model.createResource();
        model.add(res, RDF.type, SWRL.Imp);
        model.add(res, SWRL.head, Models.createTypedList(model, SWRL.AtomList, head));
        model.add(res, SWRL.body, Models.createTypedList(model, SWRL.AtomList, body));
        return model.getNodeAs(res.asNode(), Imp.class);
    }

    public static class VariableImpl extends OntSWRLImpl implements Variable {
        public VariableImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static class DArgImpl extends OntObjectImpl implements DArg {
        public DArgImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static class IArgImpl extends OntObjectImpl implements IArg {
        public IArgImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static abstract class AtomImpl<P extends RDFNode> extends OntSWRLImpl implements Atom<P> {
        public AtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }
    }

    public static class BuiltInAtomImpl extends AtomImpl<Resource> implements Atom.BuiltIn {
        public BuiltInAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Resource getPredicate() { // should be uri
            return getRequiredObject(SWRL.builtin, Resource.class);
        }

        @Override
        public Stream<DArg> arguments() {
            return rdfListMembers(SWRL.arguments, DArg.class);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(super.spec(),
                    statement(SWRL.builtin).map(Stream::of).orElse(Stream.empty()),
                    rdfListContent(SWRL.arguments)
            ).flatMap(Function.identity());
        }
    }

    public static abstract class UnaryImpl<O extends OntObject, A extends Arg> extends AtomImpl<O> implements Atom.Unary<O, A> {
        private final Property predicate;
        private final Class<O> objectView;
        private final Class<A> argView;

        UnaryImpl(Node n, EnhGraph m, Property predicate, Class<O> objectView, Class<A> argView) {
            super(n, m);
            this.predicate = predicate;
            this.objectView = objectView;
            this.argView = argView;
        }

        @Override
        public A getArg() {
            return getRequiredObject(SWRL.argument1, argView);
        }

        @Override
        public O getPredicate() {
            return getRequiredObject(predicate, objectView);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(super.spec(),
                    statement(predicate).map(Stream::of).orElse(Stream.empty()),
                    statement(SWRL.argument1).map(Stream::of).orElse(Stream.empty())
            ).flatMap(Function.identity());
        }
    }

    public static class OntClassAtomImpl extends UnaryImpl<OntCE, IArg> implements Atom.OntClass {
        public OntClassAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.classPredicate, OntCE.class, IArg.class);
        }
    }

    public static class DataRangeAtomImpl extends UnaryImpl<OntDR, DArg> implements Atom.DataRange {
        public DataRangeAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.dataRange, OntDR.class, DArg.class);
        }
    }

    public static abstract class BinaryImpl<O extends Resource, F extends Arg, S extends Arg> extends AtomImpl<O> implements Atom.Binary<O, F, S> {
        protected final Property predicate;
        private final Class<O> objectView;
        private final Class<F> firstArgView;
        private final Class<S> secondArgView;

        BinaryImpl(Node n, EnhGraph m, Property predicate, Class<O> objectView, Class<F> firstArgView, Class<S> secondArgView) {
            super(n, m);
            this.predicate = predicate;
            this.objectView = objectView;
            this.firstArgView = firstArgView;
            this.secondArgView = secondArgView;
        }

        @Override
        public O getPredicate() {
            return getRequiredObject(predicate, objectView);
        }

        @Override
        public F getFirstArg() {
            return getRequiredObject(SWRL.argument1, firstArgView);
        }

        @Override
        public S getSecondArg() {
            return getRequiredObject(SWRL.argument2, secondArgView);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(super.spec(),
                    statement(predicate).map(Stream::of).orElse(Stream.empty()),
                    statement(SWRL.argument1).map(Stream::of).orElse(Stream.empty()),
                    statement(SWRL.argument2).map(Stream::of).orElse(Stream.empty())
            ).flatMap(Function.identity());
        }
    }

    public static class DataPropertyAtomImpl extends BinaryImpl<OntNDP, IArg, DArg> implements Atom.DataProperty {
        public DataPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.propertyPredicate, OntNDP.class, IArg.class, DArg.class);
        }
    }

    public static class ObjectPropertyAtomImpl extends BinaryImpl<OntOPE, IArg, IArg> implements Atom.ObjectProperty {
        public ObjectPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.propertyPredicate, OntOPE.class, IArg.class, IArg.class);
        }
    }

    public static abstract class IndividualsAtomImpl extends BinaryImpl<Property, IArg, IArg> {
        IndividualsAtomImpl(Node n, EnhGraph m, Property predicate) {
            super(n, m, predicate, Property.class, IArg.class, IArg.class);
        }

        @Override
        public Property getPredicate() {
            return predicate;
        }
    }

    public static class DifferentIndividualsAtomImpl extends IndividualsAtomImpl implements Atom.DifferentIndividuals {
        public DifferentIndividualsAtomImpl(Node n, EnhGraph m) {
            super(n, m, OWL.differentFrom);
        }
    }

    public static class SameIndividualsAtomImpl extends IndividualsAtomImpl implements Atom.SameIndividuals {
        public SameIndividualsAtomImpl(Node n, EnhGraph m) {
            super(n, m, OWL.sameAs);
        }
    }

    public static class ImpImpl extends OntSWRLImpl implements Imp {
        public ImpImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        protected Stream<Atom> list(Property predicate) {
            Statement st = getProperty(predicate);
            if (st == null || !st.getObject().isResource())
                return Stream.empty();
            Resource list = st.getObject().asResource();
            if (list.listProperties().filterDrop(s -> RDF.type.equals(s.getPredicate()) && SWRL.AtomList.equals(s.getObject())).toSet().isEmpty()) {
                // swrl:AtomicList is a stupid huck. Some formats (e.g. json-ld) ignore any  types in rdf:List
                // special case of list. empty list would be like this: "[ a  swrl:AtomList ]"
                return Stream.empty();
            }
            return Iter.asStream(list.as(RDFList.class).iterator()).filter(n -> n.canAs(Atom.class)).map(n -> n.as(Atom.class)).distinct();
        }

        @Override
        public Stream<Atom> head() {
            return list(SWRL.head);
        }

        @Override
        public Stream<Atom> body() {
            return list(SWRL.body);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(super.spec(), rdfListContent(SWRL.head), rdfListContent(SWRL.body)).flatMap(Function.identity());
        }
    }
}

