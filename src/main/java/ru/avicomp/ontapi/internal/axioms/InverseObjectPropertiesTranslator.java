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

package ru.avicomp.ontapi.internal.axioms;

import org.apache.jena.util.iterator.ExtendedIterator;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.model.OntGraphModel;
import ru.avicomp.ontapi.jena.model.OntOPE;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.utils.Models;
import ru.avicomp.ontapi.jena.vocabulary.OWL;

import java.util.Collection;

/**
 * Example:
 * <pre>{@code
 * pizza:hasBase owl:inverseOf pizza:isBaseOf ;
 * }</pre>
 * <p>
 * Created by @szuev on 30.09.2016.
 */
public class InverseObjectPropertiesTranslator extends AxiomTranslator<OWLInverseObjectPropertiesAxiom> {
    @Override
    public void write(OWLInverseObjectPropertiesAxiom axiom, OntGraphModel model) {
        WriteHelper.writeTriple(model, axiom.getFirstProperty(), OWL.inverseOf, axiom.getSecondProperty(), axiom.annotations());
    }

    @Override
    public ExtendedIterator<OntStatement> listStatements(OntGraphModel model, InternalConfig config) {
        // NOTE as a precaution: the first (commented) way is not correct
        // since it includes anonymous object property expressions (based on owl:inverseOf),
        // which might be treat as separated axioms, but OWL-API doesn't think so.
        /*return model.statements(null, OWL.inverseOf, null)
                .filter(OntStatement::isLocal)
                .filter(s -> s.getSubject().canAs(OntOPE.class))
                .filter(s -> s.getObject().canAs(OntOPE.class));*/
        return Models.listLocalStatements(model, null, OWL.inverseOf, null) // skip {@code _:x owl:inverseOf PN}
                .filterDrop(s -> s.getSubject().isAnon() && s.getObject().isURIResource());
    }

    @Override
    public boolean testStatement(OntStatement statement, InternalConfig config) {
        if (!OWL.inverseOf.equals(statement.getPredicate())) return false;
        // skip {@code _:x owl:inverseOf PN} (inverse object property expression):
        if (statement.getSubject().isAnon() && statement.getObject().isURIResource()) return false;
        return statement.getSubject().canAs(OntOPE.class) && statement.getObject().canAs(OntOPE.class);
    }

    @Override
    public ONTObject<OWLInverseObjectPropertiesAxiom> toAxiom(OntStatement statement, InternalObjectFactory reader, InternalConfig config) {
        ONTObject<? extends OWLObjectPropertyExpression> f = reader.get(statement.getSubject(OntOPE.class));
        ONTObject<? extends OWLObjectPropertyExpression> s = reader.get(statement.getObject().as(OntOPE.class));
        Collection<ONTObject<OWLAnnotation>> annotations = reader.get(statement, config);
        OWLInverseObjectPropertiesAxiom res = reader.getOWLDataFactory()
                .getOWLInverseObjectPropertiesAxiom(f.getObject(), s.getObject(), ONTObject.extract(annotations));
        return ONTObject.create(res, statement).append(annotations).append(f).append(s);
    }
}
