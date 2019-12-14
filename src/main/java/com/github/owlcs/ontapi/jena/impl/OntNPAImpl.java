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

import com.github.owlcs.ontapi.jena.impl.conf.ObjectFactory;
import com.github.owlcs.ontapi.jena.impl.conf.OntFilter;
import com.github.owlcs.ontapi.jena.impl.conf.OntFinder;
import com.github.owlcs.ontapi.jena.model.*;
import com.github.owlcs.ontapi.jena.utils.Iter;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import com.github.owlcs.ontapi.jena.vocabulary.RDF;
import org.apache.jena.enhanced.EnhGraph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.iterator.ExtendedIterator;

import java.util.Optional;

/**
 * Implementation of the Negative Property Assertion.
 * <p>
 * Created by @szuev on 15.11.2016.
 */
@SuppressWarnings("WeakerAccess")
public abstract class OntNPAImpl<P extends OntPE, T extends RDFNode> extends OntObjectImpl implements OntNPA<P, T> {
    private static OntFinder NPA_FINDER = new OntFinder.ByType(OWL.NegativePropertyAssertion);
    private static OntFilter NPA_FILTER = OntFilter.BLANK
            .and(new OntFilter.HasPredicate(OWL.sourceIndividual))
            .and(new OntFilter.HasPredicate(OWL.assertionProperty));

    public static ObjectFactory objectNPAFactory = Factories.createCommon(ObjectAssertionImpl.class,
            NPA_FINDER, NPA_FILTER, new OntFilter.HasPredicate(OWL.targetIndividual));
    public static ObjectFactory dataNPAFactory = Factories.createCommon(DataAssertionImpl.class,
            NPA_FINDER, NPA_FILTER, new OntFilter.HasPredicate(OWL.targetValue));
    public static ObjectFactory abstractNPAFactory = Factories.createFrom(NPA_FINDER
            , ObjectAssertion.class
            , DataAssertion.class);

    public OntNPAImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    public static DataAssertion create(OntGraphModelImpl model,
                                       OntIndividual source,
                                       OntNDP property,
                                       Literal target) {
        Resource res = create(model, source).addProperty(OWL.assertionProperty, property)
                .addProperty(OWL.targetValue, target);
        return model.getNodeAs(res.asNode(), DataAssertion.class);
    }

    public static ObjectAssertion create(OntGraphModelImpl model,
                                         OntIndividual source,
                                         OntOPE property,
                                         OntIndividual target) {
        Resource res = create(model, source)
                .addProperty(OWL.assertionProperty, property)
                .addProperty(OWL.targetIndividual, target);
        return model.getNodeAs(res.asNode(), ObjectAssertion.class);
    }

    @Override
    public Optional<OntStatement> findRootStatement() {
        return getRequiredRootStatement(this, OWL.NegativePropertyAssertion);
    }

    @Override
    public ExtendedIterator<OntStatement> listSpec() {
        return Iter.concat(super.listSpec(), listRequired(OWL.sourceIndividual, OWL.assertionProperty, targetPredicate()));
    }

    abstract Class<P> propertyClass();

    abstract Property targetPredicate();

    @Override
    public OntIndividual getSource() {
        return getRequiredObject(OWL.sourceIndividual, OntIndividual.class);
    }

    @Override
    public P getProperty() {
        return getRequiredObject(OWL.assertionProperty, propertyClass());
    }

    private static Resource create(OntModel model, OntIndividual source) {
        Resource res = model.createResource();
        res.addProperty(RDF.type, OWL.NegativePropertyAssertion);
        res.addProperty(OWL.sourceIndividual, source);
        return res;
    }

    public static class ObjectAssertionImpl extends OntNPAImpl<OntOPE, OntIndividual> implements ObjectAssertion {
        public ObjectAssertionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        Class<OntOPE> propertyClass() {
            return OntOPE.class;
        }

        @Override
        Property targetPredicate() {
            return OWL.targetIndividual;
        }

        @Override
        public Class<ObjectAssertion> getActualClass() {
            return ObjectAssertion.class;
        }


        @Override
        public OntIndividual getTarget() {
            return getRequiredObject(targetPredicate(), OntIndividual.class);
        }

    }

    public static class DataAssertionImpl extends OntNPAImpl<OntNDP, Literal> implements DataAssertion {
        public DataAssertionImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        Class<OntNDP> propertyClass() {
            return OntNDP.class;
        }

        @Override
        Property targetPredicate() {
            return OWL.targetValue;
        }

        @Override
        public Class<DataAssertion> getActualClass() {
            return DataAssertion.class;
        }


        @Override
        public Literal getTarget() {
            return getRequiredObject(targetPredicate(), Literal.class);
        }
    }
}
