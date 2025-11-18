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
package gov.pnnl.goss.gridappsd.dto;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class ServiceInfo implements Serializable {

    public enum ServiceType {
        PYTHON, JAVA, WEB, EXE
    }

    public enum ServiceCategory {
        SIMULATOR, COSIMULATOR, SERVICE
    }

    String id;
    String description;
    String creator;
    List<String> input_topics;
    List<String> output_topics;
    List<String> static_args;
    String execution_path;
    HashMap<String, UserOptions> user_input;
    ServiceType type;
    boolean launch_on_startup;
    List<String> service_dependencies;
    boolean multiple_instances;
    List<EnvironmentVariable> environmentVariables;
    ServiceCategory category = ServiceCategory.SERVICE;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String creator) {
        this.creator = creator;
    }

    public List<String> getInput_topics() {
        return input_topics;
    }

    public void setInput_topics(List<String> input_topics) {
        this.input_topics = input_topics;
    }

    public List<String> getOutput_topics() {
        return output_topics;
    }

    public void setOutput_topics(List<String> output_topics) {
        this.output_topics = output_topics;
    }

    public List<String> getStatic_args() {
        return static_args;
    }

    public void setStatic_args(List<String> static_args) {
        this.static_args = static_args;
    }

    public String getExecution_path() {
        return execution_path;
    }

    public void setExecution_path(String execution_path) {
        this.execution_path = execution_path;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType type) {
        this.type = type;
    }

    public boolean isLaunch_on_startup() {
        return launch_on_startup;
    }

    public void setLaunch_on_startup(boolean launch_on_startup) {
        this.launch_on_startup = launch_on_startup;
    }

    public List<String> getService_dependencies() {
        return service_dependencies;
    }

    public void setService_dependencies(List<String> service_dependencies) {
        this.service_dependencies = service_dependencies;
    }

    public boolean isMultiple_instances() {
        return multiple_instances;
    }

    public void setMultiple_instances(boolean multiple_instances) {
        this.multiple_instances = multiple_instances;
    }

    public List<EnvironmentVariable> getEnvironmentVariables() {
        return environmentVariables;
    }

    public void setEnvironmentVariables(List<EnvironmentVariable> environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public ServiceCategory getCatagory() {
        return category;
    }

    public void setCatagory(ServiceCategory catagory) {
        this.category = catagory;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static ServiceInfo parse(String jsonString) {
        Gson gson = new Gson();
        ServiceInfo obj = gson.fromJson(jsonString, ServiceInfo.class);
        if (obj.id == null)
            throw new JsonSyntaxException("Expected attribute service_id not found");
        return obj;
    }

    public static void main(String[] args) throws IOException {

        File test = new File("../services/ochre.config");
        System.out.println(ServiceInfo.parse(new String(Files.readAllBytes(Paths.get(test.getAbsolutePath())))));
    }

}
