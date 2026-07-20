/**
* Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.opengroup.osdu.file.aws.apitest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opengroup.osdu.file.TestBase;
import org.opengroup.osdu.file.aws.util.HttpClientAws;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TestFileExpiryTime extends TestBase {

    private static final String GET_LOCATION = "/getLocation";
    private static final String GET_UPLOAD_URL_WITH_EXPIRY = "/files/uploadURL?expiryTime=%s";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void setUp() throws IOException {
        client = new HttpClientAws();
    }

    @Test
    public void testDefaultExpiryTime() throws Exception {
        // Act - Get signed URL with default expiry (1 hour)
        ClientResponse response = client.send(GET_LOCATION, "POST", getCommonHeader(), "{}");
        String responseBody = response.getEntity(String.class);
        JsonNode responseJson = objectMapper.readTree(responseBody);

        // Assert - Check that credentials expire in approximately 1 hour
        JsonNode locationNode = responseJson.get("Location");
        assertNotNull(locationNode, "Location node should not be null");

        String signedUrl = locationNode.get("SignedURL").asText();
        assertNotNull(signedUrl, "SignedURL should not be null");

        // Extract X-Amz-Expires from the signed URL
        int expiresSeconds = extractExpiresFromUrl(signedUrl);
        int expiresMinutes = expiresSeconds / 60;

        // Should be approximately 60 minutes (allow 5 minute tolerance)
        assertTrue(expiresMinutes >= 55 && expiresMinutes <= 65,
            "Expected ~60 minutes, got " + expiresMinutes);
    }

    @Test
    public void testCustomExpiryTime15Minutes() throws Exception {
        // Act - Request 10 minute expiry which should work within STS limitations
        String endpoint = String.format(GET_UPLOAD_URL_WITH_EXPIRY, "15M");
        ClientResponse response = client.send(endpoint, "GET", getCommonHeader(), null);
        String responseBody = response.getEntity(String.class);
        JsonNode responseJson = objectMapper.readTree(responseBody);

        // Assert - Should succeed and have ~10 minute expiry
        assertEquals(200, response.getStatus(), "Expected 200 for 15M expiry request");

        JsonNode locationNode = responseJson.get("Location");
        assertNotNull(locationNode, "Location node should not be null");

        String signedUrl = locationNode.get("SignedURL").asText();
        assertNotNull(signedUrl, "SignedURL should not be null");

        // Extract X-Amz-Expires from the signed URL
        int expiresSeconds = extractExpiresFromUrl(signedUrl);
        int expiresMinutes = expiresSeconds / 60;

        // Should be approximately 10 minutes (allow 2 minute tolerance)
        assertTrue(expiresMinutes >= 10 && expiresMinutes <= 20,
            "Expected ~15 minutes, got " + expiresMinutes);
    }

    @Test
    public void testCustomExpiryTime12Hours() throws Exception {
        // Act - Request 12 hour expiry which should fail due to STS role chaining limitation
        String endpoint = String.format(GET_UPLOAD_URL_WITH_EXPIRY, "12H");
        ClientResponse response = client.send(endpoint, "GET", getCommonHeader(), null);

        // Assert - Should return 400 error due to STS limitation
        assertEquals(400, response.getStatus(),
            "Expected 400 error when requesting 12H expiry due to STS role chaining limitation");
    }

    @Test
    public void testMaximumExpiryTimeCapped() throws Exception {
        // Act - Request 10 days which should fail due to STS role chaining limitation
        String endpoint = String.format(GET_UPLOAD_URL_WITH_EXPIRY, "10D");
        ClientResponse response = client.send(endpoint, "GET", getCommonHeader(), null);

        // Assert - Should return 400 error due to STS limitation
        assertEquals(400, response.getStatus(),
            "Expected 400 error when requesting 10D expiry due to STS role chaining limitation");
    }

    /**
     * Helper method to extract X-Amz-Expires parameter from signed URL
     */
    private int extractExpiresFromUrl(String signedUrl) {
        String[] parts = signedUrl.split("[?&]");
        for (String part : parts) {
            if (part.startsWith("X-Amz-Expires=")) {
                return Integer.parseInt(part.substring("X-Amz-Expires=".length()));
            }
        }
        throw new IllegalArgumentException("X-Amz-Expires not found in URL: " + signedUrl);
    }
}
