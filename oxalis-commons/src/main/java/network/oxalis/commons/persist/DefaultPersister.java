/*
 * Copyright 2010-2018 Norwegian Agency for Public Management and eGovernment (Difi)
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

package network.oxalis.commons.persist;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import network.oxalis.api.evidence.EvidenceFactory;
import network.oxalis.api.inbound.InboundMetadata;
import network.oxalis.api.lang.EvidenceException;
import network.oxalis.api.model.TransmissionIdentifier;
import network.oxalis.api.persist.PersisterHandler;
import network.oxalis.api.util.Type;
// import no.difi.oxalis.as4.inbound.As4InboundMetadata;
// import no.difi.oxalis.as4.inbound.As4PayloadHeader;
import network.oxalis.commons.filesystem.FileUtils;
import network.oxalis.vefa.peppol.common.model.Header;

import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * @author erlend
 * @since 4.0.0
 */
@Slf4j
@Singleton
@Type("default")
public class DefaultPersister implements PersisterHandler {

    private final EvidenceFactory evidenceFactory;

    private final Path inboundFolder;

    @Inject
    public DefaultPersister(@Named("inbound") Path inboundFolder, EvidenceFactory evidenceFactory) {
        this.inboundFolder = inboundFolder;
        this.evidenceFactory = evidenceFactory;
    }

    @Override
    public Path persist(TransmissionIdentifier transmissionIdentifier, Header header, InputStream inputStream, String serverName)
            throws IOException {
        Path folder = inboundFolder;
        /*
        if (header instanceof As4PayloadHeader) {
            As4PayloadHeader as4Header = (As4PayloadHeader)header;
            folder = Paths.get(inboundFolder.toString(), as4Header.getServer());
        } */

        folder = Paths.get(inboundFolder.toString(), serverName);

        Path path = PersisterUtils.createArtifactFolders(folder, header).resolve(
                String.format("%s.doc.xml", FileUtils.filterString(transmissionIdentifier.getIdentifier())));

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            ByteStreams.copy(inputStream, outputStream);
        }

        log.debug("Payload persisted to: {}", path);

        return path;
    }

    @Override
    public void persist(InboundMetadata inboundMetadata, Path payloadPath) throws IOException {
        Path folder = inboundFolder;

        String serverName = inboundMetadata.getServer();
        if(  serverName != null && serverName.length() > 0) {
            folder = Paths.get(inboundFolder.toString(), serverName);
        }
        /*
        if (inboundMetadata instanceof As4InboundMetadata) {
            As4InboundMetadata as4MetaData = (As4InboundMetadata)inboundMetadata;
            folder = Paths.get(inboundFolder.toString(), as4MetaData.getServer());
        }*/

        Path datPath = PersisterUtils.createArtifactFolders(folder, inboundMetadata.getHeader()).resolve(
                String.format("%s.receipt.dat",
                        FileUtils.filterString(inboundMetadata.getTransmissionIdentifier().getIdentifier())));

        try (OutputStream outputStream = Files.newOutputStream(datPath)) {
            evidenceFactory.write(outputStream, inboundMetadata);
        } catch (EvidenceException e) {
            throw new IOException("Unable to persist receipt.", e);
        }

        Path jsonPath = PersisterUtils.createArtifactFolders(folder, inboundMetadata.getHeader()).resolve(
                String.format("%s.receipt.json",
                        FileUtils.filterString(inboundMetadata.getTransmissionIdentifier().getIdentifier())));
        try (OutputStream outputStream = Files.newOutputStream(jsonPath)) {
            HashMap<String, String> map = new HashMap<String, String>();
            map.put("SenderCertificate", inboundMetadata.getCertificate().getSubjectDN().getName());
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(outputStream, map);
        } catch (Exception e) {
            throw new IOException("Unable to persist json.", e);
        }

        log.debug("Receipt persisted to: {}", datPath);
    }

    /**
     * @since 4.0.3
     */
    @Override
    public void persist(TransmissionIdentifier transmissionIdentifier, Header header,
                        Path payloadPath, Exception exception) {
        try {
            log.warn("Transmission '{}' failed duo to {}.", transmissionIdentifier, exception.getMessage());

            // Delete temp file
            Files.delete(payloadPath);
        } catch (IOException e) {
            log.warn("Unable to delete file: {}", payloadPath, e);
        }
    }
}