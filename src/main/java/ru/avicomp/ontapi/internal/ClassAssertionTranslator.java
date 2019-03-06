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

import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import ru.avicomp.ontapi.jena.model.*;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.RDF;

import java.util.Collection;

/**
 * Creating individual (both named and anonymous):
 * <pre>{@code pizza:France rdf:type owl:NamedIndividual, pizza:Country, owl:Thing.}</pre>
 * <p>
 * Created by @szuev on 28.09.2016.
 */
@SuppressWarnings("WeakerAccess")
public class ClassAssertionTranslator extends AxiomTranslator<OWLClassAssertionAxiom> {

    @Override
    public void write(OWLClassAssertionAxiom axiom, OntGraphModel model) {
        OntCE ce = WriteHelper.addClassExpression(model, axiom.getClassExpression());
        OWLIndividual individual = axiom.getIndividual();
        OntObject subject = individual.isAnonymous() ?
                WriteHelper.toResource(individual).inModel(model).as(OntObject.class) :
                WriteHelper.addIndividual(model, individual);
        OntStatement statement = subject.addStatement(RDF.type, ce);
        WriteHelper.addAnnotations(statement, axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        return Models.listStatements(model, null, RDF.type, null).filterKeep(this::filterSO);
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        return statement.isDeclaration() && filterSO(statement);
    }

    protected boolean filterSO(OntStatement statement) {
        // first class then individual,
        // since anonymous individual has more sophisticated and time-consuming checking
        return isClass(statement.getObject()) && isIndividual(statement.getSubject());
    }

    protected boolean isClass(RDFNode n) {
        return n.canAs(OntCE.class);
    }

    protected boolean isIndividual(RDFNode n) {
        return n.canAs(OntIndividual.class);
    }

    @Override
    public ONTObject<OWLClassAssertionAxiom> toAxiom(OntStatement statement, InternalObjectFactory reader, InternalConfig config) {
        ONTObject<? extends OWLIndividual> i = reader.get(statement.getSubject(OntIndividual.class));
        ONTObject<? extends OWLClassExpression> ce = reader.get(statement.getObject().as(OntCE.class));

        Collection<ONTObject<OWLAnnotation>> annotations = reader.get(statement, config);
        OWLClassAssertionAxiom res = reader.getOWLDataFactory()
                .getOWLClassAssertionAxiom(ce.getObject(), i.getObject(), ONTObject.extract(annotations));
        return ONTObject.create(res, statement).append(annotations).append(i).append(ce);
    }
}
