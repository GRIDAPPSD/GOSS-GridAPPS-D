/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission
 * to any person or entity lawfully obtaining a copy of this software and
 * associated documentation files (hereinafter the Software) to redistribute
 * and use the Software in source and binary forms, with or without
 * modification.
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.distributed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.northconcepts.exception.SystemException;

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
        // Exactly effectiveMaxAttempts calls (item 5: the no-config default,
        // read off this default-constructed instance rather than a removed
        // static MAX_TOPOLOGY_ATTEMPTS field): the initial request plus
        // (effectiveMaxAttempts - 1) retries, no more. A genuine cold-start
        // timeout (no exception thrown, just a null reply) must still retry
        // the FULL budget: this is the invariant item 6 must preserve.
        Mockito.verify(client, Mockito.times(process.effectiveMaxAttempts)).getResponse(
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

    // --- GOSS-023 composition: a thrown SystemException on an attempt (the
    // cold-start transport-not-ready condition) must be retried exactly like a
    // null response, not allowed to kill the retry loop. ---

    @Test
    public void retryRecoversAfterThrownSystemExceptionOnEarlyAttempt() throws Exception {
        DataResponse dataResponse = Mockito.mock(DataResponse.class);
        // First attempt throws SystemException (the cold-start transport-not-ready
        // condition surfaced by GOSS-023's getResponse fix); the second attempt
        // succeeds. The loop must catch the throw, treat it as "not ready yet",
        // sleep, and retry rather than letting the thread die after attempt 1.
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenThrow(SystemException.wrap(new RuntimeException("transport not ready")))
                .thenReturn(dataResponse);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-throws-then-succeeds", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-throws-then-succeeds";

        Serializable result = process.requestTopologyWithRetry(request);

        assertNotNull("requestTopologyWithRetry must return the eventual non-null response "
                + "even after a thrown SystemException on an earlier attempt", result);
        org.junit.Assert.assertSame(dataResponse, result);
        // Exactly 2 calls: the throwing first attempt plus the successful retry.
        Mockito.verify(client, Mockito.times(2)).getResponse(Mockito.any(),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC), Mockito.eq(RESPONSE_FORMAT.JSON),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));
    }

    @Test
    public void retryExhaustsAttemptsAndReturnsNullWhenEveryAttemptThrows() throws Exception {
        // A plain wrapped RuntimeException (not a JMSSecurityException) is the
        // benign/ambiguous-cause case: item 6 classifies only an unambiguous
        // JMSSecurityException/JMSSecurityRuntimeException as non-retryable, so
        // this cause must still retry the FULL configured budget, exactly like a
        // clean timeout.
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenThrow(SystemException.wrap(new RuntimeException("transport never comes up")));

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-throws-exhausted", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-throws-exhausted";

        Serializable result = process.requestTopologyWithRetry(request);

        assertNull("requestTopologyWithRetry must return null (not propagate) once every "
                + "attempt throws and attempts are exhausted", result);
        // Exactly effectiveMaxAttempts calls (item 5: no-config default, read
        // off this instance): the loop must give up after this many, not loop
        // forever on repeated throws, and (item 6 invariant) must not give up
        // EARLY on this ambiguous, non-security cause either.
        Mockito.verify(client, Mockito.times(process.effectiveMaxAttempts)).getResponse(
                Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));
    }

    // --- GADP-051 review remediation item 6: a non-retryable (security)
    // failure cause breaks the retry loop early instead of spending the full
    // attempt budget on a request that cannot succeed. ---

    @Test
    public void retryBreaksEarlyOnJMSSecurityExceptionInsteadOfExhaustingTheFullBudget() throws Exception {
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenThrow(SystemException.wrap(new jakarta.jms.JMSSecurityException("not authorized")));

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-security-failure", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-security-failure";

        Serializable result = process.requestTopologyWithRetry(request);

        assertNull("a non-retryable failure must still return null cleanly (no propagate)", result);
        // Exactly ONE call: the loop must break after the very first attempt
        // when the cause is classified non-retryable, not spend the full
        // effectiveMaxAttempts budget (which is > 1 for the no-config default).
        Mockito.verify(client, Mockito.times(1)).getResponse(Mockito.any(),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC), Mockito.eq(RESPONSE_FORMAT.JSON),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));
    }

    @Test
    public void giveUpWarningNamesTheTrueCauseAfterANonRetryableFailure() throws Exception {
        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenThrow(SystemException.wrap(new jakarta.jms.JMSSecurityException("not authorized")));

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-security-cause-logged", client, logManager);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-security-cause-logged";

        Serializable result = process.requestTopologyWithRetry(request);
        process.handleTopologyResponse(result);

        // The terminal give-up warning must name the true cause ("not
        // authorized"), not only the generic "not answering" message, so an
        // operator can tell a rejected request apart from a service that
        // simply never started answering. Matched on the give-up warning's own
        // distinguishing text ("Last attempt failure cause"): the per-attempt
        // warning also mentions "not authorized" as the raw exception message,
        // so matching on the cause substring alone would double-match both
        // warn() invocations and fail Mockito's default times(1) check.
        Mockito.verify(logManager).warn(Mockito.eq(ProcessStatus.RUNNING), Mockito.isNull(),
                Mockito.contains("Last attempt failure cause: not authorized"));
    }

    // --- Operator-configurable retry window (Craig's amendment to CHANGE 2):
    // field.topology.request.timeout.seconds via ConfigAdmin PID
    // pnnl.goss.gridappsd, delivered through the same config map
    // FieldBusManagerImpl.getFieldModelMrid() already reads field.model.mrid
    // through. ---

    @Test
    public void resolveTopologyTimeoutSecondsReturnsDefaultWhenConfigIsNull() {
        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(null, logManager);

        assertEquals("a null config map (the no-config-map constructor path) must resolve to the default window",
                TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS, seconds);
    }

    @Test
    public void resolveTopologyTimeoutSecondsReturnsDefaultWhenPropertyIsAbsent() {
        Map<String, Object> config = new HashMap<>();
        config.put("field.model.mrid", "some-mrid");

        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(config, logManager);

        assertEquals("an absent property must resolve to the default window, not zero or a crash",
                TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS, seconds);
    }

    @Test
    public void resolveTopologyTimeoutSecondsHonorsAConfiguredOverride() {
        Map<String, Object> config = new HashMap<>();
        config.put(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY, "120");

        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(config, logManager);

        assertEquals("an operator-set property must override the default window with its exact value",
                120L, seconds);
    }

    @Test
    public void resolveTopologyTimeoutSecondsFallsBackSafelyOnUnparseableValue() {
        Map<String, Object> config = new HashMap<>();
        config.put(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY, "not-a-number");

        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(config, logManager);

        assertEquals("an unparseable property value must fall back to the default window, not a degenerate one",
                TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS, seconds);
        // A bad config value must be visible to the operator via a warning, not silent.
        Mockito.verify(logManager).warn(Mockito.eq(ProcessStatus.RUNNING), Mockito.isNull(),
                Mockito.contains(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY));
    }

    @Test
    public void resolveTopologyTimeoutSecondsFallsBackSafelyOnNonPositiveValue() {
        Map<String, Object> config = new HashMap<>();
        config.put(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY, "0");

        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(config, logManager);

        assertEquals("a non-positive property value must fall back to the default window, "
                + "never a zero/degenerate window that tries once and gives up instantly",
                TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS, seconds);
        Mockito.verify(logManager).warn(Mockito.eq(ProcessStatus.RUNNING), Mockito.isNull(),
                Mockito.contains(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY));
    }

    // --- L2: an operator-configured value beyond the sane ceiling is clamped,
    // with a WARN, rather than honored outright (a fat-fingered value like
    // 86400 would otherwise derive a ~24h effective retry window). ---

    @Test
    public void resolveTopologyTimeoutSecondsClampsAnOversizedConfiguredValue() {
        Map<String, Object> config = new HashMap<>();
        config.put(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY, "86400");

        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(config, logManager);

        assertEquals("a configured value beyond the cap must be clamped to the cap exactly, not honored outright",
                TopologyRequestProcess.MAX_TOPOLOGY_TIMEOUT_SECONDS, seconds);
        Mockito.verify(logManager).warn(Mockito.eq(ProcessStatus.RUNNING), Mockito.isNull(),
                Mockito.contains(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY));
    }

    @Test
    public void resolveTopologyTimeoutSecondsLeavesTheDefaultUnaffectedByTheClamp() {
        // Invariant: the default (no config map at all) must stay exactly the
        // documented 60s default and must be well under the cap, unaffected by
        // the L2 clamp logic.
        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(null, logManager);

        assertEquals(60L, TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS);
        assertEquals(TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS, seconds);
        assertTrue("the default must be well under the cap",
                TopologyRequestProcess.DEFAULT_TOPOLOGY_TIMEOUT_SECONDS < TopologyRequestProcess.MAX_TOPOLOGY_TIMEOUT_SECONDS);
    }

    @Test
    public void resolveTopologyTimeoutSecondsLeavesANormalOverrideUnaffectedByTheClamp() {
        // Invariant: a normal small operator override (well under the cap)
        // must be honored exactly, not clamped.
        Map<String, Object> config = new HashMap<>();
        config.put(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY, "120");

        long seconds = TopologyRequestProcess.resolveTopologyTimeoutSeconds(config, logManager);

        assertEquals("a normal override well under the cap must be honored exactly, unaffected by the clamp",
                120L, seconds);
        Mockito.verify(logManager, Mockito.never()).warn(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    public void computeMaxAttemptsMatchesTheDocumentedSixtySecondArithmetic() {
        // 10 attempts * 5000ms + 9 * 1000ms = 59000ms, within the 60s default budget.
        int attempts = TopologyRequestProcess.computeMaxAttempts(60L,
                TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS, TopologyRequestProcess.TOPOLOGY_RETRY_SLEEP_MS);

        assertEquals("the default 60s budget must derive exactly 10 attempts at the fixed 5000ms/1000ms sizing",
                10, attempts);
        // Item 5: MAX_TOPOLOGY_ATTEMPTS (the removed static duplicate) is gone;
        // a default-constructed (no-config) instance's effectiveMaxAttempts is
        // now the single source of truth for "what the no-config default
        // resolves to", and it must equal the same computeMaxAttempts result.
        TopologyRequestProcess defaultProcess = new TopologyRequestProcess("mrid-default-attempts-check", client,
                logManager);
        assertEquals("effectiveMaxAttempts on a default-constructed instance must equal the same computation",
                defaultProcess.effectiveMaxAttempts, attempts);
    }

    @Test
    public void computeMaxAttemptsNeverReturnsZeroEvenForATinyBudget() {
        int attempts = TopologyRequestProcess.computeMaxAttempts(1L,
                TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS, TopologyRequestProcess.TOPOLOGY_RETRY_SLEEP_MS);

        // L4: value-assert the exact expected result at this boundary (a 1s
        // budget is far smaller than one attempt's worth: (1000 + 1000) /
        // (5000 + 1000) = 0, floored up to 1), not merely ">= 1" (data-invariants
        // Rule 1: assert the actual value, not just a non-degenerate range).
        assertEquals("a budget smaller than one attempt's worth must floor to exactly 1 attempt, never 0",
                1, attempts);
    }

    @Test
    public void configuredOverrideChangesTheEffectiveRetryCountEndToEnd() throws Exception {
        // An operator-configured 6s window with the fixed 5000ms timeout / 1000ms
        // sleep sizing derives exactly 1 attempt (6000ms total budget; a single
        // attempt of 5000ms plus its trailing-sleep allowance of 1000ms already
        // consumes the whole budget), versus the 10-attempt default. This proves the
        // config value actually reaches and changes retry behavior, not just the
        // resolver helper in isolation.
        Map<String, Object> config = new HashMap<>();
        config.put(TopologyRequestProcess.TOPOLOGY_TIMEOUT_CONFIG_KEY, "6");

        Mockito.when(client.getResponse(Mockito.any(), Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC),
                Mockito.eq(RESPONSE_FORMAT.JSON), Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS)))
                .thenReturn(null);

        TopologyRequestProcess process = new TopologyRequestProcess("mrid-config-override", client, logManager,
                config);
        TopologyRequest request = new TopologyRequest();
        request.mRID = "mrid-config-override";

        assertEquals("the 6s configured window must derive exactly 1 effective attempt", 1,
                process.effectiveMaxAttempts);

        Serializable result = process.requestTopologyWithRetry(request);

        assertNull("with attempts exhausted the retry must still return null cleanly", result);
        Mockito.verify(client, Mockito.times(1)).getResponse(Mockito.any(),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_REQUEST_TOPIC), Mockito.eq(RESPONSE_FORMAT.JSON),
                Mockito.eq(TopologyRequestProcess.TOPOLOGY_RESPONSE_TIMEOUT_MS));
    }
}
