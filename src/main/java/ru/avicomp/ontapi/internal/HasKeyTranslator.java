/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2017, Avicomp Services, AO
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

import java.util.stream.Stream;

import org.apache.jena.rdf.model.Property;
import org.semanticweb.owlapi.model.*;

import ru.avicomp.ontapi.jena.model.OntCE;
import ru.avicomp.ontapi.jena.model.OntNDP;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

/**
 * base class : {@link AbstractSubChainedTranslator}
 * for HasKey axiom.
 * example:
 * :MyClass1 owl:hasKey ( :ob-prop-1 ) .
 * <p>
 * Created by @szuev on 17.10.2016.
 */
public class HasKeyTranslator extends AbstractSubChainedTranslator<OWLHasKeyAxiom, OntCE> {
    @Override
    OWLObject getSubject(OWLHasKeyAxiom axiom) {
        return axiom.getClassExpression();
    }

    @Override
    Property getPredicate() {
        return OWL.hasKey;
    }

    @Override
    Stream<? extends OWLObject> getObjects(OWLHasKeyAxiom axiom) {
        return axiom.propertyExpressions();
    }

    @Override
    Class<OntCE> getView() {
        return OntCE.class;
    }

    @Override
    public InternalObject<OWLHasKeyAxiom> asAxiom(OntStatement statement) {
        ConfigProvider.Config conf = getConfig(statement);
        OntCE ce = statement.getSubject().as(OntCE.class);
        InternalObject<? extends OWLClassExpression> subject = ReadHelper.fetchClassExpression(ce, conf.dataFactory());
        InternalObject.Collection<? extends OWLPropertyExpression> members = InternalObject.Collection.create(ce.hasKey()
                .filter(p -> p.canAs(OntOPE.class) || p.canAs(OntNDP.class)) // only P or R (!)
                .map(p -> ReadHelper.getProperty(p, conf.dataFactory())));
        InternalObject.Collection<OWLAnnotation> annotations = ReadHelper.getStatementAnnotations(statement, conf.dataFactory(), conf.loaderConfig());
        OWLHasKeyAxiom res = conf.dataFactory().getOWLHasKeyAxiom(subject.getObject(), members.getObjects(), annotations.getObjects());
        return InternalObject.create(res, content(statement)).add(annotations.getTriples()).add(members.getTriples());
    }
}
