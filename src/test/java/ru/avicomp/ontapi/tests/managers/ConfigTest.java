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
import ru.avicomp.ontapi.OntManagers;
import ru.avicomp.ontapi.OntologyManager;
import ru.avicomp.ontapi.config.OntConfig;
import ru.avicomp.ontapi.config.OntSettings;

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
        int def = Integer.parseInt(Prop.ONT_API_CONF_MANAGER_CACHE_IRI.get());
        OntConfig c1 = m.getOntologyConfigurator();
        Assert.assertNotNull(c1);
        Assert.assertEquals(def, c1.getManagerIRICacheSize());
        Assert.assertEquals(def, m.getOntologyConfigurator().getManagerIRICacheSize());

        OntConfig c2 = OntConfig.createConfig(null, 1);
        Assert.assertEquals(1, c2.getManagerIRICacheSize());
        m.setOntologyConfigurator(c2);
        Assert.assertEquals(1, m.getOntologyConfigurator().getManagerIRICacheSize());
    }

    enum Prop {
        ONT_API_CONF_MANAGER_CACHE_IRI("ont.api.conf.manager.cache.iri.integer"),
        ;
        private final String key;

        Prop(String key) {
            this.key = key;
        }

        public String get() {
            return OntSettings.PROPERTIES.getProperty(key);
        }
    }
}
