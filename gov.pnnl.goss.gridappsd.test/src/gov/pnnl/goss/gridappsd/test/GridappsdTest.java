package gov.pnnl.goss.gridappsd.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class GridappsdTest {

    private final BundleContext context = FrameworkUtil.getBundle(this.getClass()).getBundleContext();

    /*
     *
     */
    @Test
    public void testGridappsd() throws Exception {
        Assert.assertNotNull(context);
    }
}
