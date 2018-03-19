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

package ru.avicomp.ontapi.benchmarks;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.junit.Assert;
import ru.avicomp.owlapi.OWLManager;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An utility class to clear OWL-API impl caches.
 * This is necessary for more purity of experiments.
 * <p>
 * Created by @szuev on 10.03.2018.
 */
@SuppressWarnings("WeakerAccess")
public class OWLAPICaches {

    public static void clearDataFactoryCaches() {
        List<LoadingCache> caches = getGlobalCaches(OWLManager.findClass("uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryInternalsImpl"));
        Assert.assertEquals(7, caches.size());
        caches.forEach(Cache::invalidateAll);
    }

    public static void clearObjectCaches() {
        List<LoadingCache> caches = getGlobalCaches(OWLManager.findClass("uk.ac.manchester.cs.owl.owlapi.OWLObjectImpl"));
        Assert.assertEquals(8, caches.size());
        caches.forEach(Cache::invalidateAll);
    }

    public static void clearAll() {
        clearDataFactoryCaches();
        clearObjectCaches();
    }

    private static List<LoadingCache> getGlobalCaches(Class<?> clazz) {
        List<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()))
                .filter(f -> LoadingCache.class.isAssignableFrom(f.getType()))
                .peek(f -> f.setAccessible(true))
                .collect(Collectors.toList());
        List<LoadingCache> res = new ArrayList<>();
        for (Field f : fields) {
            try {
                res.add((LoadingCache) f.get(null));
            } catch (IllegalAccessException | ClassCastException e) {
                throw new AssertionError("Can't get " + f, e);
            }
        }
        return res;
    }
}
