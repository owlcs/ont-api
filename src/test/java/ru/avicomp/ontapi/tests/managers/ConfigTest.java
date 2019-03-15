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

package ru.avicomp.ontapi.tests.managers;

import org.junit.Assert;
import org.junit.Test;
import ru.avicomp.ontapi.OntFormat;
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.OntologyModel;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntLoaderConfiguration;
import ru.avicomp.ontapi.config.OntSettings;
import ru.avicomp.ontapi.internal.*;
import ru.avicomp.ontapi.jena.impl.OntGraphModelImpl;
import ru.avicomp.ontapi.utils.ReadWriteUtils;

import java.util.Objects;

/**
 * Created by @ssz on 04.03.2019.
 */
public class ConfigTest {

    @Test
    public void testConfigureManagerIRICacheSize() {
        testConfigureIRICacheSize(OntManagers.createONT());
        testConfigureIRICacheSize(OntManagers.createConcurrentONT());
    }

    private static void testConfigureIRICacheSize(OntologyManager m) {
        int def = Prop.IRI_CACHE_SIZE.getInt();
        OntConfig c1 = m.getOntologyConfigurator();
        Assert.assertNotNull(c1);
        Assert.assertEquals(def, c1.getManagerIRIsCacheSize());
        Assert.assertEquals(def, m.getOntologyConfigurator().getManagerIRIsCacheSize());

        OntConfig c2 = OntConfig.createConfig(null, 1);
        Assert.assertEquals(1, c2.getManagerIRIsCacheSize());
        m.setOntologyConfigurator(c2);
        Assert.assertEquals(1, m.getOntologyConfigurator().getManagerIRIsCacheSize());
    }

    @Test
    public void testNodesCacheSize() throws Exception {
        Assert.assertEquals(Prop.NODES_CACHE_SIZE.getInt(), new OntConfig().getLoadNodesCacheSize());
        OntologyManager m = OntManagers.createONT();
        Assert.assertNotNull(m.getOntologyConfigurator().setLoadNodesCacheSize(-123));
        Assert.assertEquals(-123, m.getOntologyLoaderConfiguration().getLoadNodesCacheSize());
        // cache is disabled, try to load model
        OntologyModel o = m.loadOntologyFromOntologyDocument(ReadWriteUtils.getDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assert.assertNotNull(o);
        Assert.assertEquals(945, o.axioms().count());

        OntGraphModelImpl m1 = ((InternalModelHolder) o).getBase().getSearchModel();
        Assert.assertTrue(m1 instanceof InternalModel);

        m.setOntologyLoaderConfiguration(m.getOntologyLoaderConfiguration().setLoadNodesCacheSize(10_000));
        OntGraphModelImpl m2 = ((InternalModelHolder) o).getBase().getSearchModel();
        Assert.assertTrue(m2 instanceof SearchModel);
    }

    @Test
    public void testObjectsCacheSize() throws Exception {
        OntologyManager m = OntManagers.createONT();
        Assert.assertEquals(Prop.OBJECTS_CACHE_SIZE.getInt(), m.getOntologyConfigurator().getLoadObjectsCacheSize());
        OntLoaderConfiguration conf = new OntConfig().buildLoaderConfiguration().setLoadObjectsCacheSize(-1);
        Assert.assertEquals(-1, conf.getLoadObjectsCacheSize());
        m.setOntologyLoaderConfiguration(conf);
        OntologyModel o = m.loadOntologyFromOntologyDocument(ReadWriteUtils.getDocumentSource("/ontapi/pizza.ttl",
                OntFormat.TURTLE));
        Assert.assertNotNull(o);
        Assert.assertEquals(945, o.axioms().count());
        InternalObjectFactory of1 = ((InternalModelHolder) o).getBase().getObjectFactory();
        Assert.assertTrue(of1 instanceof NoCacheObjectFactory);
        Assert.assertFalse(of1 instanceof CacheObjectFactory);

        m.setOntologyLoaderConfiguration(conf.setLoadObjectsCacheSize(10_000));
        Assert.assertEquals(945, o.axioms().count());
        InternalObjectFactory of2 = ((InternalModelHolder) o).getBase().getObjectFactory();
        Assert.assertTrue(of2 instanceof CacheObjectFactory);
    }

    enum Prop {
        IRI_CACHE_SIZE(OntSettings.ONT_API_MANAGER_CACHE_IRIS.key() + ".integer"),
        NODES_CACHE_SIZE(OntSettings.ONT_API_LOAD_CONF_CACHE_NODES.key() + ".integer"),
        OBJECTS_CACHE_SIZE(OntSettings.ONT_API_LOAD_CONF_CACHE_OBJECTS.key() + ".integer"),
        ;
        private final String key;

        Prop(String key) {
            this.key = key;
        }

        public int getInt() {
            return Integer.parseInt(get());
        }

        public String get() {
            return Objects.requireNonNull(OntSettings.PROPERTIES.getProperty(key), "Null " + key);
        }

    }
}
