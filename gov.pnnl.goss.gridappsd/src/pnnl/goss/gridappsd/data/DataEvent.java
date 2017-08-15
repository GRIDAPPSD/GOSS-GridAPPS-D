/*******************************************************************************
 * Copyright © 2017, Battelle Memorial Institute All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY 
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
package pnnl.goss.gridappsd.data;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.pnnl.goss.gridappsd.api.DataManager;
import pnnl.goss.core.DataResponse;
import pnnl.goss.core.GossResponseEvent;
import pnnl.goss.core.Response;

/**
 *  1. Start FNCS
 *	2. Start GridLAB-D with input file location and name
 *	3. Start GOSS-FNCS Bridge
 *	4. Call FNCS IsInitialized()
 *	5. Publish 'Simulation Initialized' on 'simulation/[id]/status' once IsInitialized() returns.
 *		If IsInitialized() does not return in given time then publish error on 'simulation/[id]/status' and send 'die' message to GOSS-FNCS topic simulation/[id]/input
 * @author shar064
 *
 */
public class DataEvent implements GossResponseEvent {
	
	private volatile DataManager dataManager;
    private Logger log = LoggerFactory.getLogger(getClass());

	
	public DataEvent(DataManager manager){
		this.dataManager = manager;
	}
	
	
//	@Override
	public void onMessage(Serializable message) {
		
		/*  Parse message. message is in JSON string.
		 *  create and return response as simulation id
		 *  
		 *  make synchronous call to DataManager and receive file location
		 *  
		 *  Start FNCS
		 *	Start GridLAB-D with input file location and name
		 *	Start GOSS-FNCS Bridge
		 *	Call FNCS IsInitialized()
		 *  
		 *	Publish 'Simulation Initialized' on 'simulation/[id]/status' once IsInitialized() returns.
		 *		If IsInitialized() does not return in given time then publish error on 'simulation/[id]/status' and send 'die' message to GOSS-FNCS topic simulation/[id]/input
		*/

		Serializable requestData = null;
		
		if(message instanceof DataRequest){
			requestData = ((DataRequest)message).getRequestContent();
		} else if(message instanceof DataResponse){
			//TODO figure out why it is double nested in dataresponse
			if(((DataResponse)message).getData() instanceof DataResponse){
				requestData = ((DataResponse)((DataResponse)message).getData()).getData();
			}else{
				requestData = ((DataResponse)message).getData();
			}
		} else {
			requestData = message;
		}
		
		try {
			//TODO set up simulation id and temp data path

			Response r = dataManager.processDataRequest(requestData, 0, ".");
			//TODO create client and send response on it

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	

}
