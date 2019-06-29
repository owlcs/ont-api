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

package ru.avicomp.ontapi.jena.impl;

import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.FrontsNode;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDFS;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.ObjectFactory;
import ru.avicomp.ontapi.jena.impl.conf.OntFinder;
import ru.avicomp.ontapi.jena.impl.conf.OntPersonality;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.vocabulary.OWL;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An {@link OntIndividual} implementation, both for anonymous and named individuals.
 * <p>
 * Created by szuev on 09.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntIndividualImpl extends OntObjectImpl implements OntIndividual {

    private static final String FORBIDDEN_SUBJECTS = OntIndividual.Anonymous.class.getName() + ".InSubject";
    private static final String FORBIDDEN_OBJECTS = OntIndividual.Anonymous.class.getName() + ".InObject";

    public static OntFinder FINDER = OntFinder.ANY_SUBJECT_AND_OBJECT;
    public static ObjectFactory anonymousIndividualFactory = Factories.createCommon(AnonymousImpl.class, FINDER,
            OntIndividualImpl::testAnonymousIndividual);

    public static ObjectFactory abstractIndividualFactory = Factories.createFrom(FINDER,
            OntIndividual.Named.class, OntIndividual.Anonymous.class);

    public OntIndividualImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public final Stream<OntCE> classes() {
        return Iter.asStream(listClasses());
    }

    @Override
    public final Stream<OntCE> classes(boolean direct) {
        return Iter.asStream(listClasses(direct));
    }

    /**
     * Lists all right parts from class assertion statements where this individual is at subject position.
     *
     * @return {@link ExtendedIterator} over all direct {@link OntCE class}-types
     */
    public ExtendedIterator<OntCE> listClasses() {
        return listObjects(RDF.type, OntCE.class);
    }

    /**
     * Lists all right parts from class assertion statements where this individual is at subject position,
     * and also all their super-classes if the parameter {@code direct} is {@code false}.
     *
     * @param direct if {@code true} returns only direct types, just like {@link #listClasses()}
     * @return {@link ExtendedIterator} over all {@link OntCE class}-types
     * @see #listClasses()
     * @since 1.4.2
     */
    public ExtendedIterator<OntCE> listClasses(boolean direct) {
        return Iter.create(() -> getClasses(direct).iterator());
    }

    /**
     * Returns a {@code Set} of all class-types,
     * including their super-classes if the parameter {@code direct} is {@code false}.
     *
     * @param direct if {@code true} returns only direct types, just like {@code #listClasses().toSet()}
     * @return a {@code Set} of all {@link OntCE class}-types
     * @since 1.4.2
     */
    public Set<OntCE> getClasses(boolean direct) {
        if (direct) {
            return listClasses().toSet();
        }
        Set<OntCE> res = new HashSet<>();
        Function<OntCE, ExtendedIterator<OntCE>> listSuperClasses =
                x -> ((OntObjectImpl) x).listObjects(RDFS.subClassOf, OntCE.class);
        listObjects(RDF.type, OntCE.class).forEachRemaining(c -> collectIndirect(c, listSuperClasses, res));
        return res;
    }

    @Override
    public boolean isLocal() {
        Optional<OntStatement> root = findRootStatement();
        return (root.isPresent() && root.get().isLocal()) || hasLocalClassAssertions();
    }

    protected boolean hasLocalClassAssertions() {
        return Iter.findFirst(listClassAssertions().filterKeep(OntStatement::isLocal)).isPresent();
    }

    /**
     * Lists all class assertion statements.
     *
     * @return {@link ExtendedIterator} over all class assertions.
     */
    public ExtendedIterator<OntStatement> listClassAssertions() {
        return listStatements(RDF.type).filterKeep(s -> s.getObject().canAs(OntCE.class));
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
                if (PersonalityModel.canAs(OntCE.class, types.next(), eg)) return true;
                hasType = true;
            }
        } finally {
            types.close();
        }
        // any other typed statement:
        if (hasType) {
            return false;
        }
        OntPersonality personality = PersonalityModel.asPersonalityModel(eg).getOntPersonality();
        OntPersonality.Builtins builtins = personality.getBuiltins();
        OntPersonality.Reserved reserved = personality.getReserved();

        Set<Node> forbiddenSubjects = reserved.get(FORBIDDEN_SUBJECTS, () -> {
            Set<Node> allowed = Stream.concat(builtins.getProperties().stream(),
                    Stream.of(OWL.sameAs.asNode(), OWL.differentFrom.asNode()))
                    .collect(Collectors.toSet());
            return reserved.getProperties().stream().filter(n -> !allowed.contains(n)).collect(Iter.toUnmodifiableSet());
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
        Set<Node> forbiddenObjects = reserved.get(FORBIDDEN_OBJECTS, () -> {
            Set<Node> allowed = Stream.concat(builtins.getProperties().stream(),
                    Stream.of(OWL.sameAs, OWL.differentFrom, OWL.sourceIndividual, OWL.hasValue, RDF.first)
                            .map(FrontsNode::asNode)).collect(Collectors.toSet());
            return reserved.getProperties().stream().filter(n -> !allowed.contains(n)).collect(Iter.toUnmodifiableSet());
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

    public static OntIndividual.Anonymous createAnonymousIndividual(RDFNode node) {
        if (OntJenaException.notNull(node, "Null node.").canAs(OntIndividual.Anonymous.class))
            return node.as(OntIndividual.Anonymous.class);
        if (node.isAnon()) {
            return new AnonymousImpl(node.asNode(), (EnhGraph) node.getModel());
        }
        throw new OntJenaException.Conversion(node + " could not be " + OntIndividual.Anonymous.class);
    }

    @Override
    public final Stream<OntNPA> negativeAssertions() {
        return Iter.asStream(listNegativeAssertions());
    }

    public ExtendedIterator<OntNPA> listNegativeAssertions() {
        return listSubjects(OWL.sourceIndividual, OntNPA.class);
    }

    @Override
    protected Set<OntStatement> getContent() {
        Set<OntStatement> res = super.getContent();
        listNegativeAssertions().forEachRemaining(x -> res.addAll(((OntObjectImpl) x).getContent()));
        return res;
    }

    /**
     * Represents a named individual.
     * Note: it may not have {@link OntObject#getRoot()} statement.
     */
    public static class NamedImpl extends OntIndividualImpl implements OntIndividual.Named {
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
            return OntIndividual.Named.class;
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
     * See description to the interface {@link OntIndividual.Anonymous}.
     * The current implementation allows treating b-node as anonymous individual
     * in any case with exception of the following:
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
     * for notations and self-education see our main <a href='https://www.w3.org/TR/owl2-quick-reference/'>OWL2 Quick Refs</a>
     */
    public static class AnonymousImpl extends OntIndividualImpl implements OntIndividual.Anonymous {

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
            return OntIndividual.Anonymous.class;
        }

        @Override
        public AnonymousImpl detachClass(Resource clazz) {
            Set<OntCE> classes = classes().collect(Collectors.toSet());
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
