package gov.pnnl.goss.gridappsd;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import gov.pnnl.goss.gridappsd.api.ConfigurationManager;
import gov.pnnl.goss.gridappsd.api.DataManager;
import gov.pnnl.goss.gridappsd.api.LogManager;
import gov.pnnl.goss.gridappsd.data.ProvenTimeSeriesDataManagerImpl;
import gov.pnnl.goss.gridappsd.dto.RequestTimeseriesDataBasic;

/**
 * Unit tests for ProvenTimeSeriesDataManagerImpl, specifically testing the
 * buildInfluxQuery() method's time conversion logic.
 *
 * The handlers send time values in different formats: - Weather:
 * c.getTimeInMillis() + "000000" = milliseconds + "000000" suffix (19 digits) -
 * already nanoseconds - Zipload: c.getTimeInMillis() = plain milliseconds (13
 * digits) - needs conversion to nanoseconds
 */
@RunWith(MockitoJUnitRunner.class)
public class ProvenTimeSeriesDataManagerImplTest {

    @Mock
    private LogManager logManager;

    @Mock
    private DataManager dataManager;

    @Mock
    private ConfigurationManager configManager;

    private ProvenTimeSeriesDataManagerImpl manager;

    @Before
    public void setUp() throws Exception {
        manager = new ProvenTimeSeriesDataManagerImpl();
        // Set mocks via reflection since these are OSGi @Reference fields
        setField(manager, "logManager", logManager);
        setField(manager, "dataManager", dataManager);
        setField(manager, "configManager", configManager);
    }

    /**
     * Test that millisecond timestamps (13 digits, zipload format) are correctly
     * converted to nanoseconds.
     *
     * Zipload handler sends: c.getTimeInMillis() = "1546300800000" (13 digits) This
     * should be multiplied by 1,000,000 to get nanoseconds.
     */
    @Test
    public void testBuildInfluxQuery_withMilliseconds_convertsToNanoseconds() throws Exception {
        // Zipload format: plain milliseconds (13 digits)
        // 2019-01-01 00:00:00 UTC = 1546300800000 ms
        RequestTimeseriesDataBasic request = new RequestTimeseriesDataBasic();
        request.setQueryMeasurement("ieeezipload");
        Map<String, Object> filter = new HashMap<>();
        filter.put("startTime", "1546300800000"); // 2019-01-01 00:00:00 UTC in ms
        filter.put("endTime", "1546304400000"); // 2019-01-01 01:00:00 UTC in ms
        request.setQueryFilter(filter);

        String query = invokeBuildInfluxQuery(request);

        // Milliseconds should be converted to nanoseconds: ms * 1,000,000
        // 1546300800000 * 1000000 = 1546300800000000000
        assertTrue("Query should contain start time in nanoseconds",
                query.contains("time >= 1546300800000000000"));
        assertTrue("Query should contain end time in nanoseconds",
                query.contains("time <= 1546304400000000000"));
    }

    /**
     * Test that nanosecond timestamps (19 digits, weather format) are used directly
     * without conversion.
     *
     * Weather handler sends: c.getTimeInMillis() + "000000" = "1546300800000000000"
     * (19 digits) This is already nanoseconds and should be used directly.
     */
    @Test
    public void testBuildInfluxQuery_withNanosecondString_usesDirectly() throws Exception {
        // Weather format: milliseconds + "000000" (19 digits) - already nanoseconds
        RequestTimeseriesDataBasic request = new RequestTimeseriesDataBasic();
        request.setQueryMeasurement("weather");
        Map<String, Object> filter = new HashMap<>();
        filter.put("startTime", "1546300800000000000"); // Already nanoseconds
        filter.put("endTime", "1546304400000000000");
        request.setQueryFilter(filter);

        String query = invokeBuildInfluxQuery(request);

        // Should use directly without conversion
        assertTrue("Query should contain start time unchanged",
                query.contains("time >= 1546300800000000000"));
        assertTrue("Query should contain end time unchanged",
                query.contains("time <= 1546304400000000000"));
    }

    /**
     * Test that microsecond timestamps (16 digits) are correctly converted to
     * nanoseconds by multiplying by 1000.
     */
    @Test
    public void testBuildInfluxQuery_withMicroseconds_convertsToNanoseconds() throws Exception {
        // Microseconds format (16 digits)
        RequestTimeseriesDataBasic request = new RequestTimeseriesDataBasic();
        request.setQueryMeasurement("test_measurement");
        Map<String, Object> filter = new HashMap<>();
        filter.put("startTime", "1546300800000000"); // Microseconds (16 digits)
        filter.put("endTime", "1546304400000000");
        request.setQueryFilter(filter);

        String query = invokeBuildInfluxQuery(request);

        // Microseconds should be converted to nanoseconds: us * 1000
        // 1546300800000000 * 1000 = 1546300800000000000
        assertTrue("Query should contain start time in nanoseconds (from microseconds)",
                query.contains("time >= 1546300800000000000"));
        assertTrue("Query should contain end time in nanoseconds (from microseconds)",
                query.contains("time <= 1546304400000000000"));
    }

    /**
     * Test query generation with no time filter (other filters only).
     */
    @Test
    public void testBuildInfluxQuery_withNonTimeFilter() throws Exception {
        RequestTimeseriesDataBasic request = new RequestTimeseriesDataBasic();
        request.setQueryMeasurement("weather");
        Map<String, Object> filter = new HashMap<>();
        filter.put("place", "Denver");
        request.setQueryFilter(filter);

        String query = invokeBuildInfluxQuery(request);

        assertTrue("Query should contain SELECT * FROM weather",
                query.contains("SELECT * FROM weather"));
        assertTrue("Query should contain WHERE clause with place filter",
                query.contains("place = 'Denver'"));
    }

    /**
     * Test query generation with no filters at all.
     */
    @Test
    public void testBuildInfluxQuery_withNoFilter() throws Exception {
        RequestTimeseriesDataBasic request = new RequestTimeseriesDataBasic();
        request.setQueryMeasurement("ieeezipload");

        String query = invokeBuildInfluxQuery(request);

        assertEquals("SELECT * FROM ieeezipload", query);
    }

    /**
     * Test query with mixed time and non-time filters.
     */
    @Test
    public void testBuildInfluxQuery_withMixedFilters() throws Exception {
        RequestTimeseriesDataBasic request = new RequestTimeseriesDataBasic();
        request.setQueryMeasurement("weather");
        Map<String, Object> filter = new HashMap<>();
        filter.put("startTime", "1546300800000"); // 13-digit milliseconds
        filter.put("place", "Denver");
        request.setQueryFilter(filter);

        String query = invokeBuildInfluxQuery(request);

        assertTrue("Query should contain measurement",
                query.contains("SELECT * FROM weather"));
        assertTrue("Query should contain time filter in nanoseconds",
                query.contains("time >= 1546300800000000000"));
        assertTrue("Query should contain place filter",
                query.contains("place = 'Denver'"));
    }

    // Helper method to invoke the private buildInfluxQuery method
    private String invokeBuildInfluxQuery(RequestTimeseriesDataBasic request) throws Exception {
        Method method = ProvenTimeSeriesDataManagerImpl.class.getDeclaredMethod(
                "buildInfluxQuery",
                gov.pnnl.goss.gridappsd.dto.RequestTimeseriesData.class);
        method.setAccessible(true);
        return (String) method.invoke(manager, request);
    }

    // Helper method to set private fields via reflection
    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
