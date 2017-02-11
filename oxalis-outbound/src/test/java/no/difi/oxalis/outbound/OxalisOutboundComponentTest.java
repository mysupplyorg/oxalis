/*
 * Copyright 2010-2017 Norwegian Agency for Public Management and eGovernment (Difi)
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.oxalis.outbound;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author steinar
 *         Date: 18.11.2016
 *         Time: 16.32
 */
public class OxalisOutboundComponentTest {

    @Test
    public void testGetters() throws Exception {
        OxalisOutboundComponent oxalisOutboundComponent = new OxalisOutboundComponent();

        Assert.assertNotNull(oxalisOutboundComponent.getTransmissionRequestBuilder());
        Assert.assertNotNull(oxalisOutboundComponent.getTransmissionRequestFactory());
        Assert.assertNotNull(oxalisOutboundComponent.getLookupService());
        Assert.assertNotNull(oxalisOutboundComponent.getTransmitter());
        Assert.assertNotNull(oxalisOutboundComponent.getGlobalConfiguration());
        Assert.assertNotNull(oxalisOutboundComponent.getEvidenceFactory());
        Assert.assertNotNull(oxalisOutboundComponent.getInjector());
        Assert.assertNotNull(oxalisOutboundComponent.getTransmissionService());
    }
}
