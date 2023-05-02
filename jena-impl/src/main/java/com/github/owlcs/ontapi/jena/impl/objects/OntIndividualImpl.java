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

package com.github.owlcs.ontapi.jena.impl.objects;

import com.github.owlcs.ontapi.jena.OntJenaException;
import com.github.owlcs.ontapi.jena.impl.OntGraphModelImpl;
import com.github.owlcs.ontapi.jena.impl.PersonalityModel;
import com.github.owlcs.ontapi.jena.impl.conf.Factories;
import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.impl.conf.OntFinder;
import com.github.owlcs.ontapi.jena.impl.conf.OntPersonality;
import com.github.owlcs.ontapi.jena.model.OntClass;
import com.github.owlcs.ontapi.jena.model.OntIndividual;
import com.github.owlcs.ontapi.jena.model.OntNegativeAssertion;
import com.github.owlcs.ontapi.jena.model.OntObject;
import com.github.owlcs.ontapi.jena.model.OntStatement;
import com.github.owlcs.ontapi.jena.utils.Iterators;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import com.github.owlcs.ontapi.jena.vocabulary.SWRL;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link OntIndividual} implementation, both for anonymous and named individuals.
 * <p>
 * Created @ssz on 09.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntIndividualImpl extends OntObjectImpl implements OntIndividual {

    private static final String FORBIDDEN_SUBJECTS = Anonymous.class.getName() + ".InSubject";
    private static final String FORBIDDEN_OBJECTS = Anonymous.class.getName() + ".InObject";

    // allowed predicates for subject (the pattern '_:x p ANY'):
    private static final Set<Node> FOR_SUBJECT = Stream.of(OWL.sameAs, OWL.differentFrom)
            .map(FrontsNode::asNode).collect(Collectors.toUnmodifiableSet());
    // allowed predicates for object (the pattern 'ANY p _:x'):
    private static final Set<Node> FOR_OBJECT = Stream.of(OWL.sameAs, OWL.differentFrom,
            OWL.sourceIndividual, OWL.targetIndividual, OWL.hasValue,
            OWL.annotatedSource, OWL.annotatedTarget,
            RDF.first, SWRL.argument1, SWRL.argument2)
            .map(FrontsNode::asNode).collect(Collectors.toUnmodifiableSet());

    public static OntFinder FINDER = OntFinder.ANY_SUBJECT_AND_OBJECT;
    public static ObjectFactory anonymousIndividualFactory = Factories.createCommon(AnonymousImpl.class, FINDER,
            OntIndividualImpl::testAnonymousIndividual);

    public static ObjectFactory abstractIndividualFactory = Factories.createFrom(FINDER,
            Named.class, Anonymous.class);

    public OntIndividualImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public Stream<OntClass> classes() {
        return Iterators.asStream(listClasses(), getCharacteristics());
    }

    @Override
    public Stream<OntClass> classes(boolean direct) {
        return Iterators.fromSet(() -> getClasses(direct));
    }

    /**
     * Lists all right parts from class assertion statements where this individual is at subject position.
     *
     * @return {@link ExtendedIterator} over all direct {@link OntClass class}-types
     */
    public ExtendedIterator<OntClass> listClasses() {
        return listObjects(RDF.type, OntClass.class);
    }

    /**
     * Lists all right parts from class assertion statements where this individual is at subject position,
     * and also all their super-classes if the parameter {@code direct} is {@code false}.
     *
     * @param direct if {@code true} returns only direct types, just like {@link #listClasses()}
     * @return {@link ExtendedIterator} over all {@link OntClass class}-types
     * @see #listClasses()
     */
    public ExtendedIterator<OntClass> listClasses(boolean direct) {
        return Iterators.create(() -> getClasses(direct).iterator());
    }

    /**
     * Returns a {@code Set} of all class-types,
     * including their super-classes if the parameter {@code direct} is {@code false}.
     *
     * @param direct if {@code true} returns only direct types, just like {@code #listClasses().toSet()}
     * @return a {@code Set} of all {@link OntClass class}-types
     */
    public Set<OntClass> getClasses(boolean direct) {
        if (direct) {
            return listClasses().toSet();
        }
        Set<OntClass> res = new HashSet<>();
        Function<OntClass, ExtendedIterator<OntClass>> listSuperClasses =
                x -> ((OntObjectImpl) x).listObjects(RDFS.subClassOf, OntClass.class);
        listObjects(RDF.type, OntClass.class).forEachRemaining(c -> collectIndirect(c, listSuperClasses, res));
        return res;
    }

    @Override
    public boolean isLocal() {
        Optional<OntStatement> root = findRootStatement();
        return (root.isPresent() && root.get().isLocal()) || hasLocalClassAssertions();
    }

    protected boolean hasLocalClassAssertions() {
        return Iterators.findFirst(listClassAssertions().filterKeep(OntStatement::isLocal)).isPresent();
    }

    /**
     * Lists all class assertion statements.
     *
     * @return {@link ExtendedIterator} over all class assertions.
     */
    public ExtendedIterator<OntStatement> listClassAssertions() {
        return listStatements(RDF.type).filterKeep(s -> s.getObject().canAs(OntClass.class));
    }

    public static boolean testAnonymousIndividual(Node node, EnhGraph eg) {
        if (!node.isBlank()) {
            return false;
        }
        boolean hasType = false;
        // class-assertion:
        ExtendedIterator<Node> types = eg.asGraph().find(node, RDF.Nodes.type, Node.ANY).mapWith(Triple::getObject);
        try {
            while (types.hasNext()) {
                if (PersonalityModel.canAs(OntClass.class, types.next(), eg)) return true;
                hasType = true;
            }
        } finally {
            types.close();
        }
        // any other typed statement (builtin, such as owl:AllDifferent):
        if (hasType) {
            return false;
        }
        OntPersonality personality = PersonalityModel.asPersonalityModel(eg).getOntPersonality();
        OntPersonality.Builtins builtins = personality.getBuiltins();
        OntPersonality.Reserved reserved = personality.getReserved();

        // all known predicates whose subject definitely cannot be an individual
        Set<Node> forbiddenSubjects = reserved.get(FORBIDDEN_SUBJECTS, () -> {
            Set<Node> bSet = builtins.getProperties();
            return reserved.getProperties().stream()
                    .filter(n -> !bSet.contains(n))
                    .filter(n -> !FOR_SUBJECT.contains(n))
                    .collect(Collectors.toUnmodifiableSet());
        });
        // _:x @built-in-predicate @any:
        ExtendedIterator<Node> bySubject = eg.asGraph().find(node, Node.ANY, Node.ANY).mapWith(Triple::getPredicate);
        try {
            while (bySubject.hasNext()) {
                if (forbiddenSubjects.contains(bySubject.next()))
                    return false;
            }
        } finally {
            bySubject.close();
        }
        // all known predicates whose object definitely cannot be an individual
        Set<Node> forbiddenObjects = reserved.get(FORBIDDEN_OBJECTS, () -> {
            Set<Node> bSet = builtins.getProperties();
            return reserved.getProperties().stream()
                    .filter(n -> !bSet.contains(n))
                    .filter(n -> !FOR_OBJECT.contains(n))
                    .collect(Collectors.toUnmodifiableSet());
        });
        // @any @built-in-predicate _:x
        ExtendedIterator<Node> byObject = eg.asGraph().find(Node.ANY, Node.ANY, node).mapWith(Triple::getPredicate);
        try {
            while (byObject.hasNext()) {
                if (forbiddenObjects.contains(byObject.next()))
                    return false;
            }
        } finally {
            byObject.close();
        }
        // tolerantly allow any other blank node to be treated as anonymous individual:
        return true;
    }

    public static Anonymous createAnonymousIndividual(RDFNode node) {
        if (OntJenaException.notNull(node, "Null node.").canAs(Anonymous.class))
            return node.as(Anonymous.class);
        if (node.isAnon()) {
            return new AnonymousImpl(node.asNode(), (EnhGraph) node.getModel());
        }
        throw new OntJenaException.Conversion(node + " could not be " + Anonymous.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Stream<OntNegativeAssertion> negativeAssertions() {
        return Iterators.asStream(listNegativeAssertions(), getCharacteristics());
    }

    @SuppressWarnings("rawtypes")
    public ExtendedIterator<OntNegativeAssertion> listNegativeAssertions() {
        return listSubjects(OWL.sourceIndividual, OntNegativeAssertion.class);
    }

    @Override
    protected Set<OntStatement> getContent() {
        Set<OntStatement> res = super.getContent();
        listNegativeAssertions().forEachRemaining(x -> res.addAll(((OntObjectImpl) x).getContent()));
        return res;
    }

    /**
     * Represents a named individual.
     * Note: it may not have {@link OntObject#getMainStatement()} statement.
     */
    public static class NamedImpl extends OntIndividualImpl implements Named {
        public NamedImpl(Node n, EnhGraph m) {
            super(OntObjectImpl.checkNamed(n), m);
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return Optional.of(getModel().createStatement(this, RDF.type, OWL.NamedIndividual).asRootStatement())
                    .filter(r -> getModel().contains(r));
        }

        @Override
        public boolean isBuiltIn() {
            return false;
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Named.class;
        }

        @Override
        public NamedImpl detachClass(Resource clazz) {
            OntGraphModelImpl m = getModel();
            m.listOntStatements(this, RDF.type, clazz)
                    .filterDrop(s -> OWL.NamedIndividual.equals(s.getObject()))
                    .toList()
                    .forEach(s -> m.remove(s.clearAnnotations()));
            return this;
        }
    }

    /**
     * See description to the interface {@link Anonymous}.
     * The current implementation allows treating b-node as anonymous individual
     * in any case except the following cases:
     * <ul>
     * <li>it is a subject in statement "_:x rdf:type s", where "s" is not a class expression ("C").</li>
     * <li>it is a subject in statement "_:x @predicate @any", where @predicate is from reserved vocabulary
     * but not object, data or annotation built-in property
     * and not owl:sameAs and owl:differentFrom.</li>
     * <li>it is an object in statement "@any @predicate _:x", where @predicate is from reserved vocabulary
     * but not object, data or annotation built-in property
     * and not owl:sameAs, owl:differentFrom, owl:hasValue, owl:sourceIndividual and rdf:first.</li>
     * </ul>
     * <p>
     * for notations and self-education see our main <a href="https://www.w3.org/TR/owl2-quick-reference/">OWL2 Quick Refs</a>
     */
    public static class AnonymousImpl extends OntIndividualImpl implements Anonymous {

        public AnonymousImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public boolean isLocal() {
            return hasLocalClassAssertions();
        }

        @Override
        public Optional<OntStatement> findRootStatement() {
            return Optional.empty();
        }

        @Override
        public Class<? extends OntObject> getActualClass() {
            return Anonymous.class;
        }

        @Override
        public AnonymousImpl detachClass(Resource clazz) {
            Set<OntClass> classes = classes().collect(Collectors.toSet());
            if (clazz == null && !classes.isEmpty()) {
                throw new OntJenaException.IllegalState("Detaching classes is prohibited: " +
                        "the anonymous individual (" + this + ") should contain at least one class assertion, " +
                        "otherwise it can be lost");
            }
            if (classes.size() == 1 && classes.iterator().next().equals(clazz)) {
                throw new OntJenaException.IllegalState("Detaching class (" + clazz + ") is prohibited: " +
                        "it is a single class assertion for the individual " + this + ".");
            }
            remove(RDF.type, clazz);
            return this;
        }

    }
}
