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

package com.github.owlcs.ontapi.tests.managers;

import com.github.owlcs.ontapi.OntApiException;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.OntologyManager;
import com.github.owlcs.ontapi.config.OntConfig;
import com.github.owlcs.ontapi.config.OntSettings;
import com.github.owlcs.ontapi.jena.impl.conf.OntModelConfig;
import com.github.owlcs.ontapi.jena.vocabulary.OWL;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OntologyConfigurator;

import java.util.List;

/**
 * Created by @ssz on 21.03.2019.
 */
public class CommonConfigTest {

    @Test
    public void testShareConfigurator() {
        OntConfig c = new OntConfig();
        OntologyManager m1 = OntManagers.createManager();
        OntologyManager m2 = OntManagers.createConcurrentManager();
        m1.setOntologyConfigurator(c);
        m2.setOntologyConfigurator(c);
        assertEqualsConfigurator(c, m1);
        assertEqualsConfigurator(c, m2);

        c.setPersonality(OntModelConfig.ONT_PERSONALITY_STRICT).setFollowRedirects(false);
        assertEqualsConfigurator(c, m1);
        assertEqualsConfigurator(c, m2);

        c.setPersonality(OntModelConfig.ONT_PERSONALITY_LAX).setIgnoreAxiomsReadErrors(true);
        assertEqualsConfigurator(c, m1);
        assertEqualsConfigurator(c, m2);
    }

    private static void assertEqualsConfigurator(OntologyConfigurator conf, OWLOntologyManager m) {
        Assertions.assertEquals(m.getOntologyConfigurator(), conf);
    }

    @Test
    public void testLoaderConfigurationInManager() {
        testGeneralConfigOverrideBehaviour(OntManagers.createOWLAPIImplManager());
        testGeneralConfigOverrideBehaviour(OntManagers.createManager());
        testGeneralConfigOverrideBehaviour(OntManagers.createConcurrentManager());
    }

    private static void testGeneralConfigOverrideBehaviour(OWLOntologyManager m) {
        Assertions.assertTrue(m.getOntologyLoaderConfiguration().isAcceptingHTTPCompression());
        m.getOntologyConfigurator().setAcceptingHTTPCompression(false);
        Assertions.assertFalse(m.getOntologyConfigurator().shouldAcceptHTTPCompression());
        Assertions.assertFalse(m.getOntologyLoaderConfiguration().isAcceptingHTTPCompression());

        m.setOntologyLoaderConfiguration(new OWLOntologyLoaderConfiguration().setAcceptingHTTPCompression(true));
        Assertions.assertFalse(m.getOntologyConfigurator().shouldAcceptHTTPCompression());
        Assertions.assertTrue(m.getOntologyLoaderConfiguration().isAcceptingHTTPCompression());
    }

    @Test
    public void testIgnoredImports() {
        List<String> imports = new OntConfig() {
            @Override
            protected List<String> getIgnoredImports() {
                return super.getIgnoredImports();
            }
        }.getIgnoredImports();
        Assertions.assertEquals(7, imports.size());

        Assertions.assertTrue(new OntConfig().buildLoaderConfiguration().isIgnoredImport(IRI.create(OWL.NS)));
    }

    @Test
    public void testLockProperty() {
        OntConfig conf = new OntConfig();
        Assertions.assertTrue(conf.isAllowBulkAnnotationAssertions());
        Assertions.assertTrue(conf.buildLoaderConfiguration().isAllowBulkAnnotationAssertions());
        Assertions.assertFalse(conf.setAllowBulkAnnotationAssertions(false).isAllowBulkAnnotationAssertions());
        Assertions.assertFalse(conf.lockProperty(OntSettings.ONT_API_LOAD_CONF_ALLOW_BULK_ANNOTATION_ASSERTIONS).isAllowBulkAnnotationAssertions());
        Assertions.assertThrows(OntApiException.ModificationDenied.class, () -> conf.setAllowBulkAnnotationAssertions(true));
        Assertions.assertFalse(conf.isAllowBulkAnnotationAssertions());
        Assertions.assertFalse(conf.buildLoaderConfiguration().isAllowBulkAnnotationAssertions());
    }
}
