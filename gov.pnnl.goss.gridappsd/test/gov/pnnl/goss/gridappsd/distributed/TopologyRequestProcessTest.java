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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.dto.LogMessage.ProcessStatus;
import pnnl.goss.core.Client;
import pnnl.goss.core.DataResponse;

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
}
