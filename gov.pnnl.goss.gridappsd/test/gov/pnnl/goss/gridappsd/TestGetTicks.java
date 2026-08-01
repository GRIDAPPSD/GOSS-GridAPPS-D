/// *******************************************************************************
// * Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
// * Battelle Memorial Institute (hereinafter Battelle) hereby grants permission
/// to any person or entity
// * lawfully obtaining a copy of this software and associated documentation
/// files (hereinafter the
// * Software) to redistribute and use the Software in source and binary forms,
/// with or without modification.
// * Such person or entity may use, copy, modify, merge, publish, distribute,
/// sublicense, and/or sell copies of
// * the Software, and may permit others to do so, subject to the following
/// conditions:
// * Redistributions of source code must retain the above copyright notice, this
/// list of conditions and the
// * following disclaimers.
// * Redistributions in binary form must reproduce the above copyright notice,
/// this list of conditions and
// * the following disclaimer in the documentation and/or other materials
/// provided with the distribution.
// * Other than as used herein, neither the name Battelle Memorial Institute or
/// Battelle may be used in any
// * form whatsoever without the express written consent of Battelle.
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
/// AND ANY
// * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
/// WARRANTIES OF
// * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
/// EVENT SHALL
// * BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
/// SPECIAL, EXEMPLARY,
// * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
/// SUBSTITUTE
// * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
/// HOWEVER CAUSED
// * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
/// TORT (INCLUDING
// * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
/// SOFTWARE, EVEN IF ADVISED
// * OF THE POSSIBILITY OF SUCH DAMAGE.
// * General disclaimer for use with OSS licenses
// *
// * This material was prepared as an account of work sponsored by an agency of
/// the United States Government.
// * Neither the United States Government nor the United States Department of
/// Energy, nor Battelle, nor any
// * of their employees, nor any jurisdiction or organization that has
/// cooperated in the development of these
// * materials, makes any warranty, express or implied, or assumes any legal
/// liability or responsibility for
// * the accuracy, completeness, or usefulness or any information, apparatus,
/// product, software, or process
// * disclosed, or represents that its use would not infringe privately owned
/// rights.
// *
// * Reference herein to any specific commercial product, process, or service by
/// trade name, trademark, manufacturer,
// * or otherwise does not necessarily constitute or imply its endorsement,
/// recommendation, or favoring by the United
// * States Government or any agency thereof, or Battelle Memorial Institute.
/// The views and opinions of authors expressed
// * herein do not necessarily state or reflect those of the United States
/// Government or any agency thereof.
// *
// * PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the
// * UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
// ******************************************************************************/
// package gov.pnnl.goss.gridappsd;
//
// import java.io.Serializable;
//
// import org.apache.http.auth.Credentials;
// import org.apache.http.auth.UsernamePasswordCredentials;
//
// import junit.framework.TestCase;
// import pnnl.goss.core.Client;
// import pnnl.goss.core.Client.PROTOCOL;
// import pnnl.goss.core.ClientFactory;
// import pnnl.goss.core.GossResponseEvent;
// import pnnl.goss.core.client.ClientServiceFactory;
// import pnnl.goss.gridappsd.utils.GridAppsDConstants;
// import pnnl.goss.gridappsd.utils.RunCommandLine;
//
// public class TestGetTicks {
//
//
//
// public static void main(String[] args){
//
// try {
//
// ClientFactory clientFactory = new ClientServiceFactory();
//
// Client client;
//
// //Step1: Create GOSS Client
// Credentials credentials = new UsernamePasswordCredentials(
// GridAppsDConstants.username, GridAppsDConstants.password);
// client = clientFactory.create(PROTOCOL.OPENWIRE, credentials);
//
// //Subscribe to bridge output
// client.subscribe("*", new GossResponseEvent() {
//
// @Override
// public void onMessage(Serializable response) {
// System.out.println("simulation output is: "+response);
//
// }
// });
//
// //Start fncs_goss_bridge.py
// //RunCommandLine.runCommand("python ./scripts/fncs_goss_bridge.py");
//
//
//
// while (true){
// Thread.sleep(1000);
// }
//
//
//
//
// } catch (Exception e) {
// e.printStackTrace();
// }
//
// }
//
//
// }
