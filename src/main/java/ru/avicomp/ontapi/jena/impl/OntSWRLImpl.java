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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.SWRL;

import java.util.Collection;
import java.util.Optional;
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

    public static OntObjectFactory variableSWRLFactory =
            new CommonOntObjectFactory(new OntMaker.WithType(VariableImpl.class, SWRL.Variable),
            new OntFinder.ByType(SWRL.Variable), VAR_SWRL_FILTER);

    public static OntObjectFactory dArgSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(DArgImpl.class),
            OntFinder.ANY_SUBJECT_AND_OBJECT, VAR_SWRL_FILTER.or(LiteralImpl.factory::canWrap));
    public static OntObjectFactory iArgSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(IArgImpl.class),
            OntFinder.ANY_SUBJECT, VAR_SWRL_FILTER.or((n, g) -> OntObjectImpl.canAs(OntIndividual.class, n, g)));
    public static OntObjectFactory abstractArgSWRLFactory = new MultiOntObjectFactory(OntFinder.ANY_SUBJECT_AND_OBJECT,
            null, dArgSWRLFactory, iArgSWRLFactory);

    public static OntObjectFactory builtInAtomSWRLFactory =
            makeAtomFactory(BuiltInAtomImpl.class, SWRL.BuiltinAtom);
    public static OntObjectFactory classAtomSWRLFactory =
            makeAtomFactory(OntClassAtomImpl.class, SWRL.ClassAtom);
    public static OntObjectFactory dataRangeAtomSWRLFactory =
            makeAtomFactory(DataRangeAtomImpl.class, SWRL.DataRangeAtom);
    public static OntObjectFactory dataValuedAtomSWRLFactory =
            makeAtomFactory(DataPropertyAtomImpl.class, SWRL.DatavaluedPropertyAtom);
    public static OntObjectFactory individualAtomSWRLFactory =
            makeAtomFactory(ObjectPropertyAtomImpl.class, SWRL.IndividualPropertyAtom);
    public static OntObjectFactory differentIndividualsAtomSWRLFactory =
            makeAtomFactory(DifferentIndividualsAtomImpl.class, SWRL.DifferentIndividualsAtom);
    public static OntObjectFactory sameIndividualsAtomSWRLFactory =
            makeAtomFactory(SameIndividualsAtomImpl.class, SWRL.SameIndividualAtom);
    public static OntObjectFactory abstractAtomSWRLFactory = new MultiOntObjectFactory(OntFinder.TYPED, null,
            builtInAtomSWRLFactory, classAtomSWRLFactory, dataRangeAtomSWRLFactory, dataValuedAtomSWRLFactory,
            individualAtomSWRLFactory, differentIndividualsAtomSWRLFactory, sameIndividualsAtomSWRLFactory);

    public static OntObjectFactory impSWRLFactory = new CommonOntObjectFactory(new OntMaker.Default(ImpImpl.class),
            new OntFinder.ByType(SWRL.Imp), new OntFilter.HasType(SWRL.Imp));
    public static OntObjectFactory abstractSWRLFactory = new MultiOntObjectFactory(OntFinder.TYPED, null,
            abstractAtomSWRLFactory, variableSWRLFactory, impSWRLFactory);

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

    public static Atom.BuiltIn createBuiltInAtom(OntGraphModelImpl model,
                                                 Resource predicate,
                                                 Collection<DArg> arguments) {
        Property property = createBuiltinProperty(model, predicate);
        OntObject res = model.createResource(SWRL.BuiltinAtom).addProperty(SWRL.builtin, property).as(OntObject.class);
        OntListImpl.create(model, res, SWRL.arguments, null, DArg.class, arguments.iterator());
        return model.getNodeAs(res.asNode(), Atom.BuiltIn.class);
    }

    public static Property createBuiltinProperty(OntGraphModelImpl model, Resource predicate) {
        return checkNamed(predicate).inModel(model).addProperty(RDF.type, SWRL.Builtin).as(Property.class);
    }

    public static Atom.OntClass createClassAtom(OntGraphModelImpl model, OntCE clazz, IArg arg) {
        OntJenaException.notNull(clazz, "Null class");
        OntJenaException.notNull(arg, "Null i-arg");
        Resource res = model.createResource(SWRL.ClassAtom)
                .addProperty(SWRL.classPredicate, clazz)
                .addProperty(SWRL.argument1, arg);
        return model.getNodeAs(res.asNode(), Atom.OntClass.class);
    }

    public static Atom.DataRange createDataRangeAtom(OntGraphModelImpl model, OntDR range, DArg arg) {
        OntJenaException.notNull(range, "Null data range");
        OntJenaException.notNull(arg, "Null d-arg");
        Resource res = model.createResource(SWRL.DataRangeAtom)
                .addProperty(SWRL.dataRange, range)
                .addProperty(SWRL.argument1, arg);
        return model.getNodeAs(res.asNode(), Atom.DataRange.class);
    }

    public static Atom.DataProperty createDataPropertyAtom(OntGraphModelImpl model,
                                                           OntNDP dataProperty,
                                                           IArg firstArg,
                                                           DArg secondArg) {
        OntJenaException.notNull(dataProperty, "Null data property");
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second d-arg");
        Resource res = model.createResource(SWRL.DatavaluedPropertyAtom)
                .addProperty(SWRL.propertyPredicate, dataProperty)
                .addProperty(SWRL.argument1, firstArg)
                .addProperty(SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.DataProperty.class);
    }

    public static Atom.ObjectProperty createObjectPropertyAtom(OntGraphModelImpl model,
                                                               OntOPE objectProperty,
                                                               IArg firstArg,
                                                               IArg secondArg) {
        OntJenaException.notNull(objectProperty, "Null object property");
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource(SWRL.IndividualPropertyAtom)
                .addProperty(SWRL.propertyPredicate, objectProperty)
                .addProperty(SWRL.argument1, firstArg)
                .addProperty(SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.ObjectProperty.class);
    }

    public static Atom.DifferentIndividuals createDifferentIndividualsAtom(OntGraphModelImpl model,
                                                                           IArg firstArg,
                                                                           IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource(SWRL.DifferentIndividualsAtom)
                .addProperty(SWRL.argument1, firstArg)
                .addProperty(SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.DifferentIndividuals.class);
    }

    public static Atom.SameIndividuals createSameIndividualsAtom(OntGraphModelImpl model, IArg firstArg, IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource(SWRL.SameIndividualAtom)
                .addProperty(SWRL.argument1, firstArg)
                .addProperty(SWRL.argument2, secondArg);
        return model.getNodeAs(res.asNode(), Atom.SameIndividuals.class);
    }

    public static Imp createImp(OntGraphModelImpl model, Collection<Atom> head, Collection<Atom> body) {
        OntJenaException.notNull(head, "Null head");
        OntJenaException.notNull(body, "Null body");
        OntObject res = model.createResource(SWRL.Imp).as(OntObject.class);
        OntListImpl.create(model, res, SWRL.head, SWRL.AtomList, Atom.class, head.iterator());
        OntListImpl.create(model, res, SWRL.body, SWRL.AtomList, Atom.class, body.iterator());
        return model.getNodeAs(res.asNode(), Imp.class);
    }

    public static class VariableImpl extends OntSWRLImpl implements Variable {
        public VariableImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Variable.class;
        }
    }

    public static class DArgImpl extends OntObjectImpl implements DArg {
        public DArgImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return DArg.class;
        }
    }

    public static class IArgImpl extends OntObjectImpl implements IArg {
        public IArgImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return IArg.class;
        }
    }

    public static abstract class AtomImpl<P extends RDFNode> extends OntSWRLImpl implements Atom<P> {
        public AtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return getRequiredRootStatement(this, getResourceType());
        }

        public abstract Resource getResourceType();
    }

    public static class BuiltInAtomImpl extends AtomImpl<Resource> implements Atom.BuiltIn {
        public BuiltInAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.BuiltinAtom;
        }

        @Override
        public Resource getPredicate() { // should be uri
            return getRequiredObject(SWRL.builtin, Resource.class);
        }

        @Override
        public OntList<DArg> getArgList() {
            return OntListImpl.asOntList(getRequiredObject(SWRL.arguments, RDFList.class),
                    getModel(), this, SWRL.arguments, null, DArg.class);
        }

        public Stream<OntStatement> predicateStatements() {
            OntStatement b = getRequiredProperty(SWRL.builtin);
            OntStatement a = b.getSubject().statement(RDF.type, SWRL.Builtin)
                    .orElseThrow(() -> new OntJenaException.IllegalState("Can't find rdf:type SWRL:Builtin for " + b));
            return Stream.of(a, b);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(super.spec(), predicateStatements(), getArgList().content()).flatMap(Function.identity());
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.BuiltIn.class;
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
            return Stream.concat(super.spec(), required(predicate, SWRL.argument1));
        }
    }

    public static class OntClassAtomImpl extends UnaryImpl<OntCE, IArg> implements Atom.OntClass {
        public OntClassAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.classPredicate, OntCE.class, IArg.class);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.ClassAtom;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.OntClass.class;
        }
    }

    public static class DataRangeAtomImpl extends UnaryImpl<OntDR, DArg> implements Atom.DataRange {
        public DataRangeAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.dataRange, OntDR.class, DArg.class);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.DataRangeAtom;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.DataRange.class;
        }
    }

    public static abstract class BinaryImpl<O extends Resource, F extends Arg, S extends Arg> extends AtomImpl<O> implements Atom.Binary<O, F, S> {
        protected final Property predicate;
        private final Class<O> objectType;
        private final Class<F> firstArgType;
        private final Class<S> secondArgType;

        BinaryImpl(Node n,
                   EnhGraph m,
                   Property predicate,
                   Class<O> objectType,
                   Class<F> firstArgType,
                   Class<S> secondArgType) {
            super(n, m);
            this.predicate = predicate;
            this.objectType = objectType;
            this.firstArgType = firstArgType;
            this.secondArgType = secondArgType;
        }

        @Override
        public O getPredicate() {
            return getRequiredObject(predicate, objectType);
        }

        @Override
        public F getFirstArg() {
            return getRequiredObject(SWRL.argument1, firstArgType);
        }

        @Override
        public S getSecondArg() {
            return getRequiredObject(SWRL.argument2, secondArgType);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.concat(super.spec(), required(predicate, SWRL.argument1, SWRL.argument2));
        }
    }

    public static class DataPropertyAtomImpl extends BinaryImpl<OntNDP, IArg, DArg> implements Atom.DataProperty {
        public DataPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.propertyPredicate, OntNDP.class, IArg.class, DArg.class);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.DatavaluedPropertyAtom;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.DataProperty.class;
        }
    }

    public static class ObjectPropertyAtomImpl extends BinaryImpl<OntOPE, IArg, IArg> implements Atom.ObjectProperty {
        public ObjectPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m, SWRL.propertyPredicate, OntOPE.class, IArg.class, IArg.class);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.IndividualPropertyAtom;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.ObjectProperty.class;
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

        @Override
        public Resource getResourceType() {
            return SWRL.DifferentIndividualsAtom;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.DifferentIndividuals.class;
        }
    }

    public static class SameIndividualsAtomImpl extends IndividualsAtomImpl implements Atom.SameIndividuals {
        public SameIndividualsAtomImpl(Node n, EnhGraph m) {
            super(n, m, OWL.sameAs);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.SameIndividualAtom;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Atom.SameIndividuals.class;
        }
    }

    public static class ImpImpl extends OntSWRLImpl implements Imp {

        public ImpImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public OntList<Atom> getHeadList() {
            return getList(SWRL.head);
        }

        @Override
        public OntList<Atom> getBodyList() {
            return getList(SWRL.body);
        }

        protected OntList<Atom> getList(Property predicate) {
            return OntListImpl.asOntList(getRequiredObject(predicate, RDFList.class),
                    getModel(), this, predicate, SWRL.AtomList, Atom.class);
        }

        @Override
        public Stream<OntStatement> spec() {
            return Stream.of(super.spec(), getHeadList().content(), getBodyList().content())
                    .flatMap(Function.identity());
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return getRequiredRootStatement(this, SWRL.Imp);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Imp.class;
        }
    }
}

