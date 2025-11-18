package gov.pnnl.goss.gridappsd.role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.northconcepts.exception.SystemException;

import gov.pnnl.goss.gridappsd.api.RoleManager;

@Component(service = RoleManager.class)
public class RoleManagerImpl implements RoleManager {

    private static final Logger log = LoggerFactory.getLogger(RoleManagerImpl.class);
    private static final String CONFIG_PID = "pnnl.goss.gridappsd.security.rolefile";

    private HashMap<String, List<String>> roles = new HashMap<String, List<String>>();

    @Override
    public List<String> getRoles(String userName) throws Exception {
        if (!roles.containsKey(userName)) {
            throw new Exception("No roles specified for user " + userName);
        }

        return roles.get(userName);
    }

    @Override
    public boolean hasRole(String userName, String roleName) throws Exception {
        if (!roles.containsKey(userName)) {
            throw new Exception("No roles specified for user " + userName);
        }

        List<String> groups = roles.get(userName);
        return groups.contains(roleName);
    }

    // TODO: @ConfigurationDependency migration - This method may need refactoring
    // to use OSGi DS configuration
    // Original: @ConfigurationDependency(pid=CONFIG_PID)
    public synchronized void updated(Dictionary<String, ?> properties) throws SystemException {
        if (properties != null) {
            Enumeration<String> keys = properties.keys();

            while (keys.hasMoreElements()) {
                String user = keys.nextElement();
                String groups = properties.get(user).toString();
                System.out.println("Registering user roles: " + user + " --  " + groups);
                List<String> groupList = new ArrayList<>(Arrays.asList(StringUtils.split(groups, ",")));
                roles.put(user, groupList);
            }
        }
    }

}
