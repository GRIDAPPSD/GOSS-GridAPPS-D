/*******************************************************************************
 * Copyright 2017, Battelle Memorial Institute All rights reserved.
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

import java.io.Serializable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import gov.pnnl.gridappsd.cimhub.dto.ModelState;

public class ModelCreationConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public double load_scaling_factor = 1; // -l .2 to 1
    public char triplex = 'y'; // allowed values y|n to include secondary maps to -t
    public char encoding = 'u'; // allowed values u|i for UTF-8 or ISO-8859-1 maps to -e
    public int system_frequency = 60; // maps to -f
    public double voltage_multiplier = 1; // multiplier that converts voltage to V for GridLAB-D maps to -v
    public double power_unit_conversion = 1; // allowed values {1000|1|0.001}, multiplier that converts p,q,s to VA for
                                             // GridLAB-D maps to -s
    public char unique_names = 'y'; // allowed values y|n are unique names used? maps to -q
    public String schedule_name; // root filename for scheduled ZIP loads (defaults to none) maps to -n
    public double z_fraction = 0; // allowed values {0....1} constant Z portion (defaults to 0 for CIM-defined,
                                  // maps to -z
    public double i_fraction = 1; // allowed values {0....1} constant I portion (defaults to 0 for CIM-defined,
                                  // maps to -i
    public double p_fraction = 0; // allowed values {0....1} constant P portion (defaults to 0 for CIM-defined,
                                  // maps to -p
    public boolean randomize_zipload_fractions = false; // should randomize the zipload fraction values (eg. z, i,
                                                        // p_fractions)
    public boolean use_houses = false;
    public ModelState model_state;
    public String separated_loads_file; // option xslx file containing loads names that will be modeled separate from
                                        // the main powerflow simulator.

    public double getLoadScalingFactor() {
        return load_scaling_factor;
    }

    public void setLoadScalingFactor(double loadScalingFactor) {
        this.load_scaling_factor = loadScalingFactor;
    }

    public char getTriplex() {
        return triplex;
    }

    public void setTriplex(char triplex) {
        this.triplex = triplex;
    }

    public char getEncoding() {
        return encoding;
    }

    public void setEncoding(char encoding) {
        this.encoding = encoding;
    }

    public int getSystemFrequency() {
        return system_frequency;
    }

    public void setSystemFrequency(int systemFrequency) {
        this.system_frequency = systemFrequency;
    }

    public double getVoltageMultiplier() {
        return voltage_multiplier;
    }

    public void setVoltageMultiplier(double voltageMultiplier) {
        this.voltage_multiplier = voltageMultiplier;
    }

    public double getPowerUnitConversion() {
        return power_unit_conversion;
    }

    public void setPowerUnitConversion(double powerUnitConversion) {
        this.power_unit_conversion = powerUnitConversion;
    }

    public char getUniqueNames() {
        return unique_names;
    }

    public void setUniqueNames(char uniqueNames) {
        this.unique_names = uniqueNames;
    }

    public String getScheduleName() {
        return schedule_name;
    }

    public void setScheduleName(String scheduleName) {
        this.schedule_name = scheduleName;
    }

    public double getzFraction() {
        return z_fraction;
    }

    public void setzFraction(double zFraction) {
        this.z_fraction = zFraction;
    }

    public double getiFraction() {
        return i_fraction;
    }

    public void setiFraction(double iFraction) {
        this.i_fraction = iFraction;
    }

    public double getpFraction() {
        return p_fraction;
    }

    public void setpFraction(double pFraction) {
        this.p_fraction = pFraction;
    }

    public boolean isRandomize_zipload_fractions() {
        return randomize_zipload_fractions;
    }

    public void setRandomize_zipload_fractions(boolean randomize_zipload_fractions) {
        this.randomize_zipload_fractions = randomize_zipload_fractions;
    }

    public boolean isUse_houses() {
        return use_houses;
    }

    public void setUse_houses(boolean add_houses) {
        this.use_houses = add_houses;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public ModelState getModel_state() {
        return model_state;
    }

    public void setModel_state(ModelState model_state) {
        this.model_state = model_state;
    }

    public String getSeparateLoadsFile() {
        return separated_loads_file;
    }

    public void setSeparateLoadsFile(String fileName) {
        this.separated_loads_file = fileName;
    }

    public static ModelCreationConfig parse(String jsonString) {
        Gson gson = new Gson();
        ModelCreationConfig obj = gson.fromJson(jsonString, ModelCreationConfig.class);
        if (obj.schedule_name == null)
            throw new JsonSyntaxException("Expected attribute schedule_name not found");
        return obj;
    }

}
