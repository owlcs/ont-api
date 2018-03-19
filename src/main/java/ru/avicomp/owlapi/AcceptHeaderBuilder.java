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

package ru.avicomp.owlapi;

import org.semanticweb.owlapi.io.OWLParserFactory;
import org.semanticweb.owlapi.util.PriorityCollection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * @see <a href='https://github.com/owlcs/owlapi/blob/version5/impl/src/main/java/uk/ac/manchester/cs/AcceptHeaderBuilder.java'>uk.ac.manchester.cs.AcceptHeaderBuilder</a>
 */
public class AcceptHeaderBuilder {
    public static String headersFromParsers(PriorityCollection<OWLParserFactory> parsers) {
        Map<String, TreeSet<Integer>> map = new HashMap<>();
        parsers.forEach(p -> addToMap(map, p.getMIMETypes()));
        return map.entrySet().stream().sorted(AcceptHeaderBuilder::compare)
                .map(AcceptHeaderBuilder::tostring).collect(Collectors.joining(", "));
    }

    private static void addToMap(Map<String, TreeSet<Integer>> map, List<String> mimes) {
        // The map will contain all mime types with their position in all lists mentioning them; the
        // smallest position first
        for (int i = 0; i < mimes.size(); i++) {
            map.computeIfAbsent(mimes.get(i), k -> new TreeSet<>()).add(i + 1);
        }
    }

    private static String tostring(Map.Entry<String, TreeSet<Integer>> e) {
        return String.format("%s; q=%.1f", e.getKey(), 1D / e.getValue().first());
    }

    private static int compare(Map.Entry<String, TreeSet<Integer>> a, Map.Entry<String, TreeSet<Integer>> b) {
        return a.getValue().first().compareTo(b.getValue().first());
    }
}
