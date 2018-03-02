#-------------------------------------------------------------------------------
# Copyright (c) 2017, Battelle Memorial Institute All rights reserved.
# Battelle Memorial Institute (hereinafter Battelle) hereby grants permission to any person or entity 
# lawfully obtaining a copy of this software and associated documentation files (hereinafter the 
# Software) to redistribute and use the Software in source and binary forms, with or without modification. 
# Such person or entity may use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of 
# the Software, and may permit others to do so, subject to the following conditions:
# Redistributions of source code must retain the above copyright notice, this list of conditions and the 
# following disclaimers.
# Redistributions in binary form must reproduce the above copyright notice, this list of conditions and 
# the following disclaimer in the documentation and/or other materials provided with the distribution.
# Other than as used herein, neither the name Battelle Memorial Institute or Battelle may be used in any 
# form whatsoever without the express written consent of Battelle.
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
# BATTELLE OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, 
# OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
# GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED 
# AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
# NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED 
# OF THE POSSIBILITY OF SUCH DAMAGE.
# General disclaimer for use with OSS licenses
# 
# This material was prepared as an account of work sponsored by an agency of the United States Government. 
# Neither the United States Government nor the United States Department of Energy, nor Battelle, nor any 
# of their employees, nor any jurisdiction or organization that has cooperated in the development of these 
# materials, makes any warranty, express or implied, or assumes any legal liability or responsibility for 
# the accuracy, completeness, or usefulness or any information, apparatus, product, software, or process 
# disclosed, or represents that its use would not infringe privately owned rights.
# 
# Reference herein to any specific commercial product, process, or service by trade name, trademark, manufacturer, 
# or otherwise does not necessarily constitute or imply its endorsement, recommendation, or favoring by the United 
# States Government or any agency thereof, or Battelle Memorial Institute. The views and opinions of authors expressed 
# herein do not necessarily state or reflect those of the United States Government or any agency thereof.
# 
# PACIFIC NORTHWEST NATIONAL LABORATORY operated by BATTELLE for the 
# UNITED STATES DEPARTMENT OF ENERGY under Contract DE-AC05-76RL01830
#-------------------------------------------------------------------------------
# Author:      liuy525
#
# Created:     23/02/2017
# Copyright:   (c) liuy525 2017
# Licence:     <your licence>
#-------------------------------------------------------------------------------

import cmath
import copy
import json
import logging
import math

class VoltVarControl():

    def __init__(self, vvo_static_dict, vvo_message_dict, output_fn):
        """
        VoltVarControl class controls regulators and capacitors from the 
        GridAPPS-D feeder model based off of specific end of line voltage 
        measurements as well as feeder load to make ensure said monitoring 
        points remain within a desired operating range.
        :param vvo_static_dict: a dictionary which contains operating and 
        control information that looks something like the following: 
        {
            'control_method' : 'ACTIVE',# ['ACTIVE','STANDBY']
            'capacitor_delay': 60.0,# Default delay for capacitors
            'regulator_delay': 60.0,# Default delay for regulator
            'desired_pf': 0.99,# Desired power factor for the system
            'd_max': 0.9,# Scaling constant for capacitor switching on - 
            typically 0.3 - 0.6
            'd_min': 0.1,# Scaling constant for capacitor switching off - 
            typically 0.1 - 0.4
            'substation_link': 'xf_hvmv_sub',
            'regulator_list': ['reg_FEEDER_REG','reg_VREG2','reg_VREG3',
            'reg_VREG4'],# List of regulators, separated by commas
            'regulator_configuration_list': ["rcon_FEEDER_REG", "rcon_VREG2", 
            "rcon_VREG3", "rcon_VREG4"],# List of regulator configurations
            'capacitor_list': ['cap_capbank0a', 'cap_capbank0b', 
            'cap_capbank0c', 'cap_capbank1a', 'cap_capbank1b', 'cap_capbank1c',
             'cap_capbank2a', 'cap_capbank2b', 'cap_capbank2c', 'cap_capbank3']
             ,# List of controllable capacitors, separated by commas
            'voltage_measurements': ['l2955047,1','l3160107,1',
            'l2673313,2','l2876814,2','m1047574,3','l3254238,4'],
            # For example, 'L2955047,1' indicates sensor 'L2955047' is communicated with reg.# 1, which is 'FEEDER_REG'
            'maximum_voltages': [7500, 7500, 7500, 7500],# Minimum allowable 
            voltage of the system
            'minimum_voltages': [6500, 6500, 6500, 6500],
            'max_vdrop': [5200, 5200, 5200, 5200],
            'high_load_deadband': [100, 100, 100, 100],
            'desired_voltages': [7000, 7000, 7000, 7000],# List of desired, or 
            target voltages for the volt_var_control object to maintain.
            'low_load_deadband': [100, 100, 100, 100],
            'pf_phase': 'ABC'
        }
        :param vvo_message_dict: The output from the GridAPPS-D simulator. It 
        would look something like this:
        {
            "ieee8500":{
                "cap_capbank0a":{
                    "capacitor_A":400000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":100.0,
                    "phases":"AN",
                    "phases_connected":"NA",
                    "pt_phase":"A",
                    "switchA":"CLOSED"
                },
                "cap_capbank0b":{
                    "capacitor_B":400000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":101.0,
                    "phases":"BN",
                    "phases_connected":"NB",
                    "pt_phase":"B",
                    "switchB":"CLOSED"
                },
                "cap_capbank0c":{
                    "capacitor_C":400000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":102.0,
                    "phases":"CN",
                    "phases_connected":"NC",
                    "pt_phase":"C",
                    "switchC":"CLOSED"
                },
                "cap_capbank1a":{
                    "capacitor_A":300000.0
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":100.0,
                    "phases":"AN",
                    "phases_connected":"NA",
                    "pt_phase":"A",
                    "switchA":"CLOSED"
                },
                "cap_capbank1b":{
                    "capacitor_B":300000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":101.0,
                    "phases":"BN",
                    "phases_connected":"NB",
                    "pt_phase":"B",
                    "switchB":"CLOSED"
                },
                "cap_capbank1c":{
                    "capacitor_C":300000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":102.0,
                    "phases":"CN",
                    "phases_connected":"NC",
                    "pt_phase":"C",
                    "switchC":"CLOSED"
                },
                "cap_capbank2a":{
                    "capacitor_A":300000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":100.0,
                    "phases":"AN",
                    "phases_connected":"NA",
                    "pt_phase":"A",
                    "switchA":"CLOSED"
                },
                "cap_capbank2b":{
                    "capacitor_B":300000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":101.0,
                    "phases":"BN",
                    "phases_connected":"NB",
                    "pt_phase":"B",
                    "switchB":"CLOSED"
                },
                "cap_capbank2c":{
                    "capacitor_C":300000.0,
                    "control":"MANUAL",
                    "control_level":"BANK",
                    "dwell_time":102.0,
                    "phases":"CN",
                    "phases_connected":"NC",
                    "pt_phase":"C",
                    "switchC":"CLOSED"
                },
                "cap_capbank3":{
                    "capacitor_A":300000.0,
                    "capacitor_B":300000.0,
                    "capacitor_C":300000.0,
                    "control":"MANUAL",
                    "control_level":"INDIVIDUAL",
                    "dwell_time":0.0,
                    "phases":"ABCN",
                    "phases_connected":"NCBA",
                    "pt_phase":"",
                    "switchA":"CLOSED",
                    "switchB":"CLOSED",
                    "switchC":"CLOSED"
                },
                "190-7361":{
                    "voltage_A":"6410.387411-4584.456974j V",
                    "voltage_B":"-7198.592139-3270.308372j V",
                    "voltage_C":"642.547265+7539.531175j V"
                },
                "190-8581":{
                    "voltage_A":"6485.244722-4692.686497j V",
                    "voltage_B":"-7183.641237-3170.693324j V",
                    "voltage_C":"544.875720+7443.341013j V"
                },
                "190-8593":{
                    "voltage_A":"6723.279162-5056.725836j V",
                    "voltage_B":"-7494.205738-3101.034602j V",
                    "voltage_C":"630.475857+7534.534977j V"
                },
                "_hvmv_sub_lsb":{
                    "voltage_A":"6261.474438-3926.148203j V",
                    "voltage_B":"-6529.409296-3466.545236j V",
                    "voltage_C":"247.131622+7348.295282j V"
                },
                "l2673313":{
                    "voltage_A":"6569.522312-5003.052614j V",
                    "voltage_B":"-7431.486583-3004.840139j V",
                    "voltage_C":"644.553331+7464.115915j V"
                },
                "l2876814":{
                    "voltage_A":"6593.064915-5014.031801j V",
                    "voltage_B":"-7430.572726-3003.995538j V",
                    "voltage_C":"643.473396+7483.558765j V"
                },
                "l2955047":{
                    "voltage_A":"5850.305846-4217.166594j V",
                    "voltage_B":"-6729.652722-2987.617376j V",
                    "voltage_C":"535.302083+7395.127354j V"
                },
                "l3160107":{
                    "voltage_A":"5954.507575-4227.423005j V",
                    "voltage_B":"-6662.357613-3055.346879j V",
                    "voltage_C":"600.213657+7317.832960j V"
                },
                "l3254238":{
                    "voltage_A":"6271.490549-4631.254028j V",
                    "voltage_B":"-7169.987847-3099.952683j V",
                    "voltage_C":"751.609655+7519.062260j V"
                },
                "m1047574":{
                    "voltage_A":"6306.632406-4741.568924j V",
                    "voltage_B":"-7214.626338-2987.055914j V",
                    "voltage_C":"622.058711+7442.125124j V"
                },
                "rcon_FEEDER_REG":{
                    "Control":"MANUAL",
                    "PT_phase":"CBA",
                    "band_center":126.5,
                    "band_width":2.0,
                    "connect_type":"WYE_WYE",
                    "control_level":"INDIVIDUAL",
                    "dwell_time":15.0,
                    "lower_taps":16,
                    "raise_taps":16,
                    "regulation":0.10000000000000001
                },
                "rcon_VREG2":{
                    "Control":"MANUAL",
                    "PT_phase":"CBA",
                    "band_center":125.0,
                    "band_width":2.0,
                    "connect_type":"WYE_WYE",
                    "control_level":"INDIVIDUAL",
                    "dwell_time":15.0,
                    "lower_taps":16,
                    "raise_taps":16,
                    "regulation":0.10000000000000001
                },
                "rcon_VREG3":{
                    "Control":"MANUAL",
                    "PT_phase":"CBA",
                    "band_center":125.0,
                    "band_width":2.0,
                    "connect_type":"WYE_WYE",
                    "control_level":"INDIVIDUAL",
                    "dwell_time":15.0,
                    "lower_taps":16,
                    "raise_taps":16,
                    "regulation":0.10000000000000001
                },
                "rcon_VREG4":{
                    "Control":"MANUAL",
                    "PT_phase":"CBA",
                    "band_center":125.0,
                    "band_width":2.0,
                    "connect_type":"WYE_WYE",
                    "control_level":"INDIVIDUAL",
                    "dwell_time":15.0,
                    "lower_taps":16,
                    "raise_taps":16,
                    "regulation":0.10000000000000001
                },
                "reg_FEEDER_REG":{
                    "configuration":"rcon_FEEDER_REG",
                    "phases":"ABC",
                    "tap_A":2,
                    "tap_B":2,
                    "tap_C":1,
                    "to":"_hvmv_sub_lsb"
                },
                "reg_VREG2":{
                    "configuration":"rcon_VREG2",
                    "phases":"ABC",
                    "tap_A":10,
                    "tap_B":6,
                    "tap_C":2,
                    "to":"190-8593"
                },
                "reg_VREG3":{
                    "configuration":"rcon_VREG3",
                    "phases":"ABC",
                    "tap_A":16,
                    "tap_B":10,
                    "tap_C":1,
                    "to":"190-8581"
                },
                "reg_VREG4":{
                    "configuration":"rcon_VREG4",
                    "phases":"ABC",
                    "tap_A":12,
                    "tap_B":12,
                    "tap_C":5,
                    "to":"190-7361"
                },
                "xf_hvmv_sub":{
                    "power_in_A":"1739729.121744-774784.928874j VA",
                    "power_in_B":"1659762.622236-785218.729252j VA",
                    "power_in_C":"1709521.679116-849734.584017j VA"
                }
            }
        }
        :param output_fn: a function handle for generating a GridAPPS-D message 
        to send to the simulator. Messages sent out from this class look like:
        {
            "ieee8500": {
                "reg_FEEDER_REG": {
                    "tap_C": -3, 
                    "tap_B": -2, 
                    "tap_A": -1
                }, 
                "reg_VREG4": {
                    "tap_C": 1, 
                    "tap_B": 8, 
                    "tap_A": 8
                }, 
                "reg_VREG2": {
                    "tap_C": -1, 
                    "tap_B": 2, 
                    "tap_A": 6
                }, 
                "reg_VREG3": {
                    "tap_C": -3, 
                    "tap_B": 6, 
                    "tap_A": 12
                },
                "cap_capbank3":{
                    "switchA":"CLOSED",
                    "switchB":"CLOSED",
                    "switchC":"CLOSED"
                },
            }
        }
        """

        ##########################
        ## Initialize variables ##
        ##########################

        # Static Configuration and Dynamic Message
        self.vvc_static = vvo_static_dict
        self.vvc_message = vvo_message_dict
        self.output_fn = output_fn


        # Dict #
        self.vvc = {}
        self.reg_config = {}
        self.reg_tap = {}
        self.cap_config = {}
        self.cap_state = {}
        self.meas_nodes = {} # Initialize voltage measurement inputs in dict - 'sensor_name': [complex_volt_A, complex_volt_B, complex_volt_C]
        self.reg_to_nodes = {} # Initialize regulator to-side voltages
        self.sub_link = {}

        self.output_dict = {}

        self.reg_tap_change_flag = {}

        # Regulator #
        self.reg_list = []  # a sequential list of regulators
        self.reg_config_list = []  # a sequential list of regulator configuration
        self.num_regs = 0  # Number of regulators under our control
        self.reg_step_up = []  # Regulator step for upper taps, associated with num_regs, idx = num_regs
        self.reg_step_down = []  # Regulator step for lower taps, (may be same as reg_step_up), idx = num_regs
        self.reg_update_times = []  # Regulator progression times (differential), relative time, idx = num_regs
##        self.PrevRegState = [] # sequential list of regulators' previous states: #['MANUAL', 'OUTPUT_VOLTAGE', 'REMOTE_NODE', 'LINE_DROP_COMP'], idx = num_regs
        self.regulator_change = True  # regulator change flag
        self.t_update_status = True  # flag for control method switch of the voltage regulator

        # Capacitor #
        self.cap_list = [] # a sequential list of capacitors
        self.num_caps = 0   # Number of capacitors under our control
        self.cap_update_times = [] # Capacitor progression times (differential), relative time, idx = num_cap
##        self.Prevcap_state = [] # sequential list of capacitors' previous states: # ['MANUAL', 'VAR', 'VOLT', 'VARVOLT', 'CURRENT'], idx = num_caps

        # Measurements #
        self.num_meas = []  # Number of voltage measurements to monitor, list num_meas for each reg, idx = num_regs
        self.meas_list = []  # list of measurement sensor names, [[sensor_name list for reg1], [sensor_name list for reg2]] = [[sensor_name1, sensor_name2...], [], [], []...]
        self.meas_phases = []  # list of phase connections for sensors associated with each regulator, 'ABC' 'AC' 'BC' 'C', similar to meas_list, [['ABC', 'BC'...], [], [], []...]

        # Timestamp
        self.ts_never = 9e999  # Infinite timestamp
        self.t_reg_update = []  # Initialize absolute tap change time for each regulator
        self.t_cap_update = 0.0  # Capacitor state update time, Notice: it is a double instead of a list  !!
##        self.prev_time = 0

        # Other
##        self.first_cycle = True  # if it is the first cycle of simulation
##        self.prev_mode = 'ACTIVE'    # 'ACTIVE','STANDBY'
        self.pf_signed = False  # Flag to indicate if a signed pf value should be maintained, or just a "deadband around 1"
##        self.pf_phase = 'ABC'  # Phases for power factor monitoring to occur, could be 'AB', 'BC', 'C'...
        self.react_pwr = 1e4  # Reactive power quantity at the substation
        self.curr_pf = 0.95   # Current pf at the substation
        self.simulation_name = 'sim1'  # simulation identifier
        self.changed_cap = ''  # Name of the capacitor that has changed its state in current time step

        self.log = logging.getLogger('vvo')

        ###############################################
        ## Default Dicts (Configuration and Dynamic) ##
        ###############################################

##        self.vvc = {
##        'control_method' : 'ACTIVE',                   # ['ACTIVE','STANDBY']
##        'capacitor_delay': 60.0,                     # Default delay for capacitors
##        'regulator_delay': 60.0,                    # Default delay for regulator
##        'desired_pf': 0.99,                       # Desired power factor for the system
##        'd_max': 0.9,                              # Scaling constant for capacitor switching on - typically 0.3 - 0.6
##        'd_min': 0.1,                              # Scaling constant for capacitor switching off - typically 0.1 - 0.4
##        'substation_link': 'xf_hvmv_sub',
##        'regulator_list': ['reg_FEEDER_REG','reg_VREG2','reg_VREG3','reg_VREG4'],             # List of regulators, separated by commas
##        'regulator_configuration_list': ["rcon_FEEDER_REG", "rcon_VREG2", "rcon_VREG3", "rcon_VREG4"],       # List of regulator configurations
##        'capacitor_list': ['cap_capbank0a', 'cap_capbank0b', 'cap_capbank0c', 'cap_capbank1a', 'cap_capbank1b', 'cap_capbank1c', 'cap_capbank2a', 'cap_capbank2b', 'cap_capbank2c', 'cap_capbank3'],    # List of controllable capacitors, separated by commas
##        'voltage_measurements': ['l2955047,1','l3160107,1','l2673313,2','l2876814,2','m1047574,3','l3254238,4'],
##        # For example, 'L2955047,1' indicates sensor 'L2955047' is communicated with reg.# 1, which is 'FEEDER_REG'
##        'maximum_voltages': [7500, 7500, 7500, 7500],              # Minimum allowable voltage of the system
##        'minimum_voltages': [6500, 6500, 6500, 6500],
##        'max_vdrop': [5200, 5200, 5200, 5200],
##        'high_load_deadband': [100, 100, 100, 100],
##        'desired_voltages': [7000, 7000, 7000, 7000],             # List of desired, or target voltages for the volt_var_control object to maintain.
##        'low_load_deadband': [100, 100, 100, 100],
##        'pf_phase': 'ABC'
##        }
##
##        self.reg_config = { # in this case, four regulators
##        'connect_type': ['1', '1', '1', '1'],
##        'control': ['MANUAL', 'MANUAL', 'MANUAL', 'MANUAL'],   # 'MANUAL', 'OUTPUT_VOLTAGE', 'REMOTE_NODE', 'LINE_DROP_COMP'
##        'control_level': ['INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL'],   # 'INDIVIDUAL','BANK'
##        'PT_phase' : ['ABC', 'ABC', 'ABC', 'ABC'],
##        'band_center' : [7500, 7500, 7500, 7500], # associated with num_regs, V
##        'band_width' : [120, 120, 120, 120], # V
##        'regulation' : [0.1, 0.1, 0.1, 0.1], # pu, 10%
##        'raise_taps': [16, 16, 16, 16],
##        'lower_taps': [16, 16, 16, 16],
##        'dwell_time' : [60, 60, 60, 60],
##        # The below should belong to regulator properties, but be moved to regulator_configuration properties
##        'phases' : ['ABC', 'ABC', 'ABC', 'ABC'],
##        'to' : ['_hvmv_sub_lsb', '190-8593', '190-8581', '190-7361']
##        }
##
##        self.cap_config = {
##        'phases' : ['AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'ABCN'],
##        'pt_phase' :  ['A', 'B', 'C', 'A', 'B', 'C', 'A', 'B', 'C', 'ABC'],
##        'phases_connected' :  ['AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'AN', 'BN', 'CN', 'ABCN'],
##        'control': ['MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL', 'MANUAL'],   # 'MANUAL', 'VAR', 'VOLT', 'VARVOLT', 'CURRENT'
##        'control_level' : ['INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'INDIVIDUAL', 'BANK'],   # 'INDIVIDUAL','BANK'
##        'cap_size' : [4e5, 3e5, 3e5, 3e5, 4e5, 3e5, 3e5, 3e5, 3e5, 3e5],
##        'dwell_time' : [480, 300, 180, 60, 480, 300, 180, 60, 30, 40]
##        }
##
##
##        self.reg_tap = {
##        'FEEDER_REG': [1, -1, 2], # [TapA, TapB, TapC]
##        'VREG2': [2, -2, 3 ],
##        'VREG3': [3, -3, 4 ],
##        'VREG4': [4, -4, 5 ]
##        }
##
##        self.cap_state = {
##        'cap_capbank0a' : {'switchA': 'OPEN'},
##        'cap_capbank0b' : {'switchB': 'OPEN'},
##        'cap_capbank0c' : {'switchC': 'OPEN'},
##        'cap_capbank1a' : {'switchA': 'OPEN'},
##        'cap_capbank1b' : {'switchB': 'OPEN'},
##        'cap_capbank1c' : {'switchC': 'OPEN'},
##        'cap_capbank2a' : {'switchA': 'OPEN'},
##        'cap_capbank2b' : {'switchB': 'OPEN'},
##        'cap_capbank2c' : {'switchC': 'OPEN'},
##        'cap_capbank3' : {'switchA': 'OPEN', 'switchB': 'OPEN', 'switchC': 'OPEN'}
##        }
##
##        self.sub_link = {
##        'xf_hvmv_sub': [10000 + 10000j, 10000 + 10000j, 10000 + 10000j]  # W + j VAr for phase A, B, C
##        }
##
##        self.meas_nodes = {
##            'l2955047': {'voltage_A': 7000.91 + 0.9j, 'voltage_B': 7031.07 + 0.0j, 'voltage_C': 7011.013 + 0.1j },
##            'l2673313': {'voltage_A': 7010.92 + 0.9j, 'voltage_B': 7031.07 + 0.0j, 'voltage_C': 7041.018 + 0.1j },
##            'l3160107': {'voltage_A': 7020.93 + 0.9j, 'voltage_B': 7021.09 + 0.0j, 'voltage_C': 7021.014 + 0.1j },
##            'l2876814': {'voltage_A': 7000.94 + 0.9j, 'voltage_B': 7001.01 + 0.0j, 'voltage_C': 7011.013 + 0.1j },
##            'l3254238': {'voltage_A': 7011.05 + 0.0j, 'voltage_B': 7011.01 + 0.1j, 'voltage_C': 7011.012 + 0.1j },
##            'm1047574': {'voltage_A': 7021.01 + 0.1j, 'voltage_B': 7021.09 + 0.0j, 'voltage_C': 7021.091 + 0.0j }
##        }  # This is a non-sequential list of measurement inputs
##            # Need to be stored in the same sequence with BasicConfig['voltage_measurements'] to facilitate operation
##
##        self.reg_to_nodes = {
##				'_hvmv_sub_lsb': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ],   # voltage_A, voltage_B, voltage_C     Note: Regulator to-side must have 3-phase voltages
##				'190-8593': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ],
##				'190-8581': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ],
##				'190-7361': [7000.91 + 0.9j, 7031.07 + 0.0j, 7011.013 + 0.1j ]
##        }

        ####################################################
        ## End Default Dicts (Configuration and Dynamic)  ##
        ####################################################



        #######################################
        ## Initialize Configuration dicts ##
        #######################################

        if self.vvc_static.keys() != self.vvc_message.keys():
            raise ValueError('Simulation names mismatch for VVC static configuration and dynamic message!')

        self.simulation_name = self.vvc_static.keys()[0]

        # Extract VVC configuration from static input dictionary
        self.vvc['control_method'] = self.vvc_static[self.simulation_name]['control_method']
        self.vvc['capacitor_delay'] = self.vvc_static[self.simulation_name]['capacitor_delay']
        self.vvc['regulator_delay'] = self.vvc_static[self.simulation_name]['regulator_delay']
        self.vvc['desired_pf'] = self.vvc_static[self.simulation_name]['desired_pf']
        self.vvc['d_max'] = self.vvc_static[self.simulation_name]['d_max']
        self.vvc['d_min'] = self.vvc_static[self.simulation_name]['d_min']
        self.vvc['substation_link'] = self.vvc_static[self.simulation_name]['substation_link']
        self.vvc['regulator_list'] = self.vvc_static[self.simulation_name]['regulator_list']
        self.vvc['regulator_configuration_list'] = self.vvc_static[self.simulation_name]['regulator_configuration_list']
        self.vvc['capacitor_list'] = self.vvc_static[self.simulation_name]['capacitor_list']
        self.vvc['voltage_measurements'] = self.vvc_static[self.simulation_name]['voltage_measurements']
        self.vvc['maximum_voltages'] = self.vvc_static[self.simulation_name]['maximum_voltages']
        self.vvc['minimum_voltages'] = self.vvc_static[self.simulation_name]['minimum_voltages']
        self.vvc['max_vdrop'] = self.vvc_static[self.simulation_name]['max_vdrop']
        self.vvc['high_load_deadband'] = self.vvc_static[self.simulation_name]['high_load_deadband']
        self.vvc['desired_voltages'] = self.vvc_static[self.simulation_name]['desired_voltages']
        self.vvc['low_load_deadband'] = self.vvc_static[self.simulation_name]['low_load_deadband']
        self.vvc['pf_phase'] = self.vvc_static[self.simulation_name]['pf_phase']

        # Extract regulator list, regulator configuration list and capacitor list
        self.reg_list = self.vvc['regulator_list']
        self.reg_config_list = self.vvc['regulator_configuration_list']
        self.cap_list = self.vvc['capacitor_list']

        # Count the number of regulators
        self.num_regs = len(self.vvc['regulator_list'])  # Number of regulators under our control

        # Update VVC configuration based on input dict, make sure inside this python script, the below configuration is related to regulator number
        if type(self.vvc['maximum_voltages']) is not list:
            self.vvc['maximum_voltages'] = [self.vvc['maximum_voltages']] * self.num_regs
        elif type(self.vvc['maximum_voltages']) is list and len(self.vvc['maximum_voltages']) != self.num_regs:
            raise ValueError('vvc configuration inputs do not match the number of regulators')

        if type(self.vvc['minimum_voltages']) is not list:
            self.vvc['minimum_voltages'] = [self.vvc['minimum_voltages']] * self.num_regs
        elif type(self.vvc['minimum_voltages']) is list and len(self.vvc['minimum_voltages']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.vvc['max_vdrop']) is not list:
            self.vvc['max_vdrop'] = [self.vvc['max_vdrop']] * self.num_regs
        elif type(self.vvc['max_vdrop']) is list and len(self.vvc['max_vdrop']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.vvc['high_load_deadband']) is not list:
            self.vvc['high_load_deadband'] = [self.vvc['high_load_deadband']] * self.num_regs
        elif type(self.vvc['high_load_deadband']) is list and len(self.vvc['high_load_deadband']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.vvc['desired_voltages']) is not list:
            self.vvc['desired_voltages'] = [self.vvc['desired_voltages']] * self.num_regs
        elif type(self.vvc['desired_voltages']) is list and len(self.vvc['desired_voltages']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        if type(self.vvc['low_load_deadband']) is not list:
            self.vvc['low_load_deadband'] = [self.vvc['low_load_deadband']] * self.num_regs
        elif type(self.vvc['low_load_deadband']) is list and len(self.vvc['low_load_deadband']) != self.num_regs:
            raise ValueError('VVC configuration inputs do not match the number of regulators')

        # Initialize regulator configuration, should be related to number of regulators
        self.reg_config['connect_type'] = [''] * self.num_regs
        self.reg_config['control'] = [''] * self.num_regs
        self.reg_config['control_level'] = [''] * self.num_regs
        self.reg_config['PT_phase']= [''] * self.num_regs
        self.reg_config['band_center'] = [0] * self.num_regs
        self.reg_config['band_width'] = [0] * self.num_regs
        self.reg_config['regulation'] = [0] * self.num_regs
        self.reg_config['raise_taps']= [0] * self.num_regs
        self.reg_config['lower_taps'] = [0] * self.num_regs
        self.reg_config['dwell_time']= [0] * self.num_regs
        # The below should belong to regulator properties, but be moved to regulator_configuration properties
        self.reg_config['phases'] = [''] * self.num_regs
        self.reg_config['to'] = [''] * self.num_regs
        # Extract and update regulator configuration
        for reg_index in range(self.num_regs):
            self.reg_config['connect_type'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['connect_type']
            self.reg_config['control'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['Control']  # Note the message is using uppercase Control
            self.reg_config['control_level'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['control_level']
            self.reg_config['PT_phase'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['PT_phase']
            self.reg_config['band_center'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['band_center']
            self.reg_config['band_width'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['band_width']
            self.reg_config['regulation'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['regulation']
            self.reg_config['raise_taps'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['raise_taps']
            self.reg_config['lower_taps'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['lower_taps']
            self.reg_config['dwell_time'][reg_index] = self.vvc_message[self.simulation_name][self.reg_config_list[reg_index]]['dwell_time']
            # The below should belong to regulator properties, but be moved to regulator_configuration properties
            self.reg_config['phases'][reg_index] = self.vvc_message[self.simulation_name][self.reg_list[reg_index]]['phases']
            self.reg_config['to'][reg_index] = self.vvc_message[self.simulation_name][self.reg_list[reg_index]]['to']
            # Check validity
            if self.reg_config['control'][reg_index] != 'MANUAL':
                raise ValueError('At least one regulators are not in MANUAL control. Simulation aborted')


        # Count the number of capacitors
        self.num_caps = len(self.vvc['capacitor_list'])   # Number of capacitors under our control

        # Initialize capacitor configuration, should be related to number of capacitors
        self.cap_config['phases'] = [''] * self.num_caps
        self.cap_config['pt_phase'] = [''] * self.num_caps
        self.cap_config['phases_connected'] = [''] * self.num_caps
        self.cap_config['control']= [''] * self.num_caps
        self.cap_config['control_level'] = [''] * self.num_caps
        self.cap_config['cap_size'] = [0] * self.num_caps
        self.cap_config['dwell_time'] = [0] * self.num_caps

        # Extract and update capcacitor configuration
        for cap_index in range(self.num_caps):
            self.cap_config['phases'][cap_index] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['phases']
            self.cap_config['pt_phase'][cap_index] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['pt_phase']
            self.cap_config['phases_connected'][cap_index] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['phases_connected']
            self.cap_config['control'][cap_index] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['control']
            self.cap_config['control_level'][cap_index] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['control_level']
            self.cap_config['dwell_time'][cap_index] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['dwell_time']

            if self.vvc_message[self.simulation_name][self.cap_list[cap_index]].has_key('capacitor_A'):
                self.cap_config['cap_size'][cap_index] = self.cap_config['cap_size'][cap_index] + self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['capacitor_A']

            if self.vvc_message[self.simulation_name][self.cap_list[cap_index]].has_key('capacitor_B'):
                self.cap_config['cap_size'][cap_index] = self.cap_config['cap_size'][cap_index] + self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['capacitor_B']

            if self.vvc_message[self.simulation_name][self.cap_list[cap_index]].has_key('capacitor_C'):
                self.cap_config['cap_size'][cap_index] = self.cap_config['cap_size'][cap_index] + self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['capacitor_C']

                        # Check validity
            if self.cap_config['control'][cap_index] != 'MANUAL':
                raise ValueError('At least one capacitors are not in MANUAL control. Simulation aborted.')

        # sort capacitors based on size (from largest to smallest) - assumes they are banked in operation (only 1 size provided)
        temp_cap_bank_size = copy.deepcopy(self.cap_config['cap_size'])

        max_value = 0
        temp_pos = 0
        temp_value = None
        for i1 in range(len(temp_cap_bank_size)):
            max_value = temp_cap_bank_size[i1]
            for i2 in range(i1, len(temp_cap_bank_size)):
                if temp_cap_bank_size[i2] >= max_value:
                    max_value = temp_cap_bank_size[i2]
                    temp_pos = i2
            temp_value = temp_cap_bank_size[temp_pos]
            temp_cap_bank_size[temp_pos] = temp_cap_bank_size[i1]
            temp_cap_bank_size[i1] = temp_value

            temp_value = self.vvc['capacitor_list'][temp_pos]
            self.vvc['capacitor_list'][temp_pos] = self.vvc['capacitor_list'][i1]
            self.vvc['capacitor_list'][i1] = temp_value

            temp_value = self.cap_config['phases'][temp_pos]
            self.cap_config['phases'][temp_pos] = self.cap_config['phases'][i1]
            self.cap_config['phases'][i1] = temp_value

            temp_value = self.cap_config['pt_phase'][temp_pos]
            self.cap_config['pt_phase'][temp_pos] = self.cap_config['pt_phase'][i1]
            self.cap_config['pt_phase'][i1] = temp_value

            temp_value = self.cap_config['phases_connected'][temp_pos]
            self.cap_config['phases_connected'][temp_pos] = self.cap_config['phases_connected'][i1]
            self.cap_config['phases_connected'][i1] = temp_value

            temp_value = self.cap_config['control'][temp_pos]
            self.cap_config['control'][temp_pos] = self.cap_config['control'][i1]
            self.cap_config['control'][i1] = temp_value

            temp_value = self.cap_config['control_level'][temp_pos]
            self.cap_config['control_level'][temp_pos] = self.cap_config['control_level'][i1]
            self.cap_config['control_level'][i1] = temp_value

            temp_value = self.cap_config['cap_size'][temp_pos]
            self.cap_config['cap_size'][temp_pos] = self.cap_config['cap_size'][i1]
            self.cap_config['cap_size'][i1] = temp_value

            temp_value = self.cap_config['dwell_time'][temp_pos]
            self.cap_config['dwell_time'][temp_pos] = self.cap_config['dwell_time'][i1]
            self.cap_config['dwell_time'][i1] = temp_value

        # Update capacitor list
        self.cap_list = self.vvc['capacitor_list']




    def input(self, vvo_message_dict):
        """
        Updates the internal state of the feeder measurements, regulators, and 
        capacitors being monitored and controlled from output from the 
        GridAPPS-D simulator.
        """
        self.vvc_message = vvo_message_dict



        #######################################
        ## Initialize Dynamic dicts ##
        #######################################


        # Initialize regulator tap dict
        for reg_index in range(self.num_regs):
            self.reg_tap[self.reg_list[reg_index]] = [0] * 3        # 3-phase taps
        # Update regulator taps
        for reg_index in range(self.num_regs):
            self.reg_tap[self.reg_list[reg_index]][0] = self.vvc_message[self.simulation_name][self.reg_list[reg_index]]['tap_A']
            self.reg_tap[self.reg_list[reg_index]][1] = self.vvc_message[self.simulation_name][self.reg_list[reg_index]]['tap_B']
            self.reg_tap[self.reg_list[reg_index]][2] = self.vvc_message[self.simulation_name][self.reg_list[reg_index]]['tap_C']


        # Initialize regulator to-side voltage dict
        for reg_index in range(self.num_regs):
            self.reg_to_nodes[self.reg_config['to'][reg_index]] = [0] * 3     # 3-phase voltages
        # Update regulator to-side voltages
        for reg_index in range(self.num_regs):   # regulator to-side must have 3-phase voltages
            self.reg_to_nodes[self.reg_config['to'][reg_index]][0] = complex( self.vvc_message[self.simulation_name][self.reg_config['to'][reg_index]]['voltage_A'][:-1].replace(' ','') )
            self.reg_to_nodes[self.reg_config['to'][reg_index]][1] = complex( self.vvc_message[self.simulation_name][self.reg_config['to'][reg_index]]['voltage_B'][:-1].replace(' ','') )
            self.reg_to_nodes[self.reg_config['to'][reg_index]][2] = complex( self.vvc_message[self.simulation_name][self.reg_config['to'][reg_index]]['voltage_C'][:-1].replace(' ','') )


        # Initialize capacitor state dict
        for cap_index in range(self.num_caps):
            self.cap_state[self.cap_list[cap_index]] = {}
        # Update capacitor states
        for cap_index in range(self.num_caps):
            if self.vvc_message[self.simulation_name][self.cap_list[cap_index]].has_key('switchA'):
                self.cap_state[self.cap_list[cap_index]]['switchA'] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['switchA']

            if self.vvc_message[self.simulation_name][self.cap_list[cap_index]].has_key('switchB'):
                self.cap_state[self.cap_list[cap_index]]['switchB'] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['switchB']

            if self.vvc_message[self.simulation_name][self.cap_list[cap_index]].has_key('switchC'):
                self.cap_state[self.cap_list[cap_index]]['switchC'] = self.vvc_message[self.simulation_name][self.cap_list[cap_index]]['switchC']


        # Initialize sub_link dict
        self.sub_link[self.vvc['substation_link']] = [0] * 3  # 3-phase power
        # Update substation_link power measurement
        self.sub_link[self.vvc['substation_link']][0] = complex( self.vvc_message[self.simulation_name][self.vvc['substation_link']]['power_in_A'][:-2].replace(' ','') )
        self.sub_link[self.vvc['substation_link']][1] = complex( self.vvc_message[self.simulation_name][self.vvc['substation_link']]['power_in_B'][:-2].replace(' ','') )
        self.sub_link[self.vvc['substation_link']][2] = complex( self.vvc_message[self.simulation_name][self.vvc['substation_link']]['power_in_C'][:-2].replace(' ','') )


        # Extract measurement data
        # Initialize num_meas, meas_list
        self.num_meas = [0] * self.num_regs
        self.meas_list = [[] for k in range(self.num_regs)]  # Very Important !!, How to initialize TWO-Dimention list !!
        # Don't use a=[[]] * 2 !! If used, try a[0].append('sasd') and you will find 'sasd' is also appended to a[1] !

        # Count the number of measurement sensors for each regulator
        # Record list of names of measurement sensors for each regulator
        for sensor_index in range(len(self.vvc['voltage_measurements'])):
            c = self.vvc['voltage_measurements'][sensor_index]
            reg_index = int(c[-1]) - 1
            self.num_meas[reg_index] += 1
            self.meas_list[reg_index].append(c[:-2])
            # Initialize meas_nodes dict
            self.meas_nodes[c[:-2]] = {}

        # Update measurement dict
        for meas_keys in self.meas_nodes.keys():
            self.meas_nodes[meas_keys] = self.vvc_message[self.simulation_name][meas_keys]

        # convert "string" complex number into complex type
        for meas_keys1 in self.meas_nodes.keys():
            for meas_keys2 in self.meas_nodes[meas_keys1].keys():
                if isinstance(self.meas_nodes[meas_keys1][meas_keys2], str):
                    self.meas_nodes[meas_keys1][meas_keys2] = complex( self.meas_nodes[meas_keys1][meas_keys2][:-1].replace(' ','') )

        # print self.meas_nodes
        # Record the connection phases of each measurement sensor in sequential order as BasicConfig['voltage_measurements']
        self.meas_phases = copy.deepcopy(self.meas_list)  # initialize phases connection list to Measurement sensor names list because both are in the same structure
        # Very Important !!  Don't use list_a = list_b to copy list !! Once this is used, if list_a changes, list_b will also change

        for reg_index in range(len(self.meas_phases)):
            for meas_idx in range(len(self.meas_phases[reg_index])):
                if self.meas_nodes.has_key(self.meas_list[reg_index][meas_idx]):  # find specific sensor name in VoltSensor
                    temp_dict = self.meas_nodes[self.meas_list[reg_index][meas_idx]]
                    if len(temp_dict) == 3:  # contain 3-phase measurements
                        self.meas_phases[reg_index][meas_idx] = 'ABC'
                    elif len(temp_dict) == 2: # # contain 2-phase measurements
                        if temp_dict.has_key('voltage_A') and temp_dict.has_key('voltage_B'):
                            self.meas_phases[reg_index][meas_idx] = 'AB'
                        elif temp_dict.has_key('voltage_B') and temp_dict.has_key('voltage_C'):
                            self.meas_phases[reg_index][meas_idx] = 'BC'
                        else:
                            self.meas_phases[reg_index][meas_idx] = 'AC'
                    elif len(temp_dict) == 1: # only contain 1-phase measurement
                        if temp_dict.has_key('voltage_A'):
                            self.meas_phases[reg_index][meas_idx] = 'A'
                        elif temp_dict.has_key('voltage_B'):
                            self.meas_phases[reg_index][meas_idx] = 'B'
                        else:
                            self.meas_phases[reg_index][meas_idx] = 'C'
                    else:
                        raise ValueError('More than 3 phases for this sensor !')

        # example: self.meas_phases = [['ABC', 'ABC'], ['AC', 'AB'], ['C'], ['BC']], corresponding to the four regulators

        # Examine if all dicts are filled
##        print self.vvc
##        print self.reg_config
##        print self.reg_tap
##        print self.cap_config
##        print self.cap_state
##        print self.sub_link
##        print self.meas_nodes
##        print self.reg_to_nodes

        # Calculate volt change per step for raise and lower range
        self.reg_step_up = [0.0] * self.num_regs  # Regulator step for upper taps, associated with num_regs
        self.reg_step_down = [0.0] * self.num_regs  # Regulator step for lower taps, (may be same as reg_step_up)
        for reg_index in range(self.num_regs):
            self.reg_step_up[reg_index] = self.reg_config['band_center'][reg_index] * self.reg_config['regulation'][reg_index] /self.reg_config['raise_taps'][reg_index] # V/tap
            self.reg_step_down[reg_index] = self.reg_config['band_center'][reg_index] * self.reg_config['regulation'][reg_index] /self.reg_config['lower_taps'][reg_index] # V/tap

        # Set voltage regulators and capacitor banks response (progression) time
        self.reg_update_times = self.reg_config['dwell_time']  # Assume the time delays of regulators and capacitors are given, otherwise use default in vvc configuration
        self.cap_update_times = self.cap_config['dwell_time']

        # Initialize regulators tap change times, t_reg_update
        self.t_reg_update = [self.ts_never] * self.num_regs

        # Initialize capacitors tap change times, t_cap_update.  Notice: It is a double instead of a list !!
        self.t_cap_update = 0.0





    def reg_control(self, t0):
        """
        A function that will see what tap changes need to be made to the 
        regulators based off of the internal state of the feeder.
        :param t0: the current wallclock time in seconds.
        :return None.
        """
        # Initialize some local variables
        v_min = [0.0, 0.0, 0.0]   # 3-phase voltages
        v_drop = [0.0, 0.0, 0.0]
        v_set = [0.0, 0.0, 0.0]
        v_reg_to = [0.0, 0.0, 0.0]
        temp_var_u = 0x00
        temp_var_d = 0x00
        prop_tap_changes = [0, 0, 0] # integer, store proposed tap changes of three phases for each regulator
        limit_exceed = 0x00  # U_D - XCBA_XCBA.   0xUD -> U: flag upperbound exceed, D: flag lowerbound exceed
                            # U=1->Ph_A, U=2->Ph_B, U=4->Ph_C, the same for D
        limit_hit = False  # mainly for banked operations
##        treg_min = 0.0  # define a timestamp, Need to ask someone else what this means

        self.regulator_change = False # Start out assuming a regulator change hasn't occurred

        # Initialize regulator tap change flag dict
        for reg_index in range(self.num_regs):
            self.reg_tap_change_flag[self.reg_list[reg_index]] = False

        ###########################################
        ## From here, the core implementation begins
        ###########################################
        if self.vvc['control_method'] == 'ACTIVE':  # turned on

            for reg_index in range(self.num_regs):

                if self.t_reg_update[reg_index] <= t0 or self.t_reg_update[reg_index] == self.ts_never: # see if we're allowed to update, current time t0 exceeds RegUpdate time

                    limit_exceed = 0x00
                    v_min = [1e13] * 3  # initialize v_min to something big, 3-phase
                    v_drop = [0.0] * 3  # initialize v_drop, 3-phase
                    v_set = [self.vvc['desired_voltages'][reg_index]] * 3  # default v_set to where we want, 3-phase
                    prop_tap_changes = [0.0] * 3  # initialize tap changes, 3-phase

                    # Parse through the measurement list - find the lowest voltage
                    for meas_index in range(self.num_meas[reg_index]):
                        temp_dict = self.meas_nodes[self.meas_list[reg_index][meas_index]]

                        if self.meas_phases[reg_index][meas_index].find('A') >= 0:  # Has Phase-A
                            v_min[0] = min(v_min[0], abs(temp_dict['voltage_A']) )  # New Min
                        if self.meas_phases[reg_index][meas_index].find('B') >= 0:
                            v_min[1] = min(v_min[1], abs(temp_dict['voltage_B']) )
                        if self.meas_phases[reg_index][meas_index].find('C') >= 0:
                            v_min[2] = min(v_min[2], abs(temp_dict['voltage_C']) )

                    # May need to check if v_min[i] is still large. May lack measurements for some phases

                    # Populate v_reg_to (to end voltages), Here it is assumed that each regulator has all 3-phases voltages monitored
                    temp_list = self.reg_to_nodes[self.reg_config['to'][reg_index]]

                    if len(temp_list) != 3:
                        raise ValueError('Regulator: ' + self.vvc['regulator_list'][reg_index] + ' is not monitored for all 3 phases !')

                    v_reg_to[0] = abs(temp_list[0])
                    v_reg_to[1] = abs(temp_list[1])
                    v_reg_to[2] = abs(temp_list[2])


                    # Populate v_drop and v_set for Phase-A
                    if self.reg_config['PT_phase'][reg_index].find('A') >= 0: # may not need because we assume all 3-phases voltages are monitored
                        v_drop[0] = v_reg_to[0] - v_min[0] # calculate the drop
                        v_set[0] = self.vvc['desired_voltages'][reg_index] + v_drop[0]  # calculate where we want to be

                        if v_set[0] > self.vvc['maximum_voltages'][reg_index]:
                            print("Warning: " + "for regulator " + self.reg_list[reg_index] + \
                                   ". The set point for phase A will exceed the maximum allowed voltage!")
                            # The set point necessary to maintain the end point voltage exceeds the maximum voltage limit specified by the system.  Either
    						# increase this maximum_voltage limit, or configure your system differently.

                            if self.reg_tap[self.reg_list[reg_index]][0] > 0:  # Tap>0, in raise range
                                if v_reg_to[0] + self.reg_step_up[reg_index] > self.vvc['maximum_voltages'][reg_index]:
                                    limit_exceed |= 0x10 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent
                            else: # self.reg_tap[self.reg_list[reg_index]][0] < 0:  # must be in lower range
                                if v_reg_to[0] + self.reg_step_down[reg_index] > self.vvc['maximum_voltages'][reg_index]:
                                    limit_exceed |= 0x10 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent

                        elif v_set[0] < self.vvc['minimum_voltages'][reg_index]:
                            print("Warning: " + "for regulator " + self.reg_list[reg_index] + \
                                   ". The set point for phase A will exceed the minimum allowed voltage!")

                            if self.reg_tap[self.reg_list[reg_index]][0] > 0:  # Tap>0, in raise range
                                if v_reg_to[0] - self.reg_step_up[reg_index] < self.vvc['minimum_voltages'][reg_index]:
                                    limit_exceed |= 0x01 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent
                            else: # self.reg_tap[self.reg_list[reg_index]][0] < 0:  # must be in lower range
                                if v_reg_to[0] - self.reg_step_down[reg_index] < self.vvc['minimum_voltages'][reg_index]:
                                    limit_exceed |= 0x01 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent



                    # Populate v_drop and v_set for Phase-B
                    if self.reg_config['PT_phase'][reg_index].find('B') >= 0: # may not need because we assume all 3-phases voltages are monitored
                        v_drop[1] = v_reg_to[1] - v_min[1] # calculate the drop
                        v_set[1] = self.vvc['desired_voltages'][reg_index] + v_drop[1]  # calculate where we want to be

                        if v_set[1] > self.vvc['maximum_voltages'][reg_index]:  # exceed upperbound
                            print("Warning: " + "for regulator " + self.reg_list[reg_index] + \
                                   ". The set point for phase B will exceed the maximum allowed voltage!")

                            if self.reg_tap[self.reg_list[reg_index]][1] > 0:  # Tap>0, in raise range
                                if v_reg_to[1] + self.reg_step_up[reg_index] > self.vvc['maximum_voltages'][reg_index]:
                                    limit_exceed |= 0x20 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent
                            else: # self.reg_tap[self.reg_list[reg_index]][1] < 0:  # must be in lower range
                                if v_reg_to[1] + self.reg_step_down[reg_index] > self.vvc['maximum_voltages'][reg_index]:
                                    limit_exceed |= 0x20 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent

                        elif v_set[1] < self.vvc['minimum_voltages'][reg_index]:  # exceed lowerbound
                            print("Warning: " + "for regulator " + self.reg_list[reg_index] + \
                                   ". The set point for phase B will exceed the minimum allowed voltage!")

                            if self.reg_tap[self.reg_list[reg_index]][1] > 0:  # Tap>0, in raise range
                                if v_reg_to[1] - self.reg_step_up[reg_index] < self.vvc['minimum_voltages'][reg_index]:
                                    limit_exceed |= 0x02 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent
                            else: # self.reg_tap[self.reg_list[reg_index]][1] < 0:  # must be in lower range
                                if v_reg_to[1] - self.reg_step_down[reg_index] < self.vvc['minimum_voltages'][reg_index]:
                                    limit_exceed |= 0x02 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent



                    # Populate v_drop and v_set for Phase-C
                    if self.reg_config['PT_phase'][reg_index].find('C') >= 0: # may not need because we assume all 3-phases voltages are monitored
                        v_drop[2] = v_reg_to[2] - v_min[2] # calculate the drop
                        v_set[2] = self.vvc['desired_voltages'][reg_index] + v_drop[2]  # calculate where we want to be

                        if v_set[2] > self.vvc['maximum_voltages'][reg_index]:  # exceed upperbound
                            print("Warning: " + "for regulator " + self.reg_list[reg_index] + \
                                   ". The set point for phase C will exceed the maximum allowed voltage!")

                            if self.reg_tap[self.reg_list[reg_index]][2] > 0:  # Tap>0, in raise range
                                if v_reg_to[2] + self.reg_step_up[reg_index] > self.vvc['maximum_voltages'][reg_index]:
                                    limit_exceed |= 0x40 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent
                            else: # self.reg_tap[self.reg_list[reg_index]][2] < 0:  # must be in lower range
                                if v_reg_to[2] + self.reg_step_down[reg_index] > self.vvc['maximum_voltages'][reg_index]:
                                    limit_exceed |= 0x40 # one more step increase, will exceed upperbound. Tap cannot be raised even though a command is sent

                        elif v_set[2] < self.vvc['minimum_voltages'][reg_index]:  # exceed lowerbound
                            print("Warning: " + "for regulator " + self.reg_list[reg_index] + \
                                   ". The set point for phase C will exceed the minimum allowed voltage!")

                            if self.reg_tap[self.reg_list[reg_index]][2] > 0:  # Tap>0, in raise range
                                if v_reg_to[2] - self.reg_step_up[reg_index] < self.vvc['minimum_voltages'][reg_index]:
                                    limit_exceed |= 0x04 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent
                            else: # self.reg_tap[self.reg_list[reg_index]][2] < 0:  # must be in lower range
                                if v_reg_to[2] - self.reg_step_down[reg_index] < self.vvc['minimum_voltages'][reg_index]:
                                    limit_exceed |= 0x04 # one more step decrease, will exceed Lowerbound. Tap cannot be lowered even though a command is sent



                    # Now determine what kind of regulator we are (right now only 'INDIVIDUAL' is implemented)
                    if self.reg_config['control_level'][reg_index] == 'INDIVIDUAL':
                        # handle phases
                        for phase_index in range(3):  # loop through phases
                            limit_exceed &= 0x7F  # Use bit 8 as a validity flag (to save a variable)
                            if phase_index == 0 and self.reg_config['PT_phase'][reg_index].find('A') >= 0: # We have phase A
     							temp_var_d = 0x01		# A base lower "Limit" checker
    							temp_var_u = 0x10		# A base upper "Limit" checker
    							limit_exceed |= 0x80	# Valid phase
                            if phase_index == 1 and self.reg_config['PT_phase'][reg_index].find('B') >= 0: # We have phase B
     							temp_var_d = 0x02		# B base lower "Limit" checker
    							temp_var_u = 0x20		# B base upper "Limit" checker
    							limit_exceed |= 0x80	# Valid phase
                            if phase_index == 2 and self.reg_config['PT_phase'][reg_index].find('C') >= 0: # We have phase C
     							temp_var_d = 0x04		# C base lower "Limit" checker
    							temp_var_u = 0x40		# C base upper "Limit" checker
    							limit_exceed |= 0x80	# Valid phase

                            if (limit_exceed & 0x80) == 0x80: # valid phase
                                # Make sure we aren't below the minimum or above the maximum first (***** This below here \/ \/ ********) - sub with step check! *****                        # can go down (lower limit is not hit)
                                if ( (v_min[phase_index] > self.vvc['maximum_voltages'][reg_index]) or (v_reg_to[phase_index] > self.vvc['maximum_voltages'][reg_index]) ) and ( (limit_exceed & temp_var_d) != temp_var_d ):
                                    prop_tap_changes[phase_index] = -1 # Flag us for a down tap
                                elif ( (v_min[phase_index] < self.vvc['minimum_voltages'][reg_index]) or (v_reg_to[phase_index] < self.vvc['minimum_voltages'][reg_index]) ) and ( (limit_exceed & temp_var_u) != temp_var_u ):
                                    prop_tap_changes[phase_index] = 1 # Flag us for a up tap
                                else:  # normal operation
                                    # See if we are in high load or low load conditions
                                    if v_drop[phase_index] > self.vvc['max_vdrop'][reg_index]:  # high loading
                                        # See if we're outside our range
                                        if ( (v_set[phase_index] + self.vvc['high_load_deadband'][reg_index]) < v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                            # Above deadband, Need to Tap down.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( v_reg_to[phase_index] - self.reg_step_up[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # more more step decrease (in step_up region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( v_reg_to[phase_index] - self.reg_step_down[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # more more step decrease (in step_down region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down

                                        elif ( (v_set[phase_index] - self.vvc['high_load_deadband'][reg_index]) > v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                            # Below deadband, Need to Tap up.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step increase will get us theoretically
                                                if ( v_reg_to[phase_index] + self.reg_step_up[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # more more step increase (in step_up region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step increase will get us theoretically
                                                if ( v_reg_to[phase_index] + self.reg_step_down[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # more more step increase (in step_down region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up

                                        # else:  (default, inside the deadband, so we don't care)
                                    # Endif   # high load band


                                    else:  # low loading
                                        # See if we're outside our range
                                        if ( (v_set[phase_index] + self.vvc['low_load_deadband'][reg_index]) < v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                            # Above deadband, Need to Tap down.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( v_reg_to[phase_index] - self.reg_step_up[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # more more step decrease (in step_up region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step decrease will get us theoretically
                                                if ( v_reg_to[phase_index] - self.reg_step_down[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # more more step decrease (in step_down region), we will fall below min_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = -1   # try to tap us down

                                        elif ( (v_set[phase_index] - self.vvc['low_load_deadband'][reg_index]) > v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                            # Below deadband, Need to Tap up.
                                            # Check the theoretical change - make sure we won't exceed any limits
                                            if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                                # Find out what a step increase will get us theoretically
                                                if ( v_reg_to[phase_index] + self.reg_step_up[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # more more step increase (in step_up region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up
                                            else:  # must be Lower (step_down) region
                                                # Find out what a step increase will get us theoretically
                                                if ( v_reg_to[phase_index] + self.reg_step_down[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # more more step increase (in step_down region), we will exceed max_volt
                                                    prop_tap_changes[phase_index] = 0   # No change allowed
                                                else:   # change allowed
                                                    prop_tap_changes[phase_index] = 1   # try to tap us up

                                        #else:  (default, inside the deadband, so we don't care)
                                    #Endif   # low load band
                                #Endif  # normal operation
                            #Endif  # valid phase
                        #Endfor  # End phase FOR


    			        #Apply the taps - loop through phases (nonexistant phases should just be 0
    			        #Default assume no change will occur
                        self.regulator_change = False
                        self.t_reg_update[reg_index] = self.ts_never

                        for phase_index in range(3):  # loop through phases
                            if prop_tap_changes[phase_index] > 0:  # want to tap up
                                if self.reg_tap[self.reg_list[reg_index]][phase_index] >= self.reg_config['raise_taps'][reg_index]: # cannot exceed raise taps range
                                    self.reg_tap[self.reg_list[reg_index]][phase_index] = self.reg_config['raise_taps'][reg_index]
                                else:  # must have room to tap up
                                    self.reg_tap[self.reg_list[reg_index]][phase_index] += 1  # increment
                                    self.regulator_change = True  # Flag as change
                                    self.reg_tap_change_flag[self.reg_list[reg_index]] = True  # Flag as change if at least one phase tap changes
                                    self.t_reg_update[reg_index] = t0 + self.reg_update_times[reg_index]  # set return time

                            elif prop_tap_changes[phase_index] < 0:  # want to tap down
                                if self.reg_tap[self.reg_list[reg_index]][phase_index] <= -self.reg_config['lower_taps'][reg_index]: # cannot exceed lower taps range
                                    self.reg_tap[self.reg_list[reg_index]][phase_index] = -self.reg_config['lower_taps'][reg_index]
                                else:  # must have room to tap down
                                    self.reg_tap[self.reg_list[reg_index]][phase_index] -= 1  # decrement
                                    self.regulator_change = True  # Flag as change
                                    self.reg_tap_change_flag[self.reg_list[reg_index]] = True  # Flag as change if at least one phase tap changes
                                    self.t_reg_update[reg_index] = t0 + self.reg_update_times[reg_index]  # set return time

                            #else:  # default else, no change
                        #Endfor   # end phase FOR
                    #Endif    # end individual


                    else:  #  self.reg_config['control_level'][reg_index] == 'BANKED':
                        # Banked will take first PT_PHASE it matches.  If there's more than one, I don't want to know
                        if self.reg_config['PT_phase'][reg_index].find('A') >= 0: # We have phase A
                            phase_index = 0     # Index for A-based voltages
                            temp_var_d = 0x01		# A base lower "Limit" checker
                            temp_var_u = 0x10		# A base upper "Limit" checker

                        elif self.reg_config['PT_phase'][reg_index].find('B') >= 0: # We have phase B
                            phase_index = 1     # Index for B-based voltages
                            temp_var_d = 0x02		# B base lower "Limit" checker
                            temp_var_u = 0x20		# B base upper "Limit" checker

                        else:   # self.reg_config['PT_phase'][reg_index].find('C') >= 0:: # We have phase C
                            phase_index = 2     # Index for C-based voltages
                            temp_var_d = 0x04 	# C base lower "Limit" checker
                            temp_var_u = 0x40		# C base upper "Limit" checker

                        # Make sure we aren't below the minimum or above the maximum first
                        if ( (v_min[phase_index] > self.vvc['maximum_voltages'][reg_index]) or (v_reg_to[phase_index] > self.vvc['maximum_voltages'][reg_index]) ) and ( (limit_exceed & temp_var_d) != temp_var_d ):
                            prop_tap_changes[0] = -1 # Flag us for a down tap
                            prop_tap_changes[1] = -1
                            prop_tap_changes[2] = -1
                        elif ( (v_min[phase_index] < self.vvc['minimum_voltages'][reg_index]) or (v_reg_to[phase_index] < self.vvc['minimum_voltages'][reg_index]) ) and ( (limit_exceed & temp_var_u) != temp_var_u ):
                            prop_tap_changes[0] = 1 # Flag us for a up tap
                            prop_tap_changes[1] = 1
                            prop_tap_changes[2] = 1
                        else:  # normal operation
                            # See if we are in high load or low load conditions

                            if v_drop[phase_index] > self.vvc['max_vdrop'][reg_index]:  # high loading
                                # See if we're outside our range
                                if ( (v_set[phase_index] + self.vvc['high_load_deadband'][reg_index]) < v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                    # Above deadband, Need to Tap down.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( v_reg_to[phase_index] - self.reg_step_up[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # one more step decrease (in step_up region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( v_reg_to[phase_index] - self.reg_step_down[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # one more step decrease (in step_down region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1

                                elif ( (v_set[phase_index] - self.vvc['high_load_deadband'][reg_index]) > v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                    # Below deadband, Need to Tap up.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step increase will get us theoretically
                                        if ( v_reg_to[phase_index] + self.reg_step_up[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # one more step increase (in step_up region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step increase will get us theoretically
                                        if ( v_reg_to[phase_index] + self.reg_step_down[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # one more step increase (in step_down region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1

                                #Else:  (default, inside the deadband, so we don't care)

                            else:    #  low loading
                                # See if we're outside our range
                                if ( (v_set[phase_index] + self.vvc['low_load_deadband'][reg_index]) < v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_d) != temp_var_d ): # Above deadband, but can go down
                                    # Above deadband, Need to Tap down.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( v_reg_to[phase_index] - self.reg_step_up[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # one more step decrease (in step_up region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step decrease will get us theoretically
                                        if ( v_reg_to[phase_index] - self.reg_step_down[reg_index] ) < self.vvc['minimum_voltages'][reg_index]: # one more step decrease (in step_down region), we will fall below min_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = -1   # try to tap us down
                                            prop_tap_changes[1] = -1
                                            prop_tap_changes[2] = -1

                                elif ( (v_set[phase_index] - self.vvc['low_load_deadband'][reg_index]) > v_reg_to[phase_index] ) and ( (limit_exceed & temp_var_u) != temp_var_u ): # Below deadband, but can go up
                                    # Below deadband, Need to Tap up.
                                    # Check the theoretical change - make sure we won't exceed any limits
                                    if self.reg_tap[self.reg_list[reg_index]][phase_index] > 0: # Tap up (or step_up) region
                                        # Find out what a step increase will get us theoretically
                                        if ( v_reg_to[phase_index] + self.reg_step_up[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # more more step increase (in step_up region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1
                                    else:  # must be Lower (step_down) region
                                        # Find out what a step increase will get us theoretically
                                        if ( v_reg_to[phase_index] + self.reg_step_down[reg_index] ) > self.vvc['maximum_voltages'][reg_index]: # more more step increase (in step_down region), we will exceed max_volt
                                            prop_tap_changes[0] = 0   # No change allowed
                                            prop_tap_changes[1] = 0
                                            prop_tap_changes[2] = 0
                                        else:   # change allowed
                                            prop_tap_changes[0] = 1   # try to tap us up
                                            prop_tap_changes[1] = 1
                                            prop_tap_changes[2] = 1

                                #Else:  (default, inside the deadband, so we don't care)
                            #Endif high or low loading band
                        #Endif  # normal operation


                        # Check on the assumption of differential banked (offsets can be present, just all move simultaneously)
                        # We'll only check prop->A, since it is banked
                        self.regulator_change = False
                        self.t_reg_update[reg_index] = self.ts_never

                        if prop_tap_changes[0] > 0:  # want to tap up
                            if self.reg_tap[self.reg_list[reg_index]][0] >= self.reg_config['raise_taps'][reg_index]: # cannot exceed raise taps range
                                self.reg_tap[self.reg_list[reg_index]][0] = self.reg_config['raise_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.reg_tap[self.reg_list[reg_index]][1] >= self.reg_config['raise_taps'][reg_index]: # cannot exceed raise taps range
                                self.reg_tap[self.reg_list[reg_index]][1] = self.reg_config['raise_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.reg_tap[self.reg_list[reg_index]][2] >= self.reg_config['raise_taps'][reg_index]: # cannot exceed raise taps range
                                self.reg_tap[self.reg_list[reg_index]][2] = self.reg_config['raise_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if limit_hit == False:  # we can still proceed
                                self.reg_tap[self.reg_list[reg_index]][0] += 1   # increment them all
                                self.reg_tap[self.reg_list[reg_index]][1] += 1
                                self.reg_tap[self.reg_list[reg_index]][2] += 1
                                self.regulator_change = True   # Flag the change
                                self.reg_tap_change_flag[self.reg_list[reg_index]] = True  # Flag as change if at least one phase tap changes
                                self.t_reg_update[reg_index] = t0 + self.reg_update_times[reg_index]   # set return time
                            #Else   # limit hit, so "no change"


                        elif prop_tap_changes[0] < 0:  # want to tap down
                            # Check individually - set to rail if they are at or exceed - this may lose the offset, but I don't know how they'd ever exceed a limit anyways
                            if self.reg_tap[self.reg_list[reg_index]][0] <= -self.reg_config['lower_taps'][reg_index]: # cannot exceed lower taps range
                                self.reg_tap[self.reg_list[reg_index]][0] = -self.reg_config['lower_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.reg_tap[self.reg_list[reg_index]][1] <= -self.reg_config['lower_taps'][reg_index]: # cannot exceed lower taps range
                                self.reg_tap[self.reg_list[reg_index]][1] = -self.reg_config['lower_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if self.reg_tap[self.reg_list[reg_index]][2] <= -self.reg_config['lower_taps'][reg_index]: # cannot exceed lower taps range
                                self.reg_tap[self.reg_list[reg_index]][2] = -self.reg_config['lower_taps'][reg_index]  # Set at limit
                                limit_hit = True   # Flag that a limit was hit

                            if limit_hit == False:  # we can still proceed
                                self.reg_tap[self.reg_list[reg_index]][0] -= 1   # increment them all
                                self.reg_tap[self.reg_list[reg_index]][1] -= 1
                                self.reg_tap[self.reg_list[reg_index]][2] -= 1
                                self.regulator_change = True   # Flag the change
                                self.reg_tap_change_flag[self.reg_list[reg_index]] = True  # Flag as change if at least one phase tap changes
                                self.t_reg_update[reg_index] = t0 + self.reg_update_times[reg_index]   # set return time
                            #Else   # limit hit, so "no change"

                        #Else  # either want to tap up or tap down, no change requested
                    #Endif  INDIVIDUAL or BANKED mode
                #Endif   allowed to update, t_reg_update[reg_index] < t0
            #Endfor   # End regulator traversion FOR
        #Endif    # End VVC 'ACTIVE' control,  self.BasicConfig['control_method'] == 'ACTIVE'


##        # Find the minimum update first
##        treg_min = self.ts_never
##        for reg_index in range(self.num_regs):
##            if self.t_reg_update[reg_index] < treg_min:
##                treg_min = self.t_reg_update[reg_index]



    def cap_control(self, t0):
        """
        A function that will see what capacitory switch actions need to be made
        to the regulators based off of the internal state of the feeder.
        :param t0: the current wallclock time in seconds.
        :return None.
        """
        # Initialize some local variables
        change_requested = False
        temp_size = 0.0
        link_power_vals = 0.0 + 0.0j

        pf_check = False
        bank_status = 'OPEN'
        self.changed_cap = ''

        # Grab power values and all of those related calculations
        if self.vvc['control_method'] == 'ACTIVE' and self.regulator_change == False:  # no regulator changes in progress and we're active
            link_power_vals = 0.0 + 0.0j  # zero the power

            # Calculate total complex power at the link, assume all three phases are monitored
            link_power_vals += self.sub_link[self.vvc['substation_link']][0]
            link_power_vals += self.sub_link[self.vvc['substation_link']][1]
            link_power_vals += self.sub_link[self.vvc['substation_link']][2]

            # Populate variables of interest
            self.react_pwr = link_power_vals.imag  # pull in reactive power

            if self.pf_signed == False:
                self.curr_pf = abs(link_power_vals.real) / abs(link_power_vals)  # pull in power factor

            # Update proceeding variables
            if self.pf_signed == False:
                if self.curr_pf < self.vvc['desired_pf']:
                    pf_check = True   # Outside the range, make a change
                else:
                    pf_check = False  # Inside the deadband, don't care

            if pf_check == True and self.t_cap_update <= t0:
                change_requested = False  # start out assuming no change

                # Parse through the capacitor list - see where they sit in the categories - break after one switching operation
                for cap_index in range(self.num_caps):
                    # Find the phases being watched, check their switch
                    if self.cap_config['pt_phase'][cap_index].find('A') >= 0:
                        bank_status = self.cap_state[self.cap_list[cap_index]]['switchA']
                    elif self.cap_config['pt_phase'][cap_index].find('B') >= 0:
                        bank_status = self.cap_state[self.cap_list[cap_index]]['switchB']
                    else:  # must be C
                        bank_status = self.cap_state[self.cap_list[cap_index]]['switchC']

                    if self.pf_signed == False:  # Don't consider the sign, just consider it a range
                        # Now perform logic based on where it is
                        if bank_status == 'CLOSED':   # we are on
                            temp_size = self.cap_config['cap_size'][cap_index] * self.vvc['d_min']

                            if self.react_pwr < temp_size:
                                for switch_key in self.cap_state[self.cap_list[cap_index]].keys():
                                    self.cap_state[self.cap_list[cap_index]][switch_key] = 'OPEN'     # Turn all off
                                change_requested = True
                                self.changed_cap = self.cap_list[cap_index]
                                break  # No more loop, only one control per loop

                        else:   # Must be false, so we're off
                            temp_size = self.cap_config['cap_size'][cap_index] * self.vvc['d_max']

                            if self.react_pwr > temp_size:
                                for switch_key in self.cap_state[self.cap_list[cap_index]].keys():
                                    self.cap_state[self.cap_list[cap_index]][switch_key] = 'CLOSED'     # Turn all on
                                change_requested = True
                                self.changed_cap = self.cap_list[cap_index]
                                break  # No more loop, only one control per loop

                    # Endif pf_signed
                # End for cap_index

                if change_requested == True:   # Something changed
                    self.t_cap_update = t0 + self.cap_update_times[cap_index]  # Figure out where we want to go

            # Endif pf_check and self.t_cap_update <= t0


    def output(self):
        """
        Collect all regulator and capacitor control actions and formulate the
        message dictionary to send to the GridAPPS-D simulator and pass that in
        as an argument to the function specified by output_fn.
        :return None. 
        """
        self.output_dict = {}
        self.output_dict[self.simulation_name] = {}

        temp_reg_tap_dict = {}
        temp_cap_switch_dict = {}

        # Update regulator related outputs
        temp_reg_tapKeys = ['tap_A', 'tap_B', 'tap_C']

        for reg_index in range(self.num_regs):
            if self.reg_tap_change_flag[self.reg_list[reg_index]] == True:
               temp_reg_tap_dict[self.reg_list[reg_index]] = dict(zip(temp_reg_tapKeys, self.reg_tap[self.reg_list[reg_index]]))

        self.output_dict[self.simulation_name].update(temp_reg_tap_dict)

        # Update capacitor related outputs
        if self.changed_cap != '':
            temp_cap_switch_dict[self.changed_cap] = self.cap_state[self.changed_cap]
            self.output_dict[self.simulation_name].update(temp_cap_switch_dict)

        self.output_fn(self.output_dict.copy())
