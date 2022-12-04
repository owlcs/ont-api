/*
 * This file is part of the ONT API.
 * The contents of this file are subject to the LGPL License, Version 3.0.
 * Copyright (c) 2022, owl.cs group.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0 in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.github.owlcs.ontapi.jena.vocabulary;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Vocabulary defining builtin SWRL entities, that have implicit type {@link SWRL#Builtin swrl:Builtin}.
 * Created by @szz on 12.03.2019.
 * <p>
 * See also OWLAPI-api {@code org.semanticweb.owlapi.vocab.SWRLVocabulary}
 *
 * @see <a href="https://www.w3.org/Submission/SWRL/#8">8. Built-Ins</a>
 * @see SWRL
 */
@SuppressWarnings("unused")
public class SWRLB {
    public final static String URI = "http://www.w3.org/2003/11/swrlb";
    public final static String NS = URI + "#";

    public static final Property equal = property("equal");
    public static final Property notEqual = property("notEqual");
    public static final Property lessThan = property("lessThan");
    public static final Property lessThanOrEqual = property("lessThanOrEqual");
    public static final Property greaterThan = property("greaterThan");
    public static final Property greaterThanOrEqual = property("greaterThanOrEqual");
    public static final Property add = property("add");
    public static final Property subtract = property("subtract");
    public static final Property multiply = property("multiply");
    public static final Property divide = property("divide");
    public static final Property integerDivide = property("integerDivide");
    public static final Property mod = property("mod");
    public static final Property pow = property("pow");
    public static final Property unaryMinus = property("unaryMinus");
    public static final Property unaryPlus = property("unaryPlus");
    public static final Property abs = property("abs");
    public static final Property ceiling = property("ceiling");
    public static final Property floor = property("floor");
    public static final Property round = property("round");
    public static final Property roundHalfToEven = property("roundHalfToEven");
    public static final Property sin = property("sin");
    public static final Property cos = property("cos");
    public static final Property tan = property("tan");
    public static final Property booleanNot = property("booleanNot");
    public static final Property stringEqualIgnoreCase = property("stringEqualIgnoreCase");
    public static final Property stringConcat = property("stringConcat");
    public static final Property substring = property("substring");
    public static final Property stringLength = property("stringLength");
    public static final Property normalizeSpace = property("normalizeSpace");
    public static final Property upperCase = property("upperCase");
    public static final Property lowerCase = property("lowerCase");
    public static final Property translate = property("translate");
    public static final Property contains = property("contains");
    public static final Property containsIgnoreCase = property("containsIgnoreCase");
    public static final Property startsWith = property("startsWith");
    public static final Property endsWith = property("endsWith");
    public static final Property substringBefore = property("substringBefore");
    public static final Property substringAfter = property("substringAfter");
    public static final Property matchesLax = property("matchesLax");
    public static final Property replace = property("replace");
    public static final Property tokenize = property("tokenize");
    public static final Property yearMonthDuration = property("yearMonthDuration");
    public static final Property dayTimeDuration = property("dayTimeDuration");
    public static final Property dateTime = property("dateTime");
    public static final Property date = property("date");
    public static final Property time = property("time");
    public static final Property subtractDates = property("subtractDates");
    public static final Property subtractTimes = property("subtractTimes");
    public static final Property resolveURI = property("resolveURI");
    public static final Property anyURI = property("anyURI");
    public static final Property addYearMonthDurations = property("addYearMonthDurations");
    public static final Property subtractYearMonthDurations = property("subtractYearMonthDurations");
    public static final Property multiplyYearMonthDurations = property("multiplyYearMonthDurations");
    public static final Property divideYearMonthDurations = property("divideYearMonthDurations");
    public static final Property addDayTimeDurations = property("addDayTimeDurations");
    public static final Property subtractDayTimeDurations = property("subtractDayTimeDurations");
    public static final Property multiplyDayTimeDurations = property("multiplyDayTimeDurations");
    public static final Property divideDayTimeDurations = property("divideDayTimeDurations");
    public static final Property addDayTimeDurationToDateTime = property("addDayTimeDurationToDateTime");
    public static final Property subtractYearMonthDurationFromDateTime = property("subtractYearMonthDurationFromDateTime");
    public static final Property subtractDayTimeDurationFromDateTime = property("subtractDayTimeDurationFromDateTime");
    public static final Property addYearMonthDurationToDate = property("addYearMonthDurationToDate");
    public static final Property addDayTimeDurationToDate = property("addDayTimeDurationToDate");
    public static final Property subtractYearMonthDurationFromDate = property("subtractYearMonthDurationFromDate");
    public static final Property subtractDayTimeDurationFromDate = property("subtractDayTimeDurationFromDate");
    public static final Property addDayTimeDurationToTime = property("addDayTimeDurationToTime");
    public static final Property subtractDayTimeDurationFromTime = property("subtractDayTimeDurationFromTime");
    public static final Property subtractDateTimesYieldingYearMonthDuration = property("subtractDateTimesYieldingYearMonthDuration");
    public static final Property subtractDateTimesYieldingDayTimeDuration = property("subtractDateTimesYieldingDayTimeDuration");

    protected static Property property(String local) {
        return ResourceFactory.createProperty(NS + local);
    }
}
