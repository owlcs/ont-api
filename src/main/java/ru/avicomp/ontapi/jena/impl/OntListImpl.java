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
import org.apache.jena.enhanced.UnsupportedPolymorphismException;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.rdf.model.impl.ResourceImpl;
import org.apache.jena.shared.PropertyNotFoundException;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntList;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.utils.Iter;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;
import java.util.Objects;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * TODO:
 * Created by @szuev on 10.07.2018.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntListImpl<E extends RDFNode> extends ResourceImpl implements OntList<E> {
    private static final UnaryOperator<RDFList> IDENTITY = UnaryOperator.identity();

    protected final Resource subject;
    protected final Property predicate;
    protected final Class<E> type;
    private RDFList objectRDFList;


    protected OntListImpl(Resource subject, Property predicate, RDFList object, OntGraphModel model, Class<E> type) {
        super(object.asNode(), (EnhGraph) model);
        this.objectRDFList = object;
        this.subject = subject;
        this.predicate = predicate;
        this.type = type;
    }

    public static <N extends RDFNode> OntList<N> create(OntObject subject, Property predicate, Class<N> type, Collection<N> elements) {
        Objects.requireNonNull(subject, "Null subject");
        Objects.requireNonNull(predicate, "Null predicate");
        Objects.requireNonNull(type, "Null type");
        OntGraphModel m = Objects.requireNonNull(subject.getModel(), "Null model");
        RDFList list = elements.isEmpty() ? m.createList() : m.createList(elements.iterator());
        m.add(subject, predicate, list);
        return new OntListImpl<N>(subject, predicate, list, m, type) {
            @Override
            public boolean isValid(RDFNode n) {
                return true;
            }
        };
    }

    public static <N extends RDFNode> OntList<N> wrap(OntObject subject, Property predicate, RDFList list, Class<N> type) {
        return new OntListImpl<N>(Objects.requireNonNull(subject, "Null subject"),
                Objects.requireNonNull(predicate, "Null predicate"),
                Objects.requireNonNull(list, "Null []-list"),
                Objects.requireNonNull(subject.getModel(), "Null model"),
                Objects.requireNonNull(type, "Null type")) {
            @Override
            public boolean isValid(RDFNode n) {
                return n.canAs(type);
            }
        };
    }

    public static <N extends RDFNode> Stream<OntList<N>> stream(OntObject subject, Property predicate, Class<N> type) {
        return subject.objects(predicate, RDFList.class).map(list -> wrap(subject, predicate, list, type));
    }

    @Override
    public OntGraphModelImpl getModel() {
        return (OntGraphModelImpl) enhGraph;
    }

    protected RDFList getRDFList() {
        return setRDFList(IDENTITY).objectRDFList;
    }

    protected OntListImpl<E> setRDFList(UnaryOperator<RDFList> operation) throws OntJenaException.IllegalState {
        RDFList list = Objects.requireNonNull(operation).apply(this.objectRDFList);
        Model m = getModel();
        Statement s = m.createStatement(subject, predicate, list);
        if (!m.contains(s)) {
            throw new OntJenaException.IllegalState(Models.toString(s) + " does not exist");
        }
        this.objectRDFList = list;
        // todo: handle annotations
        return this;
    }

    @Override
    public Node asNode() {
        return getRDFList().asNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends RDFNode> T as(Class<T> t) throws UnsupportedPolymorphismException {
        if (RDFList.class.equals(t))
            return (T) getRDFList();
        return super.as(t);
    }

    @Override
    public <X extends RDFNode> boolean canAs(Class<X> t) {
        return RDFList.class.equals(t) || super.canAs(t);
    }

    @Override
    public boolean isEmpty() {
        return isEmpty(getRDFList());
    }

    public static boolean isEmpty(RDFList list) {
        return RDF.nil.equals(list);
    }

    @Override
    public Stream<E> members() {
        OntGraphModelImpl m = getModel();
        return Iter.asStream(getRDFList().iterator())
                .filter(this::isValid)
                .map(n -> {
                    try {
                        return m.getNodeAs(n.asNode(), type);
                    } catch (OntJenaException.Conversion j) {
                        throw new OntJenaException.IllegalState("Problem node: '" + n + "'", j);
                    }
                });
    }

    public abstract boolean isValid(RDFNode n);

    @Override
    public OntList<E> add(E e) {
        return setRDFList(list -> {
            if (isEmpty(list)) {
                RDFList tmp = list.with(e);
                getModel().remove(subject, predicate, RDF.nil).add(subject, predicate, tmp);
                return tmp;
            }
            list.add(e);
            return list;
        });
    }

    @Override
    public OntList<E> remove() {
        return setRDFList(list -> {
            if (isEmpty(list)) return list;
            Model model = getModel();
            RDFList tmp = list;
            StmtIterator last = null;
            StmtIterator prev;
            do {
                prev = last;
                last = tmp.listProperties();
                tmp = tmp.getTail();
            } while (!tmp.isEmpty());
            model.remove(last);
            if (prev == null) { // rdf:nil
                model.removeAll(subject, predicate, null).add(subject, predicate, list = model.createList());
                return list;
            }
            Statement st = Iter.asStream(prev)
                    .filter(s -> RDF.rest.equals(s.getPredicate()))
                    .findFirst().orElseThrow(() -> new OntJenaException("Illegal state of " + this + ": can't find rdf:rest"));
            model.remove(st).add(st.getSubject(), st.getPredicate(), RDF.nil);
            return list;
        });
    }

    @Override
    public OntList<E> addFirst(E e) throws PropertyNotFoundException {
        return setRDFList(list -> {
            Model model = getModel();
            RDFList res;
            if (isEmpty(list)) {
                res = list.with(e);
                model.remove(subject, predicate, RDF.nil).add(subject, predicate, res);
                return res;
            }
            res = model.createList(new RDFNode[]{e});
            Statement rest = res.getRequiredProperty(RDF.rest);
            model.remove(rest).remove(subject, predicate, list)
                    .add(rest.getSubject(), rest.getPredicate(), list).add(subject, predicate, res);
            return res;
        });
    }

    @Override
    public OntList<E> removeFirst() throws PropertyNotFoundException {
        return setRDFList(list -> {
            if (isEmpty(list)) return list;
            Model model = getModel();
            Statement rest = list.getRequiredProperty(RDF.rest);
            Statement first = list.getRequiredProperty(RDF.first);
            model.remove(first).remove(rest).removeAll(subject, predicate, null).add(subject, predicate, rest.getObject());
            return rest.getObject().as(RDFList.class);
        });
    }

    @Override
    public OntList<E> clear() {
        return setRDFList(list -> {
            if (isEmpty(list)) return list;
            Model model = getModel();
            list.removeList();
            RDFList res;
            model.removeAll(subject, predicate, null).add(subject, predicate, res = model.createList());
            return res;
        });
    }

    @Override
    public OntList<E> get(int index) throws PropertyNotFoundException, OntJenaException.IllegalArgument {
        if (index < 0) throw new OntJenaException.IllegalArgument("Negative index: " + index);
        if (index == 0) return this;
        RDFList tmp = getRDFList();
        int i = 0;
        while (!isEmpty(tmp)) {
            Statement rest = tmp.getRequiredProperty(RDF.rest);
            tmp = rest.getObject().as(RDFList.class);
            if (++i != index) {
                continue;
            }
            return new OntListImpl<E>(rest.getSubject(), rest.getPredicate(), tmp, getModel(), type) {
                @Override
                public boolean isValid(RDFNode n) {
                    return OntListImpl.this.isValid(n);
                }
            };
        }
        throw new OntJenaException.IllegalArgument("Index out of bounds: " + index);
    }

}