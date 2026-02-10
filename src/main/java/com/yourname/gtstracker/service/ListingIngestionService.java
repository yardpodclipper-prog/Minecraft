package com.yourname.gtstracker.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ingests and parses listing content from incoming chat messages.
 */
public class ListingIngestionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListingIngestionService.class);

    public void ingestChatMessage(String plainMessage) {
        // Placeholder for real parser/persistence behavior.
        LOGGER.debug("Ingested chat message payload: {}", plainMessage);
    }
}
