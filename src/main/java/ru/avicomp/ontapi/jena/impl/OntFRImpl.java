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
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import ru.avicomp.ontapi.jena.OntJenaException;
import ru.avicomp.ontapi.jena.impl.conf.*;
import ru.avicomp.ontapi.jena.model.OntFR;
import ru.avicomp.ontapi.jena.model.OntStatement;
import ru.avicomp.ontapi.jena.vocabulary.RDF;
import ru.avicomp.ontapi.jena.vocabulary.XSD;

/**
 * Implementation of Facet Restrictions
 * <p>
 * Created by @szuev on 16.11.2016.
 */
public abstract class OntFRImpl extends OntObjectImpl implements OntFR {
    public static OntObjectFactory lengthFRFactory =
            new CommonOntObjectFactory(makeMaker(LengthImpl.class), makeFinder(XSD.length), makeFilter(XSD.length));
    public static OntObjectFactory minLengthFRFactory =
            new CommonOntObjectFactory(makeMaker(MinLengthImpl.class), makeFinder(XSD.minLength), makeFilter(XSD.minLength));
    public static OntObjectFactory maxLengthFRFactory =
            new CommonOntObjectFactory(makeMaker(MaxLengthImpl.class), makeFinder(XSD.maxLength), makeFilter(XSD.maxLength));
    public static OntObjectFactory minInclusiveFRFactory =
            new CommonOntObjectFactory(makeMaker(MinInclusiveImpl.class), makeFinder(XSD.minInclusive), makeFilter(XSD.minInclusive));
    public static OntObjectFactory maxInclusiveFRFactory =
            new CommonOntObjectFactory(makeMaker(MaxInclusiveImpl.class), makeFinder(XSD.maxInclusive), makeFilter(XSD.maxInclusive));
    public static OntObjectFactory minExclusiveFRFactory =
            new CommonOntObjectFactory(makeMaker(MinExclusiveImpl.class), makeFinder(XSD.minExclusive), makeFilter(XSD.minExclusive));
    public static OntObjectFactory maxExclusiveFRFactory =
            new CommonOntObjectFactory(makeMaker(MaxExclusiveImpl.class), makeFinder(XSD.maxExclusive), makeFilter(XSD.maxExclusive));
    public static OntObjectFactory totalDigitsFRFactory =
            new CommonOntObjectFactory(makeMaker(TotalDigitsImpl.class), makeFinder(XSD.totalDigits), makeFilter(XSD.totalDigits));
    public static OntObjectFactory fractionDigitsFRFactory =
            new CommonOntObjectFactory(makeMaker(FractionDigitsImpl.class), makeFinder(XSD.fractionDigits), makeFilter(XSD.fractionDigits));
    public static OntObjectFactory patternFRFactory =
            new CommonOntObjectFactory(makeMaker(PatternImpl.class), makeFinder(XSD.pattern), makeFilter(XSD.pattern));
    public static OntObjectFactory langRangeFRFactory =
            new CommonOntObjectFactory(makeMaker(LangRangeImpl.class), makeFinder(RDF.langRange), makeFilter(RDF.langRange));

    public static OntObjectFactory abstractFRFactory = new MultiOntObjectFactory(OntFinder.ANY_SUBJECT, null,
            lengthFRFactory, minLengthFRFactory, maxLengthFRFactory,
            minInclusiveFRFactory, maxInclusiveFRFactory, minExclusiveFRFactory, maxExclusiveFRFactory,
            totalDigitsFRFactory, fractionDigitsFRFactory, patternFRFactory, langRangeFRFactory);

    public OntFRImpl(Node n, EnhGraph m) {
        super(n, m);
    }

    @Override
    public OntStatement getRoot() {
        return getModel().createStatement(this, predicate(getActualClass()), getValue());//.asRootStatement();
    }

    @Override
    public Literal getValue() {
        return getRequiredObject(predicate(getActualClass()), Literal.class);
    }

    private static OntMaker makeMaker(Class<? extends OntFRImpl> impl) {
        return new OntMaker.Default(impl);
    }

    private static OntFinder makeFinder(Property predicate) {
        return new OntFinder.ByPredicate(predicate);
    }

    private static OntFilter makeFilter(Property predicate) {
        return OntFilter.BLANK.and((n, g) -> !g.asGraph().find(n, predicate.asNode(), Node.ANY).mapWith(Triple::getObject).filterKeep(Node::isLiteral).toList().isEmpty());
    }

    private static Property predicate(Class<?> view) {
        if (Length.class.equals(view)) return XSD.length;
        if (MinLength.class.equals(view)) return XSD.minLength;
        if (MaxLength.class.equals(view)) return XSD.maxLength;
        if (MinInclusive.class.equals(view)) return XSD.minInclusive;
        if (MaxInclusive.class.equals(view)) return XSD.maxInclusive;
        if (MinExclusive.class.equals(view)) return XSD.minExclusive;
        if (MaxExclusive.class.equals(view)) return XSD.maxExclusive;
        if (TotalDigits.class.equals(view)) return XSD.totalDigits;
        if (FractionDigits.class.equals(view)) return XSD.fractionDigits;
        if (Pattern.class.equals(view)) return XSD.pattern;
        if (LangRange.class.equals(view)) return RDF.langRange;
        throw new OntJenaException("Unsupported facet restriction " + view);
    }

    public static <T extends OntFR> T create(OntGraphModelImpl model, Class<T> view, Literal literal) {
        Resource res = model.createResource();
        res.addProperty(predicate(view), literal);
        return model.getNodeAs(res.asNode(), view);
    }

    public static class LengthImpl extends OntFRImpl implements Length {
        public LengthImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<Length> getActualClass() {
            return Length.class;
        }
    }

    public static class MinLengthImpl extends OntFRImpl implements MinLength {
        public MinLengthImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<MinLength> getActualClass() {
            return MinLength.class;
        }
    }

    public static class MaxLengthImpl extends OntFRImpl implements MaxLength {
        public MaxLengthImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<MaxLength> getActualClass() {
            return MaxLength.class;
        }
    }

    public static class MinInclusiveImpl extends OntFRImpl implements MinInclusive {
        public MinInclusiveImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<MinInclusive> getActualClass() {
            return MinInclusive.class;
        }
    }

    public static class MaxInclusiveImpl extends OntFRImpl implements MaxInclusive {
        public MaxInclusiveImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<MaxInclusive> getActualClass() {
            return MaxInclusive.class;
        }
    }

    public static class MinExclusiveImpl extends OntFRImpl implements MinExclusive {
        public MinExclusiveImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<MinExclusive> getActualClass() {
            return MinExclusive.class;
        }
    }

    public static class MaxExclusiveImpl extends OntFRImpl implements MaxExclusive {
        public MaxExclusiveImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<MaxExclusive> getActualClass() {
            return MaxExclusive.class;
        }
    }

    public static class PatternImpl extends OntFRImpl implements Pattern {
        public PatternImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<Pattern> getActualClass() {
            return Pattern.class;
        }
    }

    public static class TotalDigitsImpl extends OntFRImpl implements TotalDigits {
        public TotalDigitsImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<TotalDigits> getActualClass() {
            return TotalDigits.class;
        }
    }

    public static class FractionDigitsImpl extends OntFRImpl implements FractionDigits {
        public FractionDigitsImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<FractionDigits> getActualClass() {
            return FractionDigits.class;
        }
    }

    public static class LangRangeImpl extends OntFRImpl implements LangRange {
        public LangRangeImpl(Node n, EnhGraph m) {
            super(n, m);
        }

        @Override
        public Class<LangRange> getActualClass() {
            return LangRange.class;
        }
    }
}
