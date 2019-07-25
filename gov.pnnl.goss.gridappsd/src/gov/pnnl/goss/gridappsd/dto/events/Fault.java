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
package gov.pnnl.goss.gridappsd.dto.events;

import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

public class Fault extends Event{

	private static final long serialVersionUID = 7348798730580951117L;
	
	public enum PhaseConnectedFaultKind {
	    lineToGround, lineToLine, lineToLineToGround, lineOpen
	}
	
	public enum FaultImpedance {
		rGround, rLineToLine, xGround, xLineToLine
	}
	
	public enum PhaseCode {
		ABCN(225),ABC(224),ABN(193),ACN(41),BCN(97),AB(132),AC(96),BC(65),AN(129),BN(65),
		CN(33),A(128),B(64),C(32),N(16),s1N(528),s2N(272),s12N(784),s1(512),
		s2(256),s12(768),none(0),X(1024),XY(3072),XN(1040),XYN(3088);
		
		private final int value;
		
		PhaseCode(final int newValue){
			value = newValue;
		}
		
		public int getValue(){
			return value;
		}
	}
	
	public Map<FaultImpedance,Double> FaultImpedance;
	
	public PhaseConnectedFaultKind PhaseConnectedFaultKind;
	
	public List<String> ObjectMRID;
	
	public PhaseCode phases;
	
	public Map<FaultImpedance,Double> getFaultImpedance() {
		return FaultImpedance;
	}

	public void setFaultImpedance(Map<FaultImpedance,Double> impedance) {
		this.FaultImpedance = impedance;
	}

	public PhaseConnectedFaultKind getPhaseConnectFaultKind() {
		return PhaseConnectedFaultKind;
	}

	public void setPhaseConnectFaultKind(
			PhaseConnectedFaultKind phaseConnectFaultKind) {
		PhaseConnectedFaultKind = phaseConnectFaultKind;
	}

	public List<String> getObjectMRID() {
		return ObjectMRID;
	}

	public void setObjectMRID(List<String> ObjectMRID) {
		this.ObjectMRID = ObjectMRID;
	}

	public PhaseCode getPhases() {
		return phases;
	}

	public void setPhases(PhaseCode phases) {
		this.phases = phases;
	}

	@Override
	public String toString() {
		Gson  gson = new Gson();
		return gson.toJson(this);
	}

	public static Fault parse(String jsonString){
		Gson  gson = new Gson();
		Fault obj = gson.fromJson(jsonString, Fault.class);
		//TODO: Check for mandatory fields and impedance-faultKind combination
		return obj;
	}

}