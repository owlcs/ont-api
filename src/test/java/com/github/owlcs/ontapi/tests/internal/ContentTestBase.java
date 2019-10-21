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

package com.github.owlcs.ontapi.tests.internal;

import org.junit.Assert;
import org.semanticweb.owlapi.model.OWLObject;
import com.github.owlcs.ontapi.internal.InternalCache;
import com.github.owlcs.ontapi.internal.objects.WithContent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by @ssz on 15.09.2019.
 *
 * @see WithContent
 */
abstract class ContentTestBase extends ObjectFactoryTestBase {

    ContentTestBase(Data data) {
        super(data);
    }

    void testContent(OWLObject sample, OWLObject test) {
        LOGGER.debug("Test content {}", data);
        testInternalCacheReset(sample, test);
    }

    private void testInternalCacheReset(OWLObject sample, OWLObject instance) {
        Assert.assertTrue(instance instanceof WithContent);
        WithContent wc = (WithContent) instance;
        InternalCache.Loading cache = getContentCache(instance);
        Assert.assertFalse(cache.isEmpty());
        Assert.assertTrue(wc.hasContent());
        wc.clearContent();
        Assert.assertTrue(cache.isEmpty());
        Assert.assertFalse(wc.hasContent());
        testCompare(sample, instance);
        Assert.assertTrue(wc.hasContent());
        testComponents(sample, instance);
    }

    private static InternalCache.Loading getContentCache(OWLObject inst) {
        return findDeclaredField(inst, "content");
    }

    @SuppressWarnings({"unchecked", "SameParameterValue"})
    private static <X> X findDeclaredField(Object inst, String name) {
        AssertionError error = new AssertionError("Can't find field '" + name + "'");
        Class type = inst.getClass();
        Set<Class> seen = new HashSet<>();
        while (type != null) {
            try {
                Field[] fields = type.getDeclaredFields();
                Field res = fields.length == 0 ? null :
                        Arrays.stream(fields).filter(x -> name.equals(x.getName())).findFirst().orElse(null);
                if (res != null) {
                    res.setAccessible(true);
                    return (X) res.get(inst);
                }
            } catch (Exception e) {
                error.addSuppressed(error);
            }
            type = type.getSuperclass();
            if (!seen.add(type)) {
                break;
            }
        }
        throw error;
    }
}
