/*
 * Copyright (c) 2010 - 2015 Norwegian Agency for Pupblic Government and eGovernment (Difi)
 *
 * This file is part of Oxalis.
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission
 * - subsequent versions of the EUPL (the "Licence"); You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence
 *  is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */
package eu.peppol.outbound;

import com.google.inject.Inject;
import eu.peppol.identifier.WellKnownParticipant;
import eu.peppol.lang.OxalisException;
import eu.peppol.lang.OxalisTransmissionException;
import eu.peppol.outbound.transmission.TransmissionRequestBuilder;
import eu.peppol.util.GlobalConfiguration;
import eu.peppol.util.OxalisKeystoreModule;
import eu.peppol.util.OxalisProductionConfigurationModule;
import no.difi.oxalis.api.outbound.TransmissionRequest;
import no.difi.oxalis.api.outbound.TransmissionResponse;
import no.difi.oxalis.api.outbound.Transmitter;
import no.difi.vefa.peppol.common.model.TransportProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.text.SimpleDateFormat;

import static org.testng.Assert.*;

/**
 * @author steinar
 * @author thore
 */
@Guice(modules = {OxalisKeystoreModule.class, OxalisProductionConfigurationModule.class})
public class SendSampleInvoiceTestIT {

    public static final String PEPPOL_BIS_INVOICE_SBDH_XML = "peppol-bis-invoice-sbdh.xml";
    public static final String EHF_WITH_SBDH = "BII04_T10_EHF-v1.5_invoice_with_sbdh.xml";

    OxalisOutboundComponent oxalisOutboundComponent;
    TransmissionRequestBuilder builder;

    public static final Logger log = LoggerFactory.getLogger(SendSampleInvoiceTestIT.class);

    @Inject
    GlobalConfiguration globalConfiguration;

    @BeforeMethod
    public void setUp() {
        globalConfiguration.setTransmissionBuilderOverride(true);
        oxalisOutboundComponent = new OxalisOutboundComponent();
        builder = oxalisOutboundComponent.getTransmissionRequestBuilder();
    }

    /**
     * This test was written to recreate the SSL problems experienced by ESPAP in order to supply a more informative
     * exception.
     *
     * @throws MalformedURLException
     * @throws OxalisTransmissionException
     */
    @Test(groups = {"manual"})
    public void sendToEspapWithSSLProblems() throws MalformedURLException, OxalisException {
        InputStream is = SendSampleInvoiceTestIT.class.getClassLoader().getResourceAsStream(PEPPOL_BIS_INVOICE_SBDH_XML);
        assertNotNull(is, "Unable to locate peppol-bis-invoice-sbdh.sml in class path");

        assertNotNull(oxalisOutboundComponent);
        assertNotNull(builder);

        // Build the payload
        builder.payLoad(is);

        // Overrides the end point address, thus preventing a SMP lookup
        builder.overrideAs2Endpoint(URI.create("https://ap1.espap.pt/oxalis/as2"), "peppol-APP_1000000222");

        // Builds our transmission request
        TransmissionRequest transmissionRequest = builder.build();

        // Gets a transmitter, which will be used to execute our transmission request
        Transmitter transmitter = oxalisOutboundComponent.getTransmitter();

        // Transmits our transmission request
        TransmissionResponse transmissionResponse = transmitter.transmit(transmissionRequest);

    }

    @Test
    public void sendSingleInvoiceToLocalEndPointUsingAS2() throws Exception {

        InputStream is = SendSampleInvoiceTestIT.class.getClassLoader().getResourceAsStream(PEPPOL_BIS_INVOICE_SBDH_XML);
        assertNotNull(is, "Unable to locate peppol-bis-invoice-sbdh.sml in class path");

        assertNotNull(oxalisOutboundComponent);
        assertNotNull(builder);

        // Build the payload
        builder.payLoad(is);

        // Overrides the end point address, thus preventing a SMP lookup
        builder.overrideAs2Endpoint(URI.create(IntegrationTestConstant.OXALIS_AS2_URL), "peppol-APP_1000000006");

        // Builds our transmission request
        TransmissionRequest transmissionRequest = builder.build();

        // Gets a transmitter, which will be used to execute our transmission request
        Transmitter transmitter = oxalisOutboundComponent.getTransmitter();

        // Transmits our transmission request
        TransmissionResponse transmissionResponse = transmitter.transmit(transmissionRequest);
        assertNotNull(transmissionResponse);
        assertNotNull(transmissionResponse.getMessageId());
        assertNotNull(transmissionResponse.getHeader());
        assertEquals(transmissionResponse.getHeader().getReceiver().getIdentifier(), WellKnownParticipant.DIFI_TEST.stringValue());
        assertEquals(transmissionResponse.getEndpoint().getAddress().toString(), IntegrationTestConstant.OXALIS_AS2_URL);
        assertEquals(transmissionResponse.getProtocol(), TransportProfile.AS2_1_0);
        assertTrue(transmissionResponse.getEndpoint().getCertificate().getSubjectX500Principal().getName().contains("peppol-APP_1000000006"));
    }


    /**
     * Verify that we can deliver AS2 message with pre-wrapped SBDH.
     */
    @Test()
    public void sendSingleInvoiceWithSbdhToLocalEndPointUsingAS2() throws Exception {

        InputStream is = SendSampleInvoiceTestIT.class.getClassLoader().getResourceAsStream(EHF_WITH_SBDH);
        assertNotNull(is, "Unable to locate peppol-bis-invoice-sbdh.sml in class path");

        assertNotNull(oxalisOutboundComponent);
        assertNotNull(builder);

        // Build the payload
        builder.payLoad(is);

        // Overrides the end point address, thus preventing a SMP lookup
        builder.overrideAs2Endpoint(URI.create(IntegrationTestConstant.OXALIS_AS2_URL), "peppol-APP_1000000006");

        // Builds our transmission request
        TransmissionRequest transmissionRequest = builder.build();

        // Gets a transmitter, which will be used to execute our transmission request
        Transmitter transmitter = oxalisOutboundComponent.getTransmitter();

        // Transmits our transmission request
        TransmissionResponse transmissionResponse = transmitter.transmit(transmissionRequest);
        assertNotNull(transmissionResponse);
        assertNotNull(transmissionResponse.getMessageId());
        assertNotNull(transmissionResponse.getStandardBusinessHeader());
        assertEquals(transmissionResponse.getStandardBusinessHeader().getRecipientId().stringValue(), WellKnownParticipant.DIFI_TEST.stringValue());
        assertEquals(transmissionResponse.getEndpoint().getAddress(), IntegrationTestConstant.OXALIS_AS2_URL);
        assertEquals(transmissionResponse.getProtocol(), TransportProfile.AS2_1_0);
        assertTrue(transmissionResponse.getEndpoint().getCertificate().getSubjectX500Principal().getName().contains("peppol-APP_1000000006"));

        assertNotEquals(transmissionResponse.getStandardBusinessHeader().getInstanceId(), transmissionResponse.getMessageId().stringValue());

        // Make sure we got the correct CreationDateAndTime from the SBDH : "2014-11-01T16:32:48.128+01:00"
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        assertEquals(sdf.format(transmissionResponse.getStandardBusinessHeader().getCreationDateAndTime()), "2014-11-01 16:32:48");
    }


    /**
     * Verifies that we can run several transmission tasks in parallell.
     *
     * @throws Exception
     */
    @Test
    public void sendWithMultipleThreads() throws Exception {

        final int MAX_THREADS = 5;

        Thread[] threads = new Thread[MAX_THREADS];
        SenderTask[] senderTasks = new SenderTask[MAX_THREADS];

        for (int i = 0; i < MAX_THREADS; i++) {
            senderTasks[i] = new SenderTask(i);
            threads[i] = new Thread(senderTasks[i], "Thread " + i);
            threads[i].start();
        }

        Thread.sleep(20 * 1000); // Wait for 10 seconds to allow worker threads to complete

        for (int i = 0; i < MAX_THREADS; i++) {
            boolean alive = threads[i].isAlive();
            threads[i].isInterrupted();
            threads[i].join(1000); // Allows transmissions to complete before we exit

            boolean actual = senderTasks[i].hasCompletedTransmission();

            assertTrue(actual, "SenderTask " + i + " has not completed");
        }

        long accumulatedElapsedTime = 0;
        for (int i = 0; i < MAX_THREADS; i++) {
            accumulatedElapsedTime += senderTasks[i].getElapsedTime();
        }

        long averageTime = accumulatedElapsedTime / MAX_THREADS;
        log.debug("Average transmission time " + averageTime + "ms");
        assertTrue(averageTime < 8000, "Average transmission time was " + averageTime + " should be less than 2000ms. Do you have a slow machine?");
    }


    /**
     * Class suitable for running several transmission threads in paralell.
     */
    static class SenderTask implements Runnable {

        private final int threadNumber;
        private boolean transmissionCompleted = false;
        private long elapsedTime = 0;

        public SenderTask(int threadNumber) {
            this.threadNumber = threadNumber;
        }

        public long getElapsedTime() {
            return elapsedTime;
        }

        public boolean hasCompletedTransmission() {
            return transmissionCompleted;

        }


        @Override
        public void run() {
            try {

                log.debug(threadNumber + " fetching resourcestream");

                InputStream is = SendSampleInvoiceTestIT.class.getClassLoader().getResourceAsStream(EHF_WITH_SBDH);
                assertNotNull(is, "Unable to locate peppol-bis-invoice-sbdh.sml in class path");

                OxalisOutboundComponent oxalisOutboundComponent = new OxalisOutboundComponent();

                TransmissionRequestBuilder builder = oxalisOutboundComponent.getTransmissionRequestBuilder();
                assertNotNull(builder);

                log.debug(threadNumber + " loading inputdata..");
                // Build the payload
                builder.payLoad(is);

                // Overrides the end point address, thus preventing a SMP lookup
                builder.overrideAs2Endpoint(URI.create(IntegrationTestConstant.OXALIS_AS2_URL), "peppol-APP_1000000006");

                log.debug(threadNumber + " building transmission request...");
                // Builds our transmission request
                TransmissionRequest transmissionRequest = builder.build();

                log.debug(threadNumber + " retrieving a transmitter....");
                // Gets a transmitter, which will be used to execute our transmission request
                Transmitter transmitter = oxalisOutboundComponent.getTransmitter();

                log.debug(threadNumber + " performing transmission ...");
                long transmissionStart = System.currentTimeMillis();
                // Transmits our transmission request
                TransmissionResponse transmissionResponse = null;
                try {
                    transmissionResponse = transmitter.transmit(transmissionRequest);
                } catch (OxalisTransmissionException e) {
                    throw new IllegalStateException(e);
                }
                long transmissionFinished = System.currentTimeMillis();

                // Calculates the elapsed time
                elapsedTime = transmissionFinished - transmissionStart;
                // Report that transmission was completed OK
                transmissionCompleted = true;

                assertNotNull(transmissionResponse);
                assertNotNull(transmissionResponse.getMessageId());
                assertNotNull(transmissionResponse.getStandardBusinessHeader());
                assertEquals(transmissionResponse.getStandardBusinessHeader().getRecipientId().stringValue(), WellKnownParticipant.DIFI_TEST.stringValue());
                assertEquals(transmissionResponse.getEndpoint().getAddress().toString(), IntegrationTestConstant.OXALIS_AS2_URL);
                assertEquals(transmissionResponse.getProtocol(), TransportProfile.AS2_1_0);
                assertTrue(transmissionResponse.getEndpoint().getCertificate().getSubjectX500Principal().getName().contains("peppol-APP_1000000006"));

                assertNotEquals(transmissionResponse.getStandardBusinessHeader().getInstanceId(), transmissionResponse.getMessageId().stringValue());

                // Make sure we got the correct CreationDateAndTime from the SBDH : "2014-11-01T16:32:48.128+01:00"
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                assertEquals(sdf.format(transmissionResponse.getStandardBusinessHeader().getCreationDateAndTime()), "2014-11-01 16:32:48");
                log.debug(threadNumber + " transmission complete...");

            } catch (OxalisException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }

    }
}

