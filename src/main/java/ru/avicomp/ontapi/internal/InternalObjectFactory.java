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

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.semanticweb.owlapi.model.*;
import ru.avicomp.ontapi.DataFactory;
import ru.avicomp.ontapi.OntApiException;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.jena.model.*;

import java.util.Collection;

/**
 * Internal Object Factory to map {@link OntObject} =&gt; {@link OWLObject}.
 * Used by the {@link InternalModel} while read objects from the graph.
 * It is a functional analogue of {@code uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals}.
 * <p>
 * Created by @szuev on 14.03.2018.
 *
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/owl/owlapi/OWLDataFactoryInternals.java'>uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternals</a>
 * @see ONTObject
 */
public interface InternalObjectFactory {

    InternalObjectFactory DEFAULT = new NoCacheObjectFactory(OntManagers.getDataFactory());

    void clear();

    DataFactory getOWLDataFactory();

    ONTObject<OWLClass> get(OntClass ce);

    ONTObject<OWLDatatype> get(OntDT dr);

    ONTObject<OWLObjectProperty> get(OntNOP nop);

    ONTObject<OWLAnnotationProperty> get(OntNAP nap);

    ONTObject<OWLDataProperty> get(OntNDP ndp);

    ONTObject<OWLNamedIndividual> get(OntIndividual.Named i);

    ONTObject<OWLAnonymousIndividual> get(OntIndividual.Anonymous i);

    ONTObject<OWLFacetRestriction> get(OntFR fr);

    ONTObject<SWRLVariable> get(OntSWRL.Variable var);

    ONTObject<OWLLiteral> get(Literal literal);

    ONTObject<? extends OWLClassExpression> get(OntCE ce);

    ONTObject<? extends OWLDataRange> get(OntDR dr);

    ONTObject<? extends OWLObjectPropertyExpression> get(OntOPE ope);

    ONTObject<? extends OWLPropertyExpression> get(OntDOP property);

    ONTObject<? extends OWLIndividual> get(OntIndividual i);

    ONTObject<? extends OWLAnnotationValue> getValue(RDFNode value);

    ONTObject<? extends OWLAnnotationSubject> getSubject(OntObject subject);

    ONTObject<? extends SWRLAtom> get(OntSWRL.Atom atom);

    Collection<ONTObject<OWLAnnotation>> get(OntStatement statement, InternalConfig config);

    ONTObject<IRI> asIRI(OntObject s);

    default ONTObject<? extends OWLEntity> get(OntEntity e) {
        if (e instanceof OntClass) {
            return get((OntClass) e);
        }
        if (e instanceof OntDT) {
            return get((OntDT) e);
        }
        if (e instanceof OntIndividual.Named) {
            return get((OntIndividual.Named) e);
        }
        if (e instanceof OntNAP) {
            return get((OntNAP) e);
        }
        if (e instanceof OntNDP) {
            return get((OntNDP) e);
        }
        if (e instanceof OntNOP) {
            return get((OntNOP) e);
        }
        throw new OntApiException("Unsupported " + e);
    }

    default IRI toIRI(String str) {
        return IRI.create(OntApiException.notNull(str, "Null IRI."));
    }

    default ONTObject<? extends OWLPropertyExpression> get(OntPE property) {
        if (OntApiException.notNull(property, "Null property.").canAs(OntNAP.class)) {
            return get(property.as(OntNAP.class));
        }
        return get((OntDOP) property);
    }

}
