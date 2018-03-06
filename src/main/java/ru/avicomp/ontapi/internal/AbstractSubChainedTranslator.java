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
 *
 */

package ru.avicomp.ontapi.internal;

import org.apache.jena.graph.FrontsTriple;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFList;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLObject;
import ru.avicomp.ontapi.jena.impl.OntObjectImpl;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntObject;
import ru.avicomp.ontapi.jena.model.OntStatement;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * The base class for
 * <ul>
 * <li>{@link HasKeyTranslator}</li>
 * <li>{@link SubPropertyChainOfTranslator}</li>
 * <li>{@link DisjointUnionTranslator}</li>
 * </ul>
 * Created by @szuev on 18.10.2016.
 */
public abstract class AbstractSubChainedTranslator<Axiom extends OWLLogicalAxiom, ONT extends OntObject,
        OWL_SUBJECT extends OWLObject, OWL_MEMBERS extends OWLObject> extends AxiomTranslator<Axiom> {

    abstract OWLObject getSubject(Axiom axiom);

    abstract Property getPredicate();

    abstract Stream<? extends OWLObject> getObjects(Axiom axiom);

    abstract Class<ONT> getView();

    @Override
    public void write(Axiom axiom, OntGraphModel model) {
        WriteHelper.writeList(model, getSubject(axiom), getPredicate(), getObjects(axiom), axiom.annotations());
    }

    @Override
    public Stream<OntStatement> statements(OntGraphModel model) {
        return model.statements(null, getPredicate(), null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(getView()))
                .filter(s -> s.getObject().canAs(RDFList.class));
    }

    @Override
    public boolean testStatement(OntStatement statement) {
        return statement.getSubject().equals(getPredicate())
                && statement.getSubject().canAs(getView())
                && statement.getObject().canAs(RDFList.class);
    }

    InternalObject<Axiom> makeAxiom(ConfigProvider.Config conf,
                                    OntStatement statement,
                                    Function<ONT, InternalObject<? extends OWL_SUBJECT>> subjectExtractor,
                                    Function<ONT, Collection<InternalObject<? extends OWL_MEMBERS>>> membersExtractor,
                                    TriFunction<InternalObject<? extends OWL_SUBJECT>,
                                            Collection<InternalObject<? extends OWL_MEMBERS>>,
                                            Collection<InternalObject<OWLAnnotation>>, Axiom> axiomMaker) {

        ONT clazz = statement.getSubject().as(getView());
        InternalObject<? extends OWL_SUBJECT> subject = subjectExtractor.apply(clazz);
        Collection<InternalObject<? extends OWL_MEMBERS>> members = membersExtractor.apply(clazz);
        Collection<InternalObject<OWLAnnotation>> annotations = getAnnotations(statement, conf);
        Axiom res = axiomMaker.apply(subject, members, annotations);
        return InternalObject.create(res, statement)
                .append(() -> ((OntObjectImpl) statement.getSubject()).rdfListContent(getPredicate()).map(FrontsTriple::asTriple));

    }

    interface TriFunction<A, B, C, R> {
        R apply(A a, B b, C c);
    }
}
