/*******************************************************************************
 * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
 * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity
 * lawfully obtaining a copy of this software and associated documentation files (hereinafter the
 * Software) to redistribute and use the Software in source and binary forms, with or without modification.
 * Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and may permit others to do so, subject to the following conditions:
 * Redistributions of source code must retain the above copyright notice, this list of conditions and the
 * following disclaimers.
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided with the distribution.
 * Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any
 * form whatsoever without the express written consent of Battelle.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * General disclaimer for use with OSS licenses
 *
 * This material was prepared as an account of work sponsored by an agency of the United States Government.
 * Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any
 * of their employees, nor any jurisdiction or organization that has cooperated in the development of these
 * materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for
 * the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process
 * disclosed, or represents that its use would not infringe privately owned rights.
 *
 * Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer,
 * or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United
 * States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed
 * herein do not necessarily state or reflect those of the United States Government or any agency thereof.
 *
 * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
 * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
 ******************************************************************************/
package gov.pnnl.goss.gridappsd.simulation;

import java.io.Serializable;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import pnnl.goss.core.Client;
import pnnl.goss.core.ClientFactory;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Client.PROTOCOL;
// TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
//import pnnl.goss.core.security.SecurityConfig;
import gov.pnnl.goss.gridappsd.utils.GridAppsDConstants;
import gov.pnnl.goss.gridappsd.utils.RunCommandLine;

/**
 * SimulationEvent starts a single instance of simulation
 *
 * @author shar064
 *
 */
@Component(service = GossResponseEvent.class)
public class SimulationEvent implements GossResponseEvent {

    // TODO: Get these paths from pnnl.goss.gridappsd.cfg file
    String commandFNCS = "./fncs_broker 2";
    String commandGridLABD = "gridlabd";
    String commandFNCS_GOSS_Bridge = "python ./scripts/fncs_goss_bridge.py";

    @Reference
    private volatile ClientFactory clientFactory;
    // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
    // @Reference
    // private volatile SecurityConfig securityConfig;

    /**
     * message is in the JSON string format {'SimulationId': 1, 'SimulationFile':
     * '/path/name'}
     */
    @Override
    public void onMessage(Serializable message) {

        try {
            DataResponse event = (DataResponse) message;
            String username = event.getUsername();

            // TODO: Security removed in GOSS Java 21 upgrade - needs reimplementation
            // Credentials credentials = new UsernamePasswordCredentials(
            // securityConfig.getManagerUser(), securityConfig.getManagerPassword());
            Credentials credentials = new UsernamePasswordCredentials(
                    "system", "manager");

            Client client = clientFactory.create(PROTOCOL.STOMP, credentials);

            // Extract simulation id and simulation files from message
            // TODO: Parse message to get simulationId and simulationFile
            String simulationId = "1";
            String simulationFile = "filename";

            // Start FNCS
            RunCommandLine.runCommand(commandFNCS);

            // TODO: check if FNCS is started correctly and send publish simulation status
            // accordingly
            client.publish(GridAppsDConstants.topic_simulationLog + simulationId, "FNCS Co-Simulator started");

            // Start GridLAB-D
            RunCommandLine.runCommand(commandGridLABD + " " + simulationFile);

            // TODO: check if GridLAB-D is started correctly and send publish simulation
            // status accordingly
            client.publish(GridAppsDConstants.topic_simulationLog + simulationId, "GridLAB-D started");

            // Start GOSS-FNCS Bridge
            RunCommandLine.runCommand(commandFNCS_GOSS_Bridge);

            // TODO: check if bridge is started correctly and send publish simulation status
            // accordingly
            client.publish(GridAppsDConstants.topic_simulationLog + simulationId, "FNCS-GOSS Bridge started");

            // Subscribe to GOSS FNCS Bridge output topic
            client.subscribe(GridAppsDConstants.topic_COSIM_output, new FNCSOutputEvent());

            // Communicate with GOSS FNCS Bride to get status and output
            client.publish(GridAppsDConstants.topic_COSIM, "isInitialized");

        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

}
