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

package com.github.owlcs.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.enhanced.EnhNode;
import org.apache.jena.enhanced.Implementation;
import org.apache.jena.enhanced.UnsupportedPolymorphismException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.impl.LiteralImpl;
import org.apache.jena.rdf.model.impl.RDFListImpl;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.NullIterator;
import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.impl.conf.*;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Ont SWRL Object Implementation.
 * <p>
 * Created by @szuev on 18.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public class OntSWRLImpl extends OntObjectImpl implements OntSWRL {
    private static final OntFilter VARIABLE_FILTER = OntFilter.URI.and(new OntFilter.HasType(SWRL.Variable));
    private static final OntFilter BUILTIN_FILTER = (n, g) -> {
        if (!n.isURI())
            return false;
        OntPersonality p = PersonalityModel.asPersonalityModel(g).getOntPersonality();
        if (p.getBuiltins().get(Builtin.class).contains(n)) {
            return true;
        }
        return Iter.findFirst(g.asGraph().find(n, RDF.Nodes.type, SWRL.Builtin.asNode())).isPresent();
    };

    public static ObjectFactory variableSWRLFactory = Factories.createCommon(
            new OntMaker.WithType(VariableImpl.class, SWRL.Variable),
            new OntFinder.ByType(SWRL.Variable), VARIABLE_FILTER);

    public static ObjectFactory builtinWRLFactory = Factories.createCommon(
            new OntMaker.WithType(BuiltinImpl.class, SWRL.Builtin),
            new OntFinder.ByType(SWRL.Builtin), BUILTIN_FILTER);

    public static ObjectFactory dArgSWRLFactory = Factories.createCommon(DArgImpl.class,
            OntFinder.ANY_SUBJECT_AND_OBJECT, VARIABLE_FILTER.or(LiteralImpl.factory::canWrap));

    public static ObjectFactory iArgSWRLFactory = Factories.createCommon(IArgImpl.class,
            OntFinder.ANY_SUBJECT_AND_OBJECT, VARIABLE_FILTER.or((n, g) -> PersonalityModel.canAs(OntIndividual.class, n, g)));

    public static ObjectFactory abstractArgSWRLFactory = Factories.createFrom(OntFinder.ANY_SUBJECT_AND_OBJECT
            , DArg.class
            , IArg.class);

    public static ObjectFactory builtInAtomSWRLFactory =
            makeAtomFactory(BuiltInAtomImpl.class, SWRL.BuiltinAtom);

    public static ObjectFactory classAtomSWRLFactory =
            makeAtomFactory(OntClassAtomImpl.class, SWRL.ClassAtom);

    public static ObjectFactory dataRangeAtomSWRLFactory =
            makeAtomFactory(DataRangeAtomImpl.class, SWRL.DataRangeAtom);

    public static ObjectFactory dataValuedAtomSWRLFactory =
            makeAtomFactory(DataPropertyAtomImpl.class, SWRL.DatavaluedPropertyAtom);

    public static ObjectFactory individualAtomSWRLFactory =
            makeAtomFactory(ObjectPropertyAtomImpl.class, SWRL.IndividualPropertyAtom);

    public static ObjectFactory differentIndividualsAtomSWRLFactory =
            makeAtomFactory(DifferentIndividualsAtomImpl.class, SWRL.DifferentIndividualsAtom);

    public static ObjectFactory sameIndividualsAtomSWRLFactory =
            makeAtomFactory(SameIndividualsAtomImpl.class, SWRL.SameIndividualAtom);

    public static ObjectFactory abstractAtomSWRLFactory = Factories.createFrom(OntFinder.TYPED
            , Atom.BuiltIn.class
            , Atom.OntClass.class
            , Atom.DataRange.class
            , Atom.DataProperty.class
            , Atom.ObjectProperty.class
            , Atom.DifferentIndividuals.class
            , Atom.SameIndividuals.class);

    public static ObjectFactory abstractBinarySWRLFactory = Factories.createFrom(OntFinder.TYPED
            , Atom.DataProperty.class
            , Atom.ObjectProperty.class
            , Atom.DifferentIndividuals.class
            , Atom.SameIndividuals.class);
    public static ObjectFactory abstractUnarySWRLFactory = Factories.createFrom(OntFinder.TYPED
            , Atom.OntClass.class
            , Atom.DataRange.class);
    public static ObjectFactory abstractSWRLFactory = Factories.createFrom(OntFinder.TYPED
            , Atom.BuiltIn.class
            , Atom.OntClass.class
            , Atom.DataRange.class
            , Atom.DataProperty.class
            , Atom.ObjectProperty.class
            , Atom.DifferentIndividuals.class
            , Atom.SameIndividuals.class
            , Builtin.class
            , Variable.class
            , Imp.class);

    public static ObjectFactory impSWRLFactory = new SWRLImplFactory();
    //Factories.createCommon(ImpImpl.class, new OntFinder.ByType(SWRL.Imp), new OntFilter.HasType(SWRL.Imp));

    private static ObjectFactory makeAtomFactory(Class<? extends AtomImpl> view, Resource type) {
        return Factories.createCommon(new OntMaker.Default(view),
                new OntFinder.ByType(type), OntFilter.BLANK.and(new OntFilter.HasType(type)));
    }

    public OntSWRLImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public static Builtin fetchBuiltinEntity(OntGraphModelImpl model, String uri) {
        Builtin res = model.findNodeAs(NodeFactory.createURI(OntJenaException.notNull(uri, "Null uri.")), Builtin.class);
        if (res == null) {
            res = createBuiltinEntity(model, uri);
        }
        return res;
    }

    public static Builtin createBuiltinEntity(OntGraphModelImpl model, String uri) {
        return model.createOntObject(Builtin.class, Objects.requireNonNull(uri));
    }

    public static Variable createVariable(OntGraphModelImpl model, String uri) {
        return model.createOntObject(Variable.class, uri);
    }

    public static Atom.BuiltIn createBuiltInAtom(OntGraphModelImpl model,
                                                 Resource predicate,
                                                 Collection<DArg> arguments) {
        Builtin property = fetchBuiltinEntity(model, predicate.getURI());
        OntObject res = model.createResource(SWRL.BuiltinAtom).addProperty(SWRL.builtin, property).as(OntObject.class);
        OntListImpl.create(model, res, SWRL.arguments, null, DArg.class, Iter.create(arguments));
        return model.getNodeAs(res.asNode(), Atom.BuiltIn.class);
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
        model.fetchOntEntity(OntNOP.class, OWL.differentFrom.getURI());
        return model.getNodeAs(res.asNode(), Atom.DifferentIndividuals.class);
    }

    public static Atom.SameIndividuals createSameIndividualsAtom(OntGraphModelImpl model,
                                                                 IArg firstArg,
                                                                 IArg secondArg) {
        OntJenaException.notNull(firstArg, "Null first i-arg");
        OntJenaException.notNull(secondArg, "Null second i-arg");
        Resource res = model.createResource(SWRL.SameIndividualAtom)
                .addProperty(SWRL.argument1, firstArg)
                .addProperty(SWRL.argument2, secondArg);
        model.fetchOntEntity(OntNOP.class, OWL.sameAs.getURI());
        return model.getNodeAs(res.asNode(), Atom.SameIndividuals.class);
    }

    public static Imp createImp(OntGraphModelImpl model,
                                Collection<Atom> head,
                                Collection<Atom> body) {
        OntJenaException.notNull(head, "Null head");
        OntJenaException.notNull(body, "Null body");
        OntObject res = model.createResource(SWRL.Imp).as(OntObject.class);
        OntListImpl.create(model, res, SWRL.head, SWRL.AtomList, Atom.class, Iter.create(head));
        OntListImpl.create(model, res, SWRL.body, SWRL.AtomList, Atom.class, Iter.create(body));
        return model.getNodeAs(res.asNode(), Imp.class);
    }

    public static class BuiltinImpl extends OntSWRLImpl implements Builtin {
        public BuiltinImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Builtin.class;
        }
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

        /**
         * Answers the encapsulated node as {@link Literal}.
         *
         * @return {@link Literal}
         * @throws UnsupportedPolymorphismException if not a literal
         */
        @Override
        public Literal asLiteral() throws UnsupportedPolymorphismException {
            return as(Literal.class);
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return node.isLiteral() ? NullIterator.instance() : ((VariableImpl) as(Variable.class)).listSpec();
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

    public static abstract class AtomImpl<P extends OntObject> extends OntSWRLImpl implements Atom<P> {
        public AtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return getRequiredRootStatement(this, getResourceType());
        }

        public abstract Resource getResourceType();
    }

    public static class BuiltInAtomImpl extends AtomImpl<Builtin> implements Atom.BuiltIn {
        public BuiltInAtomImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Resource getResourceType() {
            return SWRL.BuiltinAtom;
        }

        @Override
        public Builtin getPredicate() {
            return getRequiredObject(SWRL.builtin, Builtin.class);
        }

        @Override
        public OntListImpl<DArg> getArgList() {
            return OntListImpl.asOntList(getRequiredObject(SWRL.arguments, RDFList.class),
                    getModel(), this, SWRL.arguments, null, DArg.class);
        }

        public ExtendedIterator<OntStatement> listPredicateStatements() {
            OntStatement p = getRequiredProperty(SWRL.builtin);
            OntStatement b = getPredicate().getRoot();
            return b == null ? Iter.of(p) : Iter.of(p, b);
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listPredicateStatements(), getArgList().listContent());
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
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listRequired(predicate, SWRL.argument1));
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

    public static abstract class BinaryImpl<O extends OntObject, F extends Arg, S extends Arg> extends AtomImpl<O> implements Atom.Binary<O, F, S> {
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
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), listPredicateSpec(), listRequired(SWRL.argument1, SWRL.argument2));
        }

        protected ExtendedIterator<OntStatement> listPredicateSpec() {
            return listRequired(predicate);
        }
    }

    public static abstract class PropertyAtomImpl<P extends OntPE, A extends Arg> extends BinaryImpl<P, IArg, A> {

        PropertyAtomImpl(Node n, EnhGraph m, Class<P> objectType, Class<A> secondArgType) {
            super(n, m, SWRL.propertyPredicate, objectType, IArg.class, secondArgType);
        }
    }

    public static class DataPropertyAtomImpl extends PropertyAtomImpl<OntNDP, DArg> implements Atom.DataProperty {
        public DataPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m, OntNDP.class, DArg.class);
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

    public static class ObjectPropertyAtomImpl extends PropertyAtomImpl<OntOPE, IArg> implements Atom.ObjectProperty {
        public ObjectPropertyAtomImpl(Node n, EnhGraph m) {
            super(n, m, OntOPE.class, IArg.class);
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

    public static abstract class IndividualsAtomImpl extends BinaryImpl<OntNOP, IArg, IArg> {
        public IndividualsAtomImpl(Node n, EnhGraph m, Property predicate) {
            super(n, m, predicate, OntNOP.class, IArg.class, IArg.class);
        }

        @Override
        public OntNOP getPredicate() {
            return getModel().fetchOntEntity(OntNOP.class, predicate.getURI());
        }

        @Override
        protected ExtendedIterator<OntStatement> listPredicateSpec() {
            OntStatement s = getPredicate().getRoot();
            return s == null ? NullIterator.instance() : Iter.of(s);
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
        public OntListImpl<Atom> getHeadList() {
            return getList(SWRL.head);
        }

        @Override
        public OntListImpl<Atom> getBodyList() {
            return getList(SWRL.body);
        }

        protected OntListImpl<Atom> getList(Property predicate) {
            RDFList list = getRequiredObject(predicate, RDFList.class);
            return OntListImpl.asOntList(list, getModel(), this, predicate, SWRL.AtomList, Atom.class);
        }

        @Override
        public ExtendedIterator<OntStatement> listSpec() {
            return Iter.concat(super.listSpec(), getHeadList().listContent(), getBodyList().listContent());
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

    public static class SWRLImplFactory extends BaseFactoryImpl {
        private static final Node IMP = SWRL.Imp.asNode();
        private static final Node BODY = SWRL.body.asNode();
        private static final Node HEAD = SWRL.head.asNode();
        private static final Node LIST = SWRL.AtomList.asNode();

        private static final Implementation LIST_FACTORY = RDFListImpl.factory;

        @Override
        public ExtendedIterator<EnhNode> iterator(EnhGraph eg) {
            return eg.asGraph().find(Node.ANY, RDF.Nodes.type, IMP)
                    .filterKeep(t -> hasAtomList(HEAD, t.getSubject(), eg) && hasAtomList(BODY, t.getSubject(), eg))
                    .mapWith(t -> createInstance(t.getSubject(), eg));
        }

        @Override
        public boolean canWrap(Node node, EnhGraph eg) {
            return eg.asGraph().contains(node, RDF.Nodes.type, IMP)
                    && hasAtomList(HEAD, node, eg)
                    && hasAtomList(BODY, node, eg);
        }

        @Override
        public EnhNode createInstance(Node node, EnhGraph eg) {
            return new ImpImpl(node, eg);
        }

        private boolean hasAtomList(Node p, Node node, EnhGraph eg) {
            return Iter.findFirst(eg.asGraph().find(node, p, Node.ANY)
                    .filterKeep(t -> isAtomList(t.getObject(), eg)))
                    .isPresent();
        }

        private boolean isAtomList(Node n, EnhGraph eg) {
            if (RDF.Nodes.nil.equals(n)) return true;
            return eg.asGraph().contains(n, RDF.Nodes.type, LIST) && LIST_FACTORY.canWrap(n, eg);
        }
    }
}

