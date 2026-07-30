/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission
 * to any person or entity lawfully obtaining a copy of this software and
 * associated documentation files (hereinafter the Software) to redistribute
 * and use the Software in source and binary forms, with or without
 * modification.
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.distributed;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.Request.RESPONSE_FORMAT;

/**
 * Unit tests for TopologyRequestProcess response handling (GADP-005).
 *
 * These assert the null-idle fail-safe and the valid-parse path per the
 * acceptance criteria: 1. A null topology response does not throw, leaves root
 * null, and logs an actionable warning (subscribed-but-idle posture,
 * recoverable later). 2. A valid DataResponse carrying topology JSON parses
 * into a Root tree.
 *
 * The response-handling logic is extracted into the package-visible
 * handleTopologyResponse so it can be exercised without the live STOMP
 * client.getResponse() call that run() otherwise makes.
 */
@RunWith(MockitoJUnitRunner.class)
public class TopologyRequestProcessTest {

    @Mock
    private Client client;

    @Mock
    private LogManager logManager;

    // --- Null-idle fail-safe: null response must not NPE; root stays null ---

    @Test
    public void nullResponseLeavesRootNullAndDoesNotThrow() {
        TopologyRequestProcess process = new TopologyRequestProcess("mrid-null-test", client, logManager);

        boolean populated = process.handleTopologyResponse(null);

        assertFalse("handleTopologyResponse must report root not populated on null", populated);
        assertNull("root must remain null when the topology response is null", process.root);
    }

    @Test
    public void nullResponseLogsActionableWarning() {
        TopologyRequestProcess process = new TopologyRequestProcess("mrid-warn-test", client, logManager);

        process.handleTopologyResponse(null);

        // The warning must name the idle posture so operators can act on it. The
        // mrid is included so the message identifies which field model is idle.
        Mockito.verify(logManager).warn(Mockito.eq(ProcessStatus.RUNNING), Mockito.isNull(),
                Mockito.contains("mrid-warn-test"));
    }

    // --- Valid-parse path: a DataResponse carrying topology JSON parses Root ---

    @Test
    public void validDataResponseParsesRootTree() {
        String topologyJson = "{\"DistributionArea\":{\"@id\":\"da-1\",\"@type\":\"area\","
                + "\"Substations\":[]}}";
        DataResponse dataResponse = Mockito.mock(DataResponse.class);
        Mockito.when(dataResponse.getData()).thenReturn(topologyJson);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-parse-test", client, logManager);

        boolean populated = process.handleTopologyResponse(dataResponse);

        assertTrue("handleTopologyResponse must report root populated on valid JSON", populated);
        assertNotNull("root must be parsed from a valid DataResponse", process.root);
        assertNotNull("parsed root must carry its DistributionArea", process.root.DistributionArea);
        org.junit.Assert.assertEquals("da-1", process.root.DistributionArea.id);
        // No warning is logged on the success path.
        Mockito.verify(logManager, Mockito.never()).warn(Mockito.any(), Mockito.any(), Mockito.any());
    }

    // --- Non-DataResponse valid-parse path: a raw JSON String response (not
    // wrapped in DataResponse) must still parse into Root via toString(). ---

    @Test
    public void nonDataResponseStringParsesRootTree() throws Exception {
        String topologyJson = "{\"DistributionArea\":{\"@id\":\"da-raw-1\",\"@type\":\"area\","
                + "\"Substations\":[]}}";

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-raw-test", client, logManager);

        boolean populated = process.handleTopologyResponse(topologyJson);

        assertTrue("handleTopologyResponse must report root populated on a raw JSON String",
                populated);
        assertNotNull("root must be parsed from a non-DataResponse String", process.root);
        assertNotNull("parsed root must carry its DistributionArea", process.root.DistributionArea);
        org.junit.Assert.assertEquals("da-raw-1", process.root.DistributionArea.id);
        // No warning is logged on the success path.
        Mockito.verify(logManager, Mockito.never()).warn(Mockito.any(), Mockito.any(), Mockito.any());
    }

    // --- Bounded retry: requestTopologyWithRetry (GADP-005 fix round 2) ---

    @Test
    public void retrySucceedsImmediatelyWithoutSleepingOrRetrying() throws Exception {
        DataResponse dataResponse = Mockito.mock(DataResponse.class);
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenReturn(dataResponse);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-immediate-success", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-immediate-success";

        Serializable result = process.requestTopologyWithRetry(request);

        assertNotNull("first successful getResponse must be returned as-is", result);
        org.junit.Assert.assertSame(dataResponse, result);
        Mockito.verify(client, Mockito.times(1)).getResponse(Mockito.any(),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC), Mockito.eq(RESPONSE_FORMAT.JSON),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));
    }

    @Test
    public void retryRecoversAfterTransientNullResponses() throws Exception {
        DataResponse dataResponse = Mockito.mock(DataResponse.class);
        // First two calls return null (service not yet answering); the third call
        // succeeds. requestTopologyWithRetry must sleep between attempts and keep
        // re-requesting rather than giving up on the first null.
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenReturn(null, null, dataResponse);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-recovers", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-recovers";

        Serializable result = process.requestTopologyWithRetry(request);

        assertNotNull("requestTopologyWithRetry must return the eventual non-null response", result);
        org.junit.Assert.assertSame(dataResponse, result);
        // Exactly 3 calls: the initial request plus 2 retries, no more (no
        // busy-spin past the successful attempt).
        Mockito.verify(client, Mockito.times(3)).getResponse(Mockito.any(),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC), Mockito.eq(RESPONSE_FORMAT.JSON),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));
    }

    @Test
    public void retryExhaustsAttemptsAndReturnsNullWhenServiceNeverAnswers() throws Exception {
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenReturn(null);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-exhausted", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-exhausted";

        Serializable result = process.requestTopologyWithRetry(request);

        assertNull("requestTopologyWithRetry must return null once attempts are exhausted", result);
        // Exactly MAX_TOPOLOGY_ATTEMPTS calls: the initial request plus
        // (MAX_TOPOLOGY_ATTEMPTS - 1) retries, no more.
        Mockito.verify(client, Mockito.times(TopologyRequestProcess.MAX_TOPOLOGY_ATTEMPTS)).getResponse(
                Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));

        // Feeding the exhausted (null) result into handleTopologyResponse exercises
        // the full null-idle path this retry loop feeds into.
        boolean populated = process.handleTopologyResponse(result);
        assertFalse("null result after exhausted retries must leave root unpopulated", populated);
        assertNull("root must remain null after exhausted retries", process.root);
    }

    // --- Per-attempt timeout: requestTopologyWithRetry must call the bounded
    // overload rather than the unbounded one (GADP-051 fix round 3) ---

    @Test
    public void requestUsesTheBoundedTimeoutOverloadNotTheUnboundedOne() throws Exception {
        DataResponse dataResponse = Mockito.mock(DataResponse.class);
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenReturn(dataResponse);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-bounded-call", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-bounded-call";

        process.requestTopologyWithRetry(request);

        // The unbounded 3-arg overload must never be called: a first attempt on
        // that overload could block the retry loop forever if the topology
        // service is not yet answerable (the exact GADP-051 boot-order race).
        Mockito.verify(client, Mockito.never()).getResponse(Mockito.any(),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC), Mockito.eq(RESPONSE_FORMAT.JSON));
    }
}
