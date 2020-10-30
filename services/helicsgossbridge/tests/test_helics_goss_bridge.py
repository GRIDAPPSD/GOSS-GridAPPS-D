from datetime import datetime
import json
import pathlib
try:
    from Queue import Queue
except:
    from queue import Queue
import time
import unittest.mock
from unittest.mock import call, patch, PropertyMock

from gridappsd import GridAPPSD, utils, topics
import helics
import pytest

from service.helics_goss_bridge import HelicsGossBridge, _main as helics_main

mfile = "{}/model_dict.json".format(pathlib.Path(__file__).parent.absolute())
message_header = {
    "header_1": "Hello",
    "header_2": "There!"
}
message_is_initialized = {
    "command" : "isInitialized"
}
message_update = {
    "command" : "update",
    "input" : {
        "simulation_id" : "123456",
        "message" : {
            "timestamp" : 1357048800,
            "difference_mrid" : "123a456b-789c-012d-345e-678f901a235b",
            "reverse_differences" : [
                {
                    "object" : "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 1
                },
                {
                    "object" : "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 0
                }
            ],
            "forward_differences": [
                {
                    "object" : "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 0
                },
                {
                    "object" : "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA",
                    "attribute" : "ShuntCompensator.sections",
                    "value" : 1
                }
            ]
        }
    }
}
message_start_simulation = {
    "command" : "StartSimulation"
}
message_comm_outage = {
    "command" : "CommOutage",
    "input" : {
        "reverse_differences" : [
            {
                "allInputOutage" : False,
                "inputOutageList" : [],
                "allOutputOutage" : False,
                "outputOutageList" : []
            }
        ],
        "forward_differences" : [
            {
                "allInputOutage" : False,
                "inputOutageList" : [
                    "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA"
                ],
                "allOutputOutage" : False,
                "outputOutageList" : [
                    "61A547FB-9F68-5635-BB4C-F7F537FD824E",
                    "E3CA4CD4-B0D4-9A83-3E2F-18AC5F1B55BA"
                ]
            }
        ]
    }
}
message_stop = {
    "command" : "stop"
}
message_pause = {
    "command" : "pause"
}
message_resume = {
    "command" : "resume"
}
message_resume_pause_at = {
    "command" : "resumePauseAt",
    "input" : {
        "pauseIn" : 20
    }
}
message_junk = {}
goss_message_dict = {
    "simulation_id" : "123",
    "message" : {
        "timestamp" : 1357048800,
        "difference_mrid" : "123a456b-789c-012d-345e-678f901a235b",
        "reverse_differences" : [
            {
                "object" : "_00E2D49D-6F87-4E71-8225-75840868E5C0",
                "attribute" : "Switch.open",
                "value" : 0
            },
            {
                "object" : "_EAC7564D-2126-4138-8525-26B63DB6FDEE",
                "attribute" : "RegulatingControl.mode",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "RegulatingControl.targetDeadband",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "RegulatingControl.targetValue",
                "value" : 0
            },
            {
                "object" : "_EAC7564D-2126-4138-8525-26B63DB6FDEE",
                "attribute" : "ShuntCompensator.aVRDelay",
                "value" : 0
            },
            {
                "object" : "_EAC7564D-2126-4138-8525-26B63DB6FDEE",
                "attribute" : "ShuntCompensator.sections",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.initialDelay",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.step",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.lineDropCompensation",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.lineDropR",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.lineDropX",
                "value" : 0
            },
            {
                "object" : "_6D402486-96FB-4666-A3A3-7C0A180041DC",
                "attribute" : "PowerElectronicsConnection.p",
                "value" : 0
            },
            {
                "object" : "_6D402486-96FB-4666-A3A3-7C0A180041DC",
                "attribute" : "PowerElectronicsConnection.q",
                "value" : 0
            },
            {
                "object" : "_C066F793-B149-4E3E-89E4-A7DD0B7A0DF7",
                "attribute" : "EnergyConsumer.p",
                "value" : 0
            },
            {
                "object" : "_46F2C3DE-8EC9-443F-8F7E-9CC70DB1465A",
                "attribute" : "EnergyConsumer.p",
                "value" : 0
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {}
            }
        ],
        "forward_differences": [
            {
                "object" : "_00E2D49D-6F87-4E71-8225-75840868E5C0",
                "attribute" : "Switch.open",
                "value" : 1
            },
            {
                "object" : "_EAC7564D-2126-4138-8525-26B63DB6FDEE",
                "attribute" : "RegulatingControl.mode",
                "value" : 2
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "RegulatingControl.targetDeadband",
                "value" : 240.0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "RegulatingControl.targetValue",
                "value" : 2400.0
            },
            {
                "object" : "_EAC7564D-2126-4138-8525-26B63DB6FDEE",
                "attribute" : "ShuntCompensator.aVRDelay",
                "value" : 60
            },
            {
                "object" : "_EAC7564D-2126-4138-8525-26B63DB6FDEE",
                "attribute" : "ShuntCompensator.sections",
                "value" : 1
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.initialDelay",
                "value" : 30
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.step",
                "value" : 5
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.lineDropCompensation",
                "value" : 0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.lineDropR",
                "value" : 120.0
            },
            {
                "object" : "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
                "attribute" : "TapChanger.lineDropX",
                "value" : 240.0
            },
            {
                "object" : "_6D402486-96FB-4666-A3A3-7C0A180041DC",
                "attribute" : "PowerElectronicsConnection.p",
                "value" : 4500.0
            },
            {
                "object" : "_6D402486-96FB-4666-A3A3-7C0A180041DC",
                "attribute" : "PowerElectronicsConnection.q",
                "value" : -2400.0
            },
            {
                "object" : "_C066F793-B149-4E3E-89E4-A7DD0B7A0DF7",
                "attribute" : "EnergyConsumer.p",
                "value" : 6000.0
            },
            {
                "object" : "_46F2C3DE-8EC9-443F-8F7E-9CC70DB1465A",
                "attribute" : "EnergyConsumer.p",
                "value" : 6000.0
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "A",
                    "PhaseConnectedFaultKind" : "lineToGround"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "ABC",
                    "PhaseConnectedFaultKind" : "lineToLine"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "AB",
                    "PhaseConnectedFaultKind" : "lineToLine"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "ABC",
                    "PhaseConnectedFaultKind" : "lineToLineToGround"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "AC",
                    "PhaseConnectedFaultKind" : "lineToLineToGround"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "ABC",
                    "PhaseConnectedFaultKind" : "lineOpen"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "BC",
                    "PhaseConnectedFaultKind" : "lineOpen"
                }
            },
            {
                "object" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                "attribute" : "IdentifiedObject.Fault",
                "value" : {
                    "ObjectMRID" : "_EF469BD6-BE77-433A-95A6-2EF18601A0AA",
                    "PhaseCode" : "C",
                    "PhaseConnectedFaultKind" : "lineOpen"
                }
            }
        ]
    }
}
goss_message_converted_dict = {
    "123": {
        "671": {
            "base_power_A": 2000.0,
            "base_power_B": 2000.0,
            "base_power_C": 2000.0
        },
        "cap_cap1": {
            "control": "VAR",
            "dwell_time": 60.0,
            "switchA": "CLOSED",
            "switchB": "CLOSED",
            "switchC": "CLOSED"
        },
        "external_event_handler": {
            "external_fault_event": "[{\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"SLG-A\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"TLL\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"LL-AB\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"TLG\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"DLG-AC\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"OC3\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"OC2-BC\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\", \"fault_object\": \"line_object_name\", \"type\": \"OC-C\"}, {\"name\": \"_EF469BD6-BE77-433A-95A6-2EF18601A0AA\"}]"
        },
        "inv_pv_house": {
            "P_Out": 4500.0,
            "Q_Out": -2400.0
        },
        "ld_house": {
            "base_power_1": 3000.0,
            "base_power_2": 3000.0
        },
        "rcon_Reg": {
            "Control": "MANUAL",
            "band_center": 2400.0,
            "band_width": 240.0,
            "compensator_r_setting_A": 120.0,
            "compensator_x_setting_A": 240.0,
            "dwell_time": 30.0
        },
        "reg_Reg": {
            "tap_A": 5
        },
        "swt_brkr1": {
            "status": "OPEN"
        }
    }
}
object_mrid_to_name = {
    "_00E2D49D-6F87-4E71-8225-75840868E5C0": {
        "name": "brkr1",
        "phases": "ABC",
        "prefix": "sw_",
        "total_phases": "ABC",
        "type": "switch"
    },
    "_2C2316E3-C3FC-4B24-BBDE-1982F4B24138": {
        "name": "Reg",
        "phases": "A",
        "prefix": "reg_",
        "total_phases": "ABC",
        "type": "regulator"
    },
    "_46F2C3DE-8EC9-443F-8F7E-9CC70DB1465A": {
        "name": "671",
        "phases": "ABC",
        "prefix": "ld_",
        "total_phases": "ABC",
        "type": "load"
    },
    "_6D402486-96FB-4666-A3A3-7C0A180041DC": {
        "name": "house",
        "phases": "BS",
        "prefix": "pv_",
        "total_phases": "BS",
        "type": "pv"
    },
    "_6F2BCF22-348B-4AF4-9555-1D7C3B12F9E9": {
        "name": "rec1",
        "phases": "ABC",
        "prefix": "sw_",
        "total_phases": "ABC",
        "type": "recloser"
    },
    "_9575E63E-E682-4BAC-A0CD-D4B9BA86C2A9": {
        "name": "school",
        "phases": "ABC",
        "prefix": "batt_",
        "total_phases": "ABC",
        "type": "battery"
    },
    "_98525341-BE02-428C-B70F-6F78850F7D49": {
        "name": "Reg",
        "phases": "B",
        "prefix": "reg_",
        "total_phases": "ABC",
        "type": "regulator"
    },
    "_A2280E52-4563-4114-B7F5-1BC9D734EC68": {
        "name": "671692",
        "phases": "ABC",
        "prefix": "sw_",
        "total_phases": "ABC",
        "type": "switch"
    },
    "_C066F793-B149-4E3E-89E4-A7DD0B7A0DF7": {
        "name": "house",
        "phases": "s2:s1",
        "prefix": "ld_",
        "total_phases": "s2:s1",
        "type": "triplex_load"
    },
    "_C6C04885-5480-4C12-9FAC-46D852CEDC2B": {
        "name": "Reg",
        "phases": "C",
        "prefix": "reg_",
        "total_phases": "ABC",
        "type": "regulator"
    },
    "_D75B3B35-5B4F-47F9-97C4-1160CABAC482": {
        "name": "house",
        "phases": "BS",
        "prefix": "batt_",
        "total_phases": "BS",
        "type": "battery"
    },
    "_DFF0FD0E-9383-4D6E-8D2C-8BD0809FB5DD": {
        "name": "school",
        "phases": "ABC",
        "prefix": "pv_",
        "total_phases": "ABC",
        "type": "pv"
    },
    "_EAC7564D-2126-4138-8525-26B63DB6FDEE": {
        "name": "cap1",
        "phases": "ABC",
        "prefix": "cap_",
        "total_phases": "ABC",
        "type": "capacitor"
    }
}
object_property_to_measurement_id = {
    "632": [
        {
            "conducting_equipment_type": "Recloser_rec1_Voltage",
            "measurement_mrid": "_c6a13da5-d6a3-49cc-8d50-d334fe878c69",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_Voltage",
            "measurement_mrid": "_b82fbc96-6d1b-44fc-b9ae-6769e3b6a03a",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_Voltage",
            "measurement_mrid": "_294818b5-e17b-4835-b232-26b7a772d6ad",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "634_stmtr": [
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_school",
            "measurement_mrid": "_36ec993a-9ef1-4886-bc9b-03b11b05188c",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_school",
            "measurement_mrid": "_838da845-5315-4f62-867b-ff491b741b19",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_school",
            "measurement_mrid": "_23bdbc36-1c3d-4d1b-a962-d453f03f9b54",
            "phases": "C",
            "property": "voltage_C"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_school",
            "measurement_mrid": "_c5cea1e1-4edd-4bec-8f8b-e5b8849cf50a",
            "phases": "A",
            "property": "measured_power_A"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_school",
            "measurement_mrid": "_76100369-183d-4481-9d65-b863a5abad55",
            "phases": "B",
            "property": "measured_power_B"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_school",
            "measurement_mrid": "_735c610a-cd35-4e4f-a463-a73f1db9a1ca",
            "phases": "C",
            "property": "measured_power_C"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_school",
            "measurement_mrid": "_eb924da8-a6e4-414a-80af-791aff9b5853",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_school",
            "measurement_mrid": "_42b0c2b5-ab55-45f4-a1ca-2d94064ffe4a",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_school",
            "measurement_mrid": "_5ca63cd2-558f-4058-be76-12fde7e27939",
            "phases": "C",
            "property": "voltage_C"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_school",
            "measurement_mrid": "_21452422-7497-4b4d-aba3-8ee933d2bf4d",
            "phases": "A",
            "property": "measured_power_A"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_school",
            "measurement_mrid": "_f3c42dae-1777-45b7-8669-b000c4604bf5",
            "phases": "B",
            "property": "measured_power_B"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_school",
            "measurement_mrid": "_f6302f66-0c47-4728-955e-248555df1db6",
            "phases": "C",
            "property": "measured_power_C"
        }
    ],
    "650": [
        {
            "conducting_equipment_type": "Breaker_brkr1_Voltage",
            "measurement_mrid": "_76fe6bb2-0bc3-41c8-bb5b-8baf6d00795b",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Voltage",
            "measurement_mrid": "_330d046c-dc9d-4680-8501-d21fc234a130",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Voltage",
            "measurement_mrid": "_80503dda-d5c8-4e16-bfd5-02be7a77471e",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "671": [
        {
            "conducting_equipment_type": "EnergyConsumer_671",
            "measurement_mrid": "_1b526731-4c96-43df-b089-c1a4767117c4",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "EnergyConsumer_671",
            "measurement_mrid": "_9bc57ca8-c6f8-4dc8-accf-3c717901891c",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "EnergyConsumer_671",
            "measurement_mrid": "_41c462e7-7700-4e72-b14b-07d54d6cd443",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "680": [
        {
            "conducting_equipment_type": "ACLineSegment_671680_Voltage",
            "measurement_mrid": "_0dcaec8e-d75d-4c1f-a6d7-be4d0b6a0bdb",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "ACLineSegment_671680_Voltage",
            "measurement_mrid": "_91975cba-1d25-4cd7-97f8-d59a1610e8eb",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "ACLineSegment_671680_Voltage",
            "measurement_mrid": "_cb894941-f481-492a-9c13-01ccc2d69989",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "692": [
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_Voltage",
            "measurement_mrid": "_67a0e66c-8e59-4683-af31-b57fb7ac995f",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_Voltage",
            "measurement_mrid": "_d314a1a0-6d5d-45cf-9019-7836323ff514",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_Voltage",
            "measurement_mrid": "_74ce9949-af05-4eac-9218-e5b384fef755",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "brkr": [
        {
            "conducting_equipment_type": "Breaker_brkr1_Voltage",
            "measurement_mrid": "_133429aa-e2a7-4526-92bd-00eb83db8cb5",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Voltage",
            "measurement_mrid": "_f0c27bcd-942c-45bd-91a8-74a6a4f3ecd9",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Voltage",
            "measurement_mrid": "_c38ad942-92d7-4bfe-bb98-e7a0afbfb903",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "cap_cap1": [
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_42b662ed-df46-452a-8730-d5569949b5d0",
            "phases": "A",
            "property": "switchA"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_c03a4c32-d9d5-4eed-b694-481262394955",
            "phases": "B",
            "property": "switchB"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_7d023054-6d55-44f6-a934-7600cf302827",
            "phases": "C",
            "property": "switchC"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_e4ac63f1-ba0d-48f5-ace4-754fb673b292",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_0df0fb2a-6df4-4020-a545-37ca57a6570d",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_ae967130-39a2-4013-ade8-e828072b6cf1",
            "phases": "C",
            "property": "voltage_C"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_c521dd10-16c5-49cf-94ef-aee59f927661",
            "phases": "A",
            "property": "shunt_A"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_f107956f-2f01-44e1-84e9-20f9b49b9761",
            "phases": "B",
            "property": "shunt_B"
        },
        {
            "conducting_equipment_type": "LinearShuntCompensator_cap1",
            "measurement_mrid": "_5652e10f-21ab-43d6-bf66-d8b14f874aff",
            "phases": "C",
            "property": "shunt_C"
        }
    ],
    "house": [
        {
            "conducting_equipment_type": "EnergyConsumer_house",
            "measurement_mrid": "_00bb99d3-741b-4a15-a666-f3f4db3d2b24",
            "phases": "1",
            "property": "voltage_1"
        },
        {
            "conducting_equipment_type": "EnergyConsumer_house",
            "measurement_mrid": "_be29dc1c-9082-416f-b1d4-01a71a2a249d",
            "phases": "2",
            "property": "voltage_2"
        }
    ],
    "house_stmtr": [
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_house",
            "measurement_mrid": "_f8ffd642-379e-487a-8853-a84bfd1cf3dc",
            "phases": "1",
            "property": "voltage_1"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_house",
            "measurement_mrid": "_ff284d40-d818-427f-9497-bdaead163780",
            "phases": "2",
            "property": "voltage_2"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_house",
            "measurement_mrid": "_8aa8e908-53dd-4ba8-b2f0-952d5c0df256",
            "phases": "1",
            "property": "indiv_measured_power_1"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_BatteryUnit_house",
            "measurement_mrid": "_dd8b0cba-b715-4fdf-a22c-0adcd596d5a1",
            "phases": "2",
            "property": "indiv_measured_power_2"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_house",
            "measurement_mrid": "_a64ce2d9-dea7-4ee2-982f-e44cc8f9defb",
            "phases": "1",
            "property": "voltage_1"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_house",
            "measurement_mrid": "_f1e02c9f-cef4-4b91-803e-dc7b85822a5c",
            "phases": "2",
            "property": "voltage_2"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_house",
            "measurement_mrid": "_c9c802ba-a12b-4b48-810a-a002250f1ba8",
            "phases": "1",
            "property": "indiv_measured_power_1"
        },
        {
            "conducting_equipment_type": "PowerElectronicsConnection_PhotovoltaicUnit_house",
            "measurement_mrid": "_e8cf067e-1a5e-4ea9-9d27-5db9d61e4300",
            "phases": "2",
            "property": "indiv_measured_power_2"
        }
    ],
    "ld_671": [
        {
            "conducting_equipment_type": "EnergyConsumer_671",
            "measurement_mrid": "_53cdf9ba-7fa0-4108-b9a9-9e34e65a2e84",
            "phases": "A",
            "property": "measured_power_A"
        },
        {
            "conducting_equipment_type": "EnergyConsumer_671",
            "measurement_mrid": "_7ace1d3c-a108-4101-89ed-f38378b645fa",
            "phases": "B",
            "property": "measured_power_B"
        },
        {
            "conducting_equipment_type": "EnergyConsumer_671",
            "measurement_mrid": "_7ba6f6e9-e0ef-4baa-bd93-9d58e2bb1dda",
            "phases": "C",
            "property": "measured_power_C"
        }
    ],
    "ld_house": [
        {
            "conducting_equipment_type": "EnergyConsumer_house",
            "measurement_mrid": "_fa7bd34c-96f9-45f5-90a1-f82ec65f46e8",
            "phases": "1",
            "property": "indiv_measured_power_1"
        },
        {
            "conducting_equipment_type": "EnergyConsumer_house",
            "measurement_mrid": "_0d19eea3-78e6-45be-aa46-adc249a37baf",
            "phases": "2",
            "property": "indiv_measured_power_2"
        }
    ],
    "line_671680": [
        {
            "conducting_equipment_type": "ACLineSegment_671680_Power",
            "measurement_mrid": "_b811be3c-b0ff-4cd9-afe3-864b223b2a8d",
            "phases": "A",
            "property": "power_in_A"
        },
        {
            "conducting_equipment_type": "ACLineSegment_671680_Power",
            "measurement_mrid": "_a32c6938-4cfc-4c3f-b2ff-57775c68f47d",
            "phases": "B",
            "property": "power_in_B"
        },
        {
            "conducting_equipment_type": "ACLineSegment_671680_Power",
            "measurement_mrid": "_9dc106be-3800-46de-a4e6-9bc21b72936e",
            "phases": "C",
            "property": "power_in_C"
        }
    ],
    "reg_Reg": [
        {
            "conducting_equipment_type": "RatioTapChanger_Reg",
            "measurement_mrid": "_2baaea4b-5f98-4d4f-8ac3-619bb2712ce7",
            "phases": "A",
            "property": "tap_A"
        },
        {
            "conducting_equipment_type": "RatioTapChanger_Reg",
            "measurement_mrid": "_4ebeba65-7ec3-4b72-82e2-87265b433cec",
            "phases": "B",
            "property": "tap_B"
        },
        {
            "conducting_equipment_type": "RatioTapChanger_Reg",
            "measurement_mrid": "_53c17fc1-eac6-44b9-a865-cb19a96e2c3c",
            "phases": "C",
            "property": "tap_C"
        }
    ],
    "sourcebus": [
        {
            "conducting_equipment_type": "PowerTransformer_sub3_Voltage",
            "measurement_mrid": "_0b0ab00e-a509-4b4a-8276-aa954bcd0298",
            "phases": "A",
            "property": "voltage_A"
        },
        {
            "conducting_equipment_type": "PowerTransformer_sub3_Voltage",
            "measurement_mrid": "_060d6afc-88d7-4264-9e15-a9564d0eacb7",
            "phases": "B",
            "property": "voltage_B"
        },
        {
            "conducting_equipment_type": "PowerTransformer_sub3_Voltage",
            "measurement_mrid": "_1b7a1143-e020-4e8d-80fb-a60826f9a606",
            "phases": "C",
            "property": "voltage_C"
        }
    ],
    "swt_671692": [
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_State",
            "measurement_mrid": "_2b907ce5-6294-4919-824f-4f68212a042c",
            "phases": "A",
            "property": "status"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_State",
            "measurement_mrid": "_5a50446d-86f7-4da0-a319-bad0b9549b35",
            "phases": "B",
            "property": "status"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_State",
            "measurement_mrid": "_9a734acc-649b-4d9e-8f27-a5d09623d894",
            "phases": "C",
            "property": "status"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_Current",
            "measurement_mrid": "_b75c0dea-3b18-4252-b671-bbeb273171bf",
            "phases": "A",
            "property": "current_in_A"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_Current",
            "measurement_mrid": "_4a37c515-3ad5-4e02-b694-316f01b99619",
            "phases": "B",
            "property": "current_in_B"
        },
        {
            "conducting_equipment_type": "LoadBreakSwitch_671692_Current",
            "measurement_mrid": "_eb9d8124-ee2d-4279-9889-7497b68cabcd",
            "phases": "C",
            "property": "current_in_C"
        }
    ],
    "swt_brkr1": [
        {
            "conducting_equipment_type": "Breaker_brkr1_State",
            "measurement_mrid": "_b8428048-0930-4f6a-9fcd-f15f78bfe5fa",
            "phases": "A",
            "property": "status"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_State",
            "measurement_mrid": "_da9f657b-ab28-42fc-a139-52718b18ddbd",
            "phases": "B",
            "property": "status"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_State",
            "measurement_mrid": "_07e84d5c-9ed1-4eeb-963e-1d95a6996aca",
            "phases": "C",
            "property": "status"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Current",
            "measurement_mrid": "_e76768da-befb-4bd1-8505-cac74ad96c0b",
            "phases": "A",
            "property": "current_in_A"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Current",
            "measurement_mrid": "_7190a416-c07d-4ac0-8351-082f4daea4d7",
            "phases": "B",
            "property": "current_in_B"
        },
        {
            "conducting_equipment_type": "Breaker_brkr1_Current",
            "measurement_mrid": "_401731bc-1be4-4d20-8737-1a7fcc004697",
            "phases": "C",
            "property": "current_in_C"
        }
    ],
    "swt_rec1": [
        {
            "conducting_equipment_type": "Recloser_rec1_State",
            "measurement_mrid": "_a99c9ecc-24cc-4b6f-934b-77cc2c62be81",
            "phases": "A",
            "property": "status"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_State",
            "measurement_mrid": "_8ace00b3-2f70-4726-8927-eafbd868d0aa",
            "phases": "B",
            "property": "status"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_State",
            "measurement_mrid": "_170ae508-ad8d-4a40-a17d-cc9e10728ff8",
            "phases": "C",
            "property": "status"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_Current",
            "measurement_mrid": "_715a9996-63ef-43e5-b145-0a8825b5c66d",
            "phases": "A",
            "property": "current_in_A"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_Current",
            "measurement_mrid": "_a78eb3e2-1df3-4395-b03a-acd3f738a51c",
            "phases": "B",
            "property": "current_in_B"
        },
        {
            "conducting_equipment_type": "Recloser_rec1_Current",
            "measurement_mrid": "_d6061048-437e-447e-851c-65e1af056ff6",
            "phases": "C",
            "property": "current_in_C"
        }
    ],
    "xf_sub3": [
        {
            "conducting_equipment_type": "PowerTransformer_sub3_Power",
            "measurement_mrid": "_31d1eb6e-5ce1-4df5-bbef-6f667ac50198",
            "phases": "A",
            "property": "power_in_A"
        },
        {
            "conducting_equipment_type": "PowerTransformer_sub3_Power",
            "measurement_mrid": "_f8b09ba0-559f-4443-b643-3effb913ab92",
            "phases": "B",
            "property": "power_in_B"
        },
        {
            "conducting_equipment_type": "PowerTransformer_sub3_Power",
            "measurement_mrid": "_6f13ffe1-c257-43d6-8bc1-7808d67ae676",
            "phases": "C",
            "property": "power_in_C"
        }
    ]
}
helics_simulation_output_message_dict = {
    "123": {
        "632": {
            "voltage_A": "2161.154124-1412.210005j V",
            "voltage_B": "-2191.320094-1142.210158j V",
            "voltage_C": "169.033512+2554.668481j V"
        },
        "634_stmtr": {
            "measured_power_A": "33333.333333-0.000000j VA",
            "measured_power_B": "33333.333333+0.000000j VA",
            "measured_power_C": "33333.333333+0.000000j VA",
            "voltage_A": "245.881558-164.624795j V",
            "voltage_B": "-252.497259-128.737464j V",
            "voltage_C": "21.858528+292.763841j V"
        },
        "650": {
            "voltage_A": "2079.969604-1200.954387j V",
            "voltage_B": "-2080.033392-1200.832131j V",
            "voltage_C": "0.081991+2401.780489j V"
        },
        "671": {
            "voltage_A": "2122.330386-1557.432087j V",
            "voltage_B": "-2182.587284-1125.838464j V",
            "voltage_C": "318.275521+2499.731538j V"
        },
        "680": {
            "voltage_A": "2122.330386-1557.432087j V",
            "voltage_B": "-2182.587284-1125.838464j V",
            "voltage_C": "318.275521+2499.731538j V"
        },
        "692": {
            "voltage_A": "2122.317874-1557.418962j V",
            "voltage_B": "-2182.583043-1125.833901j V",
            "voltage_C": "318.265457+2499.716338j V"
        },
        "brkr": {
            "voltage_A": "2079.950202-1200.914645j V",
            "voltage_B": "-2079.993850-1200.838089j V",
            "voltage_C": "0.051975+2401.740441j V"
        },
        "cap_cap1": {
            "shunt_A": "0.000000+0.034671j S",
            "shunt_B": "0.000000+0.034671j S",
            "shunt_C": "0.000000+0.034671j S",
            "switchA": "CLOSED",
            "switchB": "CLOSED",
            "switchC": "CLOSED",
            "voltage_A": "2104.125779-1569.369263j V",
            "voltage_B": "-2191.332021-1124.563825j V",
            "voltage_C": "322.984545+2493.908725j V"
        },
        "globals": {
            "clock": 1583188956
        },
        "house": {
            "voltage_1": "-105.346298-44.440876j V",
            "voltage_2": "-105.346298-44.440876j V"
        },
        "house_stmtr": {
            "indiv_measured_power_1": "2500.000000-0.000000j VA",
            "indiv_measured_power_2": "2500.000000-0.000000j VA",
            "voltage_1": "-105.346298-44.440876j V",
            "voltage_2": "-105.346298-44.440876j V"
        },
        "ld_671": {
            "measured_power_A": "482864.226406-55250.332069j VA",
            "measured_power_B": "452798.823323-23485.947298j VA",
            "measured_power_C": "461243.793543-60815.599962j VA"
        },
        "ld_house": {
            "indiv_measured_power_1": "1050.155689+650.851324j VA",
            "indiv_measured_power_2": "1050.155689+650.851324j VA"
        },
        "line_671680": {
            "power_in_A": "0.000000-0.000000j VA",
            "power_in_B": "-0.000000-0.000000j VA",
            "power_in_C": "-0.000000-0.000000j VA"
        },
        "reg_Reg": {
            "tap_A": 10,
            "tap_B": 8,
            "tap_C": 11
        },
        "sourcebus": {
            "voltage_A": "66395.280957+0.000000j V",
            "voltage_B": "-33197.640478-57500.000000j V",
            "voltage_C": "-33197.640478+57500.000000j V"
        },
        "swt_671692": {
            "current_in_A": "256.366678-6.127463j A",
            "current_in_B": "3.214123-88.033656j A",
            "current_in_C": "-51.372972+252.636618j A",
            "status": "CLOSED"
        },
        "swt_brkr1": {
            "current_in_A": "591.440664-203.412176j A",
            "current_in_B": "-455.001435-335.841308j A",
            "current_in_C": "-100.322221+700.646041j A",
            "status": "CLOSED"
        },
        "swt_rec1": {
            "current_in_A": "477.057657-137.668179j A",
            "current_in_B": "-186.788314-197.623464j A",
            "current_in_C": "-101.870915+577.406707j A",
            "status": "CLOSED"
        },
        "xf_sub3": {
            "power_in_A": "1451066.303539-183635.053965j VA",
            "power_in_B": "1490616.645685-292701.002299j VA",
            "power_in_C": "1565295.356557-203916.427105j VA"
        }
    }
}
cim_measurement_message = {
    "message": {
        "measurements": {
            "_00bb99d3-741b-4a15-a666-f3f4db3d2b24": {
                "angle": -157.12730138437158,
                "magnitude": 114.33649444491545,
                "measurement_mrid": "_00bb99d3-741b-4a15-a666-f3f4db3d2b24"
            },
            "_060d6afc-88d7-4264-9e15-a9564d0eacb7": {
                "angle": -119.99999999969847,
                "magnitude": 66395.28095660523,
                "measurement_mrid": "_060d6afc-88d7-4264-9e15-a9564d0eacb7"
            },
            "_07e84d5c-9ed1-4eeb-963e-1d95a6996aca": {
                "measurement_mrid": "_07e84d5c-9ed1-4eeb-963e-1d95a6996aca",
                "value": 1
            },
            "_0b0ab00e-a509-4b4a-8276-aa954bcd0298": {
                "angle": 0.0,
                "magnitude": 66395.280957,
                "measurement_mrid": "_0b0ab00e-a509-4b4a-8276-aa954bcd0298"
            },
            "_0d19eea3-78e6-45be-aa46-adc249a37baf": {
                "angle": 31.789248336042018,
                "magnitude": 1235.489545520972,
                "measurement_mrid": "_0d19eea3-78e6-45be-aa46-adc249a37baf"
            },
            "_0dcaec8e-d75d-4c1f-a6d7-be4d0b6a0bdb": {
                "angle": -36.27238032668408,
                "magnitude": 2632.4667088029973,
                "measurement_mrid": "_0dcaec8e-d75d-4c1f-a6d7-be4d0b6a0bdb"
            },
            "_0df0fb2a-6df4-4020-a545-37ca57a6570d": {
                "angle": -152.8336800656223,
                "magnitude": 2463.0427975897164,
                "measurement_mrid": "_0df0fb2a-6df4-4020-a545-37ca57a6570d"
            },
            "_133429aa-e2a7-4526-92bd-00eb83db8cb5": {
                "angle": -30.00113289996849,
                "magnitude": 2401.7470365080744,
                "measurement_mrid": "_133429aa-e2a7-4526-92bd-00eb83db8cb5"
            },
            "_170ae508-ad8d-4a40-a17d-cc9e10728ff8": {
                "measurement_mrid": "_170ae508-ad8d-4a40-a17d-cc9e10728ff8",
                "value": 1
            },
            "_1b526731-4c96-43df-b089-c1a4767117c4": {
                "angle": -36.27238032668408,
                "magnitude": 2632.4667088029973,
                "measurement_mrid": "_1b526731-4c96-43df-b089-c1a4767117c4"
            },
            "_1b7a1143-e020-4e8d-80fb-a60826f9a606": {
                "angle": 119.99999999969847,
                "magnitude": 66395.28095660523,
                "measurement_mrid": "_1b7a1143-e020-4e8d-80fb-a60826f9a606"
            },
            "_21452422-7497-4b4d-aba3-8ee933d2bf4d": {
                "angle": -0.0,
                "magnitude": 33333.333333,
                "measurement_mrid": "_21452422-7497-4b4d-aba3-8ee933d2bf4d"
            },
            "_23bdbc36-1c3d-4d1b-a962-d453f03f9b54": {
                "angle": 85.73006702843762,
                "magnitude": 293.5787149018131,
                "measurement_mrid": "_23bdbc36-1c3d-4d1b-a962-d453f03f9b54"
            },
            "_294818b5-e17b-4835-b232-26b7a772d6ad": {
                "angle": 86.21445585460627,
                "magnitude": 2560.2545529681033,
                "measurement_mrid": "_294818b5-e17b-4835-b232-26b7a772d6ad"
            },
            "_2b907ce5-6294-4919-824f-4f68212a042c": {
                "measurement_mrid": "_2b907ce5-6294-4919-824f-4f68212a042c",
                "value": 1
            },
            "_2baaea4b-5f98-4d4f-8ac3-619bb2712ce7": {
                "measurement_mrid": "_2baaea4b-5f98-4d4f-8ac3-619bb2712ce7",
                "value": 10
            },
            "_31d1eb6e-5ce1-4df5-bbef-6f667ac50198": {
                "angle": -7.212543335785178,
                "magnitude": 1462639.822482304,
                "measurement_mrid": "_31d1eb6e-5ce1-4df5-bbef-6f667ac50198"
            },
            "_330d046c-dc9d-4680-8501-d21fc234a130": {
                "angle": -150.00156408744056,
                "magnitude": 2401.777824586826,
                "measurement_mrid": "_330d046c-dc9d-4680-8501-d21fc234a130"
            },
            "_36ec993a-9ef1-4886-bc9b-03b11b05188c": {
                "angle": -33.80344956325968,
                "magnitude": 295.9038081764062,
                "measurement_mrid": "_36ec993a-9ef1-4886-bc9b-03b11b05188c"
            },
            "_401731bc-1be4-4d20-8737-1a7fcc004697": {
                "angle": 98.14852808098416,
                "magnitude": 707.791934678085,
                "measurement_mrid": "_401731bc-1be4-4d20-8737-1a7fcc004697"
            },
            "_41c462e7-7700-4e72-b14b-07d54d6cd443": {
                "angle": 82.74392122913375,
                "magnitude": 2519.9121153999927,
                "measurement_mrid": "_41c462e7-7700-4e72-b14b-07d54d6cd443"
            },
            "_42b0c2b5-ab55-45f4-a1ca-2d94064ffe4a": {
                "angle": -152.98492646516112,
                "magnitude": 283.42230053343434,
                "measurement_mrid": "_42b0c2b5-ab55-45f4-a1ca-2d94064ffe4a"
            },
            "_42b662ed-df46-452a-8730-d5569949b5d0": {
                "measurement_mrid": "_42b662ed-df46-452a-8730-d5569949b5d0",
                "value": 1
            },
            "_4a37c515-3ad5-4e02-b694-316f01b99619": {
                "angle": -87.90905057825924,
                "magnitude": 88.0923105349466,
                "measurement_mrid": "_4a37c515-3ad5-4e02-b694-316f01b99619"
            },
            "_4ebeba65-7ec3-4b72-82e2-87265b433cec": {
                "measurement_mrid": "_4ebeba65-7ec3-4b72-82e2-87265b433cec",
                "value": 8
            },
            "_53c17fc1-eac6-44b9-a865-cb19a96e2c3c": {
                "measurement_mrid": "_53c17fc1-eac6-44b9-a865-cb19a96e2c3c",
                "value": 11
            },
            "_53cdf9ba-7fa0-4108-b9a9-9e34e65a2e84": {
                "angle": -6.527514421588502,
                "magnitude": 486014.87666160957,
                "measurement_mrid": "_53cdf9ba-7fa0-4108-b9a9-9e34e65a2e84"
            },
            "_5652e10f-21ab-43d6-bf66-d8b14f874aff": {
                "angle": 90.0,
                "magnitude": 0.034671,
                "measurement_mrid": "_5652e10f-21ab-43d6-bf66-d8b14f874aff"
            },
            "_5a50446d-86f7-4da0-a319-bad0b9549b35": {
                "measurement_mrid": "_5a50446d-86f7-4da0-a319-bad0b9549b35",
                "value": 1
            },
            "_5ca63cd2-558f-4058-be76-12fde7e27939": {
                "angle": 85.73006702843762,
                "magnitude": 293.5787149018131,
                "measurement_mrid": "_5ca63cd2-558f-4058-be76-12fde7e27939"
            },
            "_67a0e66c-8e59-4683-af31-b57fb7ac995f": {
                "angle": -36.27231113193084,
                "magnitude": 2632.448856387724,
                "measurement_mrid": "_67a0e66c-8e59-4683-af31-b57fb7ac995f"
            },
            "_6f13ffe1-c257-43d6-8bc1-7808d67ae676": {
                "angle": -7.422318766154656,
                "magnitude": 1578521.923351771,
                "measurement_mrid": "_6f13ffe1-c257-43d6-8bc1-7808d67ae676"
            },
            "_715a9996-63ef-43e5-b145-0a8825b5c66d": {
                "angle": -16.096957971035373,
                "magnitude": 496.52445620684756,
                "measurement_mrid": "_715a9996-63ef-43e5-b145-0a8825b5c66d"
            },
            "_7190a416-c07d-4ac0-8351-082f4daea4d7": {
                "angle": -143.56857816122806,
                "magnitude": 565.5224929312805,
                "measurement_mrid": "_7190a416-c07d-4ac0-8351-082f4daea4d7"
            },
            "_735c610a-cd35-4e4f-a463-a73f1db9a1ca": {
                "angle": 0.0,
                "magnitude": 33333.333333,
                "measurement_mrid": "_735c610a-cd35-4e4f-a463-a73f1db9a1ca"
            },
            "_74ce9949-af05-4eac-9218-e5b384fef755": {
                "angle": 82.74410457363855,
                "magnitude": 2519.89576601564,
                "measurement_mrid": "_74ce9949-af05-4eac-9218-e5b384fef755"
            },
            "_76100369-183d-4481-9d65-b863a5abad55": {
                "angle": 0.0,
                "magnitude": 33333.333333,
                "measurement_mrid": "_76100369-183d-4481-9d65-b863a5abad55"
            },
            "_76fe6bb2-0bc3-41c8-bb5b-8baf6d00795b": {
                "angle": -30.001722509372275,
                "magnitude": 2401.783710748839,
                "measurement_mrid": "_76fe6bb2-0bc3-41c8-bb5b-8baf6d00795b"
            },
            "_7ace1d3c-a108-4101-89ed-f38378b645fa": {
                "angle": -2.9691792232398164,
                "magnitude": 453407.5033820876,
                "measurement_mrid": "_7ace1d3c-a108-4101-89ed-f38378b645fa"
            },
            "_7ba6f6e9-e0ef-4baa-bd93-9d58e2bb1dda": {
                "angle": -7.511196942312625,
                "magnitude": 465235.8265231469,
                "measurement_mrid": "_7ba6f6e9-e0ef-4baa-bd93-9d58e2bb1dda"
            },
            "_7d023054-6d55-44f6-a934-7600cf302827": {
                "measurement_mrid": "_7d023054-6d55-44f6-a934-7600cf302827",
                "value": 1
            },
            "_80503dda-d5c8-4e16-bfd5-02be7a77471e": {
                "angle": 89.99804406011384,
                "magnitude": 2401.780490399488,
                "measurement_mrid": "_80503dda-d5c8-4e16-bfd5-02be7a77471e"
            },
            "_838da845-5315-4f62-867b-ff491b741b19": {
                "angle": -152.98492646516112,
                "magnitude": 283.42230053343434,
                "measurement_mrid": "_838da845-5315-4f62-867b-ff491b741b19"
            },
            "_8aa8e908-53dd-4ba8-b2f0-952d5c0df256": {
                "angle": -0.0,
                "magnitude": 2500.0,
                "measurement_mrid": "_8aa8e908-53dd-4ba8-b2f0-952d5c0df256"
            },
            "_8ace00b3-2f70-4726-8927-eafbd868d0aa": {
                "measurement_mrid": "_8ace00b3-2f70-4726-8927-eafbd868d0aa",
                "value": 1
            },
            "_91975cba-1d25-4cd7-97f8-d59a1610e8eb": {
                "angle": -152.71407343844422,
                "magnitude": 2455.8500563553093,
                "measurement_mrid": "_91975cba-1d25-4cd7-97f8-d59a1610e8eb"
            },
            "_9a734acc-649b-4d9e-8f27-a5d09623d894": {
                "measurement_mrid": "_9a734acc-649b-4d9e-8f27-a5d09623d894",
                "value": 1
            },
            "_9bc57ca8-c6f8-4dc8-accf-3c717901891c": {
                "angle": -152.71407343844422,
                "magnitude": 2455.8500563553093,
                "measurement_mrid": "_9bc57ca8-c6f8-4dc8-accf-3c717901891c"
            },
            "_9dc106be-3800-46de-a4e6-9bc21b72936e": {
                "angle": -180.0,
                "magnitude": 0.0,
                "measurement_mrid": "_9dc106be-3800-46de-a4e6-9bc21b72936e"
            },
            "_a32c6938-4cfc-4c3f-b2ff-57775c68f47d": {
                "angle": -180.0,
                "magnitude": 0.0,
                "measurement_mrid": "_a32c6938-4cfc-4c3f-b2ff-57775c68f47d"
            },
            "_a64ce2d9-dea7-4ee2-982f-e44cc8f9defb": {
                "angle": -157.12730138437158,
                "magnitude": 114.33649444491545,
                "measurement_mrid": "_a64ce2d9-dea7-4ee2-982f-e44cc8f9defb"
            },
            "_a78eb3e2-1df3-4395-b03a-acd3f738a51c": {
                "angle": -133.38547080219908,
                "magnitude": 271.928129788593,
                "measurement_mrid": "_a78eb3e2-1df3-4395-b03a-acd3f738a51c"
            },
            "_a99c9ecc-24cc-4b6f-934b-77cc2c62be81": {
                "measurement_mrid": "_a99c9ecc-24cc-4b6f-934b-77cc2c62be81",
                "value": 1
            },
            "_ae967130-39a2-4013-ade8-e828072b6cf1": {
                "angle": 82.62073351416988,
                "magnitude": 2514.7365160071904,
                "measurement_mrid": "_ae967130-39a2-4013-ade8-e828072b6cf1"
            },
            "_b75c0dea-3b18-4252-b671-bbeb273171bf": {
                "angle": -1.3691753614614928,
                "magnitude": 256.4398943058042,
                "measurement_mrid": "_b75c0dea-3b18-4252-b671-bbeb273171bf"
            },
            "_b811be3c-b0ff-4cd9-afe3-864b223b2a8d": {
                "angle": -0.0,
                "magnitude": 0.0,
                "measurement_mrid": "_b811be3c-b0ff-4cd9-afe3-864b223b2a8d"
            },
            "_b82fbc96-6d1b-44fc-b9ae-6769e3b6a03a": {
                "angle": -152.46953896816945,
                "magnitude": 2471.138968048247,
                "measurement_mrid": "_b82fbc96-6d1b-44fc-b9ae-6769e3b6a03a"
            },
            "_b8428048-0930-4f6a-9fcd-f15f78bfe5fa": {
                "measurement_mrid": "_b8428048-0930-4f6a-9fcd-f15f78bfe5fa",
                "value": 1
            },
            "_be29dc1c-9082-416f-b1d4-01a71a2a249d": {
                "angle": -157.12730138437158,
                "magnitude": 114.33649444491545,
                "measurement_mrid": "_be29dc1c-9082-416f-b1d4-01a71a2a249d"
            },
            "_c03a4c32-d9d5-4eed-b694-481262394955": {
                "measurement_mrid": "_c03a4c32-d9d5-4eed-b694-481262394955",
                "value": 1
            },
            "_c38ad942-92d7-4bfe-bb98-e7a0afbfb903": {
                "angle": 89.99876008743956,
                "magnitude": 2401.740441562384,
                "measurement_mrid": "_c38ad942-92d7-4bfe-bb98-e7a0afbfb903"
            },
            "_c521dd10-16c5-49cf-94ef-aee59f927661": {
                "angle": 90.0,
                "magnitude": 0.034671,
                "measurement_mrid": "_c521dd10-16c5-49cf-94ef-aee59f927661"
            },
            "_c5cea1e1-4edd-4bec-8f8b-e5b8849cf50a": {
                "angle": -0.0,
                "magnitude": 33333.333333,
                "measurement_mrid": "_c5cea1e1-4edd-4bec-8f8b-e5b8849cf50a"
            },
            "_c6a13da5-d6a3-49cc-8d50-d334fe878c69": {
                "angle": -33.16267984471963,
                "magnitude": 2581.651457091818,
                "measurement_mrid": "_c6a13da5-d6a3-49cc-8d50-d334fe878c69"
            },
            "_c9c802ba-a12b-4b48-810a-a002250f1ba8": {
                "angle": -0.0,
                "magnitude": 2500.0,
                "measurement_mrid": "_c9c802ba-a12b-4b48-810a-a002250f1ba8"
            },
            "_cb894941-f481-492a-9c13-01ccc2d69989": {
                "angle": 82.74392122913375,
                "magnitude": 2519.9121153999927,
                "measurement_mrid": "_cb894941-f481-492a-9c13-01ccc2d69989"
            },
            "_d314a1a0-6d5d-45cf-9019-7836323ff514": {
                "angle": -152.71412269044856,
                "magnitude": 2455.844195430976,
                "measurement_mrid": "_d314a1a0-6d5d-45cf-9019-7836323ff514"
            },
            "_d6061048-437e-447e-851c-65e1af056ff6": {
                "angle": 100.0056330327502,
                "magnitude": 586.3243032755175,
                "measurement_mrid": "_d6061048-437e-447e-851c-65e1af056ff6"
            },
            "_da9f657b-ab28-42fc-a139-52718b18ddbd": {
                "measurement_mrid": "_da9f657b-ab28-42fc-a139-52718b18ddbd",
                "value": 1
            },
            "_dd8b0cba-b715-4fdf-a22c-0adcd596d5a1": {
                "angle": -0.0,
                "magnitude": 2500.0,
                "measurement_mrid": "_dd8b0cba-b715-4fdf-a22c-0adcd596d5a1"
            },
            "_e4ac63f1-ba0d-48f5-ace4-754fb673b292": {
                "angle": -36.71754003801835,
                "magnitude": 2624.9314614864747,
                "measurement_mrid": "_e4ac63f1-ba0d-48f5-ace4-754fb673b292"
            },
            "_e76768da-befb-4bd1-8505-cac74ad96c0b": {
                "angle": -18.979456409300695,
                "magnitude": 625.442701114831,
                "measurement_mrid": "_e76768da-befb-4bd1-8505-cac74ad96c0b"
            },
            "_e8cf067e-1a5e-4ea9-9d27-5db9d61e4300": {
                "angle": -0.0,
                "magnitude": 2500.0,
                "measurement_mrid": "_e8cf067e-1a5e-4ea9-9d27-5db9d61e4300"
            },
            "_eb924da8-a6e4-414a-80af-791aff9b5853": {
                "angle": -33.80344956325968,
                "magnitude": 295.9038081764062,
                "measurement_mrid": "_eb924da8-a6e4-414a-80af-791aff9b5853"
            },
            "_eb9d8124-ee2d-4279-9889-7497b68cabcd": {
                "angle": 101.49422251148148,
                "magnitude": 257.80698789325066,
                "measurement_mrid": "_eb9d8124-ee2d-4279-9889-7497b68cabcd"
            },
            "_f0c27bcd-942c-45bd-91a8-74a6a4f3ecd9": {
                "angle": -150.00096936210218,
                "magnitude": 2401.7465586591343,
                "measurement_mrid": "_f0c27bcd-942c-45bd-91a8-74a6a4f3ecd9"
            },
            "_f107956f-2f01-44e1-84e9-20f9b49b9761": {
                "angle": 90.0,
                "magnitude": 0.034671,
                "measurement_mrid": "_f107956f-2f01-44e1-84e9-20f9b49b9761"
            },
            "_f1e02c9f-cef4-4b91-803e-dc7b85822a5c": {
                "angle": -157.12730138437158,
                "magnitude": 114.33649444491545,
                "measurement_mrid": "_f1e02c9f-cef4-4b91-803e-dc7b85822a5c"
            },
            "_f3c42dae-1777-45b7-8669-b000c4604bf5": {
                "angle": 0.0,
                "magnitude": 33333.333333,
                "measurement_mrid": "_f3c42dae-1777-45b7-8669-b000c4604bf5"
            },
            "_f6302f66-0c47-4728-955e-248555df1db6": {
                "angle": 0.0,
                "magnitude": 33333.333333,
                "measurement_mrid": "_f6302f66-0c47-4728-955e-248555df1db6"
            },
            "_f8b09ba0-559f-4443-b643-3effb913ab92": {
                "angle": -11.10938778739608,
                "magnitude": 1519082.5721928484,
                "measurement_mrid": "_f8b09ba0-559f-4443-b643-3effb913ab92"
            },
            "_f8ffd642-379e-487a-8853-a84bfd1cf3dc": {
                "angle": -157.12730138437158,
                "magnitude": 114.33649444491545,
                "measurement_mrid": "_f8ffd642-379e-487a-8853-a84bfd1cf3dc"
            },
            "_fa7bd34c-96f9-45f5-90a1-f82ec65f46e8": {
                "angle": 31.789248336042018,
                "magnitude": 1235.489545520972,
                "measurement_mrid": "_fa7bd34c-96f9-45f5-90a1-f82ec65f46e8"
            },
            "_ff284d40-d818-427f-9497-bdaead163780": {
                "angle": -157.12730138437158,
                "magnitude": 114.33649444491545,
                "measurement_mrid": "_ff284d40-d818-427f-9497-bdaead163780"
            }
        },
        "timestamp": 1583188956
    },
    "simulation_id": "123"
}


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(HelicsGossBridge,'get_is_initialized', return_value=True)
@patch.object(HelicsGossBridge,'get_start_simulation', return_value=True)
@patch.object(HelicsGossBridge,'get_stop_simulation', return_value=False)
@patch.object(HelicsGossBridge,'get_simulation_finished', return_value=False)
@patch.object(HelicsGossBridge,'run_simulation', return_value=None)
def test_helics_goss_bridge_main_function(mock_run_simulation,
        mock_simulation_finished,mock_stop_simulation,mock_start_simulation,
        mock_is_initialized, mock_init):
    helics_main(123,5570,
        {"simulation_config":1,"duration":120,"simulation_start":0})
    mock_init.assert_called_once_with(123,5570,
        {"simulation_config":1,"duration":120,"simulation_start":0})
    mock_is_initialized.assert_called_once()
    mock_start_simulation.assert_called_once()
    mock_stop_simulation.assert_called_once()
    mock_simulation_finished.assert_called_once()
    mock_run_simulation.assert_called_once()

    
@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(HelicsGossBridge,'_create_cim_object_map')
def test_helics_goss_bridge_init(mock_cim_object_map,mock_register_with_helics,
        mock_register_with_goss):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":1,"duration":120,
            "simulation_start":0}})
    assert bridge.get_simulation_id() == 123
    assert bridge.get_broker_port() == 5570
    assert bridge.get_simulation_request() == {
        "simulation_config":{
            "run_realtime":1,
            "duration":120,
            "simulation_start":0
        }
    }
    mock_register_with_goss.assert_called_once()
    mock_register_with_helics.assert_called_once()
    mock_cim_object_map.assert_called_once()
    
    
@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(HelicsGossBridge,'_create_cim_object_map')
def test_helics_goss_bridge_getters(mock_cim_object_map,
        mock_register_with_helics,mock_register_with_goss):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":1,"duration":120,
            "simulation_start":0}})
    assert bridge.get_simulation_id() == 123
    assert bridge.get_broker_port() == 5570
    assert bridge.get_gad_connection() == None
    assert bridge.get_helics_configuration() == {}
    assert bridge.get_helics_federate() == None
    assert bridge.get_is_initialized() == False
    assert bridge.get_simulation_manager_input_topic() == 'goss.gridappsd.fncs.output'
    assert type(bridge.get_simulation_command_queue()) == type(Queue())
    assert bridge.get_simulation_command_queue().empty()
    assert bridge.get_start_simulation() == False
    assert bridge.get_filter_all_commands() == False
    assert bridge.get_filter_all_measurements() == False
    assert bridge.get_command_filter() == []
    assert bridge.get_measurement_filter() == []
    assert bridge.get_stop_simulation() == False
    assert bridge.get_simulation_finished() == False
    assert bridge.get_pause_simulation() == False
    assert bridge.get_simulation_time() == 0
    assert bridge.get_pause_simulation_at() == -1
    assert bridge.get_object_property_to_measurement_id() == None
    assert bridge.get_object_mrid_to_name() == None
    assert bridge.get_model_mrid() == None
    assert bridge.get_difference_attribute_map() == {
        "RegulatingControl.mode" : {
            "capacitor" : {
                "property" : ["control"],
                "prefix" : "cap_"
            }
        },
        "RegulatingControl.targetDeadband" : {
            "capacitor" : {
                "property" : ["voltage_deadband", "VAr_deadband", "current_deadband"],
                "prefix" : "cap_"
            },
            "regulator" : {
                "property" : ["band_width"],
                "prefix" : "rcon_"
            }
        },
        "RegulatingControl.targetValue" : {
            "capacitor" : {
                "property" : ["voltage_center", "VAr_center", "current_center"],
                "prefix" : "cap_"
            },
            "regulator" : {
                "property" : ["band_center"],
                "prefix" : "rcon_"
            }
        },
        "RotatingMachine.p" : {
            "diesel_dg" : {
                "property" : ["real_power_out_{}"],
                "prefix" : "dg_"
            }
        },
        "RotatingMachine.q" : {
            "diesel_dg" : {
                "property" : ["reactive_power_out_{}"],
                "prefix" : "dg_"
            }
        },
        "ShuntCompensator.aVRDelay" : {
            "capacitor" : {
                "property" : ["dwell_time"],
                "prefix" : "cap_"
            }
        },
        "ShuntCompensator.sections" : {
            "capacitor" : {
                "property" : ["switch{}"],
                "prefix" : "cap_"
            }
        },
        "PowerElectronicsConnection.p": {
            "pv": {
                "property": ["P_Out"],
                "prefix": "inv_pv_"
            },
            "battery": {
                "property": ["P_Out"],
                "prefix": "inv_bat_"
            }
        },
        "PowerElectronicsConnection.q": {
            "pv": {
                "property": ["Q_Out"],
                "prefix": "inv_pv_"
            },
            "battery": {
                "property": ["Q_Out"],
                "prefix": "inv_bat_"
            }
        },
        "Switch.open" : {
            "switch" : {
                "property" : ["status"],
                "prefix" : "swt_"
            },
            "recloser" : {
                "property" : ["status"],
                "prefix" : "swt_"
            }
        },
        "TapChanger.initialDelay" : {
            "regulator" : {
                "property" : ["dwell_time"],
                "prefix" : "rcon_"
            }
        },
        "TapChanger.step" : {
            "regulator" : {
                "property" : ["tap_{}"],
                "prefix" : "reg_"
            }
        },
        "TapChanger.lineDropCompensation" : {
            "regulator" : {
                "property" : ["Control"],
                "prefix" : "rcon_"
            }
        },
        "TapChanger.lineDropR" : {
            "regulator" : {
                "property" : ["compensator_r_setting_{}"],
    
                "prefix" : "rcon_"
            }
        },
        "TapChanger.lineDropX" : {
            "regulator" : {
                "property" : ["compensator_x_setting_{}"],
                "prefix" : "rcon_"
            }
        },
        "EnergyConsumer.p" : {
            "triplex_load" : {
                "property" : ["base_power_{}"],
                "prefix" : "ld_"
            },
            "load" : {
                "property" : ["base_power_{}"],
                "prefix" : ""
            }
        }
    }

    
@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(helics,'helicsFederateGetState', return_value=1)
@patch.object(HelicsGossBridge,'_gad_connection')
@unittest.mock.patch('service.helics_goss_bridge.datetime')
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_message(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_datetime,mock_gad_connection,
        mock_helicsFederateGetState,mock_init):
    mock_datetime.utcnow.return_value = datetime(2017,8,25,10,33,6,150642)
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":1,"duration":120,
            "simulation_start":0}})
    #test initialize messages
    bridge.on_message(message_header, message_is_initialized)
    mock_helicsFederateGetState.assert_called_once()
    message_dict = {
        'received message': {
            'header': message_header,
            'message_content': message_is_initialized
        }
    }
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("STARTED",
        message_str,"DEBUG")
    message_str = 'isInitialized check: '+str(False)
    mock_gad_connection.send_simulation_status.called_with("STARTED",
        message_str,"DEBUG")
    message={}
    message['command'] = 'isInitialized'
    message['response'] = str(False)
    t_now = mock_datetime.utcnow()
    message['timestamp'] = int(time.mktime(t_now.timetuple()))
    mock_gad_connection.send.assert_called_once_with(
        bridge.get_simulation_manager_input_topic(),json.dumps(message))
    
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateGetState.return_value = 2
    bridge.on_message(message_header, message_is_initialized)
    mock_helicsFederateGetState.assert_called_once()
    message_dict = {
        'received message': {
            'header': message_header,
            'message_content': message_is_initialized
        }
    }
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    message_str = 'isInitialized check: '+str(True)
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    message={}
    message['command'] = 'isInitialized'
    message['response'] = str(True)
    t_now = mock_datetime.utcnow()
    message['timestamp'] = int(time.mktime(t_now.timetuple()))
    mock_gad_connection.send.assert_called_once_with(
        bridge.get_simulation_manager_input_topic(),json.dumps(message))
    #test update message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_update)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_update
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 1
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    q = bridge.get_simulation_command_queue()
    assert q.qsize() == 1
    command = q.get()
    assert command == json.dumps(message_update["input"])
    #test start simulation message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_start_simulation)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_start_simulation
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 1
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert bridge.get_start_simulation()
    #test comm outage message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_comm_outage)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_comm_outage
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 1
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    command_filter = bridge.get_command_filter()
    assert command_filter == message_comm_outage["input"]["forward_differences"][0]["inputOutageList"]
    measurement_filter = bridge.get_measurement_filter()
    assert measurement_filter == message_comm_outage["input"]["forward_differences"][0]["outputOutageList"]
    assert not bridge.get_filter_all_commands()
    assert not bridge.get_filter_all_measurements()
    #test stop message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_stop)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_stop
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    message_str = "Stopping the simulation"
    mock_gad_connection.send_simulation_status.called_with("CLOSED",
        message_str,"INFO")
    assert not bridge.get_simulation_finished()
    mock_helicsFederateGlobalError.called_once_with(
        bridge.get_helics_federate(), 1, 
        "Stopping the simulation prematurely at operator's request!")
    mock_close_helics_connection.assert_called_once()
    #test pause message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateGlobalError.reset_mock()
    mock_close_helics_connection.reset_mock()
    bridge.on_message(message_header, message_pause)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_pause
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert bridge.get_pause_simulation()
    mock_gad_connection.send_simulation_status.called_with('PAUSED', 
        'The simulation has paused.', 'INFO')
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_pause)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with('PAUSED', 
        'Pause command received but the simulation is already paused.', 'WARN')
    #test resume message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_resume
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert not bridge.get_pause_simulation()
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "The simulation has resumed.","INFO")
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "Resume command received but the simulation is already running.",
        "INFO")
    #test resume pause at message
    bridge.on_message(message_header, message_pause)
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume_pause_at)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_resume_pause_at
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    assert not bridge.get_pause_simulation()
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "The simulation has resumed.","INFO")
    assert bridge.get_pause_simulation_at() == bridge.get_simulation_time() + 20
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_resume_pause_at)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        "The resumePauseAt command was received but the simulation is already running.",
        "WARN")
    #test incorrect json schema message
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    bridge.on_message(message_header, message_junk)
    mock_helicsFederateGetState.assert_called_once()
    message_dict['message_content'] = message_junk
    message_str = json.dumps(message_dict, indent=4, sort_keys=True)
    assert mock_gad_connection.send_simulation_status.call_count == 2
    mock_gad_connection.send_simulation_status.called_with("RUNNING",
        message_str,"DEBUG")
    mock_gad_connection.send_simulation_status.called_with("WARNING",
        "The message received did not have a command key. Ignoring malformed message.",
        "WARN")
        
    
@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(HelicsGossBridge,'_gad_connection')
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_error(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_gad_connection,mock_init):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":1,"duration":120,
            "simulation_start":0}})
    bridge.on_error(message_header, message_is_initialized)
    message_str = 'Error in HelicsGossBridge: '+str(message_is_initialized)
    mock_gad_connection.send_simulation_status.assert_called_once_with('ERROR',
        message_str, 'ERROR')
    assert bridge.get_stop_simulation()
    mock_helicsFederateGlobalError.assert_called_once_with(
        bridge.get_helics_federate(),1,message_str)
    mock_close_helics_connection.assert_called_once()


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_on_disconnected(mock_close_helics_connection,
        mock_helicsFederateGlobalError,mock_init):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":1,"duration":120,
            "simulation_start":0}})
    bridge.on_disconnected()
    message_str = 'HelicsGossBridge instance lost connection to GOSS bus.'
    assert bridge.get_stop_simulation()
    mock_helicsFederateGlobalError.assert_called_once_with(
        bridge.get_helics_federate(),1,message_str)
    mock_close_helics_connection.assert_called_once()


@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(HelicsGossBridge,'_create_cim_object_map')
@patch.object(helics,'helicsFederateGetState', return_value=2)
@patch.object(helics,'helicsFederateGlobalError')
@patch.object(HelicsGossBridge,'_gad_connection')
@patch.object(HelicsGossBridge,'_get_helics_bus_messages',
    return_value=message_update)
@unittest.mock.patch('service.helics_goss_bridge.time')
@patch.object(HelicsGossBridge,'_publish_to_helics_bus')
@patch.object(HelicsGossBridge,'_done_with_time_step')
@patch.object(helics,'helicsFederateFinalize')
@patch.object(HelicsGossBridge,'_close_helics_connection')
def test_helics_goss_bridge_run_simulation(mock_close_helics_connection,
    mock_helicsFederateFinalize,mock_done_with_time_step,
    mock_publish_to_helics_bus,mock_time,mock_get_helics_bus_messages,
    mock_gad_connection,mock_helicsFederateGlobalError,
    mock_helicsFederateGetState,mock_create_cim_object_map,
    mock_register_with_helics,mock_register_with_goss):
    #test when _stop_simulation == true
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":120,
            "simulation_start":0}})
    bridge.on_message(message_header, message_stop)
    assert bridge.get_stop_simulation
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateGlobalError.reset_mock()
    mock_close_helics_connection.reset_mock()
    bridge.run_simulation()
    assert mock_helicsFederateGetState.call_count == 2
    assert mock_helicsFederateGlobalError.call_count == 2
    mock_helicsFederateGlobalError.assert_called_with(
        bridge.get_helics_federate(),1,
        "Stopping the simulation prematurely at operator's request!")
    assert mock_gad_connection.send.call_count == 3
    send_calls = [
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 0})
        ),
        call(
            topics.simulation_output_topic(bridge.get_simulation_id()),
            json.dumps(message_update,indent=4,sort_keys=True)
        ),
        call(
            bridge.get_simulation_manager_input_topic(),
            json.dumps({'command':'simulationFinished'})
        )
    ]
    mock_gad_connection.send.assert_has_calls(send_calls)
    mock_helicsFederateFinalize.assert_called_once_with(
        bridge.get_helics_federate())
    mock_close_helics_connection.assert_called_once()
    mock_gad_connection.send_simulation_status.assert_called_once_with(
        'COMPLETE',
        'Simulation {} has finished.'.format(bridge.get_simulation_id()),
        'INFO')
    #test regular timestep functionality
    bridge2 = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge2.on_message(message_header, message_update)
    mock_helicsFederateGetState.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateGlobalError.reset_mock()
    mock_close_helics_connection.reset_mock()
    mock_helicsFederateFinalize.reset_mock()
    bridge2.run_simulation()
    assert mock_helicsFederateGetState.call_count == 6
    assert bridge2.get_simulation_time() == 5
    assert mock_helicsFederateGlobalError.call_count == 0
    assert mock_gad_connection.send.call_count == 13
    send_calls = [
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 0})
        ),
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 1})
        ),
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 2})
        ),
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 3})
        ),
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 4})
        ),
        call(
            "goss.gridappsd.fncs.timestamp.{}".format(bridge.get_simulation_id()),
            json.dumps({"timestamp": 5})
        ),
        call(
            topics.simulation_output_topic(bridge.get_simulation_id()),
            json.dumps(message_update,indent=4,sort_keys=True)
        ),
        call(
            bridge.get_simulation_manager_input_topic(),
            json.dumps({'command':'simulationFinished'})
        )
    ]
    mock_gad_connection.send.assert_has_calls(send_calls, any_order=True)
    assert mock_gad_connection.send_simulation_status.call_count == 6
    status_calls = [
        call('RUNNING', 'incrementing to {}'.format(1),'DEBUG'),
        call('RUNNING', 'incrementing to {}'.format(2),'DEBUG'),
        call('RUNNING', 'incrementing to {}'.format(3),'DEBUG'),
        call('RUNNING', 'incrementing to {}'.format(4),'DEBUG'),
        call('RUNNING', 'incrementing to {}'.format(5),'DEBUG'),
        call('COMPLETE',
            'Simulation {} has finished.'.format(bridge.get_simulation_id()),
            'INFO')
    ]
    mock_gad_connection.send_simulation_status.assert_has_calls(status_calls)
    mock_publish_to_helics_bus.assert_called_once_with(
        json.dumps(message_update["input"]),bridge2.get_command_filter())
    assert mock_done_with_time_step.call_count == 5
    mock_helicsFederateFinalize.assert_called_once_with(
        bridge2.get_helics_federate())
    mock_close_helics_connection.assert_called_once()
    assert bridge2.get_simulation_finished()


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@unittest.mock.patch('service.helics_goss_bridge.GridAPPSD')
def test_helics_goss_bridge_register_with_goss(mock_GridAPPSD,mock_init):
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge._register_with_goss()
    mock_GridAPPSD.assert_called_once_with(bridge.get_simulation_id(), address=utils.get_gridappsd_address(),
                username=utils.get_gridappsd_user(), password=utils.get_gridappsd_pass())
    assert bridge._gad_connection.subscribe.call_count == 2
    subscribe_calls = [
        call(topics.simulation_input_topic("123"), bridge.on_message)]
    bridge._gad_connection.subscribe.assert_has_calls()
    #assert bridge.get_gad_connection() == 23
    
    
@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_create_cim_object_map')
@unittest.mock.patch('service.helics_goss_bridge.helics')
def test_helics_goss_bridge_register_with_helics(mock_helics,
    mock_create_cim_object_map,mock_register_with_goss):
    mock_helics.helicsCreateMessageFederateFromConfig.return_value = 'helics_federate'
    #HelicsGossBridge.__init__() calls _register_with_helics()
    bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    mock_helics.helicsCreateMessageFederateFromConfig.assert_called_once_with(
        json.dumps(bridge.get_helics_configuration()))
    assert bridge.get_helics_federate() == 'helics_federate'
    mock_helics.helicsFederateEnterExecutingMode.assert_called_once_with(
        'helics_federate')


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@unittest.mock.patch('service.helics_goss_bridge.helics')
def test_helics_goss_bridge_close_helics_connection(mock_helics,mock_init):
    bridge = bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge._close_helics_connection()
    mock_helics.helicsFederateFree.assert_called_once_with(
        bridge.get_helics_federate())
    mock_helics.helicsCloseLibrary.assert_called_once()


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch('service.helics_goss_bridge.HelicsGossBridge._object_mrid_to_name',
    new_callable=PropertyMock)
@patch.object(HelicsGossBridge,'_gad_connection')
def test_helics_goss_bridge_get_gld_object_name(mock_gad_connection,
        mock_object_mrid_to_name,mock_init):
    #test when _object_mrid_to_name.get(object_mrid) == None
    mock_object_mrid_to_name.return_value = {}
    #test when object type is "LinearShuntCompesator"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"LinearShuntCompensator"
            }
        ]
    }
    bridge = bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    assert bridge.get_object_mrid_to_name() == {}
    assert bridge._get_gld_object_name('123') == "cap_object_name"
    mock_gad_connection.query_object_dictionary.assert_called_once_with(
        model_id=bridge.get_model_mrid(),object_id='123')
    #test when object type is "PowerTransformer"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"PowerTransformer"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "xf_object_name"
    #test when object type is "ACLineSegment"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"ACLineSegment"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "line_object_name"
    #test when object type is "LoadBreakSwitch"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"LoadBreakSwitch"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "sw_object_name"
    #test when object type is "Recloser"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"Recloser"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "sw_object_name"
    #test when object type is "Breaker"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"Breaker"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "sw_object_name"
    #test when object type is "RatioTapChanger"
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"RatioTapChanger"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "reg_object_name"
    #test when object type is not a valid type
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"RandomType"
            }
        ]
    }
    assert bridge._get_gld_object_name('123') == "object_name"
    #test when _object_mrid_to_name.get(object_mrid) != None
    mock_gad_connection.reset_mock()
    mock_object_mrid_to_name.return_value = {
        "123": {
            "name":"object_name",
            "prefix":"random_prefix_"
        }
    }
    assert bridge._get_gld_object_name('123') == "random_prefix_object_name"
    mock_gad_connection.query_object_dictionary.assert_not_called()


@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(HelicsGossBridge,'_gad_connection')
@patch.object(helics,'helicsFederateGetState', return_value=1)
@patch.object(helics,'helicsFederateGetEndpoint', return_value="helics_input_enpoint")
@patch.object(helics,'helicsFederateCreateMessageObject', return_value=None)
@patch.object(helics,'helicsMessageSetString')
@patch.object(helics,'helicsEndpointSendMessageObject')
def test_helics_goss_bridge_publish_to_helics_bus(
        mock_helicsEndpointSendMessageObject,mock_helicsMessageSetString,
        mock_helicsFederateCreateMessageObject,mock_helicsFederateGetEndpoint,
        mock_helicsFederateGetState,mock_gad_connection,
        mock_register_with_helics,register_with_goss):
    #test simulation_id is malformed
    bridge1 = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge1._create_cim_object_map(map_file=mfile)
    with pytest.raises(ValueError) as e_info:
        bridge1._publish_to_helics_bus("",[])
    assert str(e_info.value) == "simulation_id must be a nonempty string.\nsimulation_id = 123.\nsimulation_id type = <class 'int'>."
    #test goss message is malformed
    bridge2 = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge2._create_cim_object_map(map_file=mfile)
    with pytest.raises(ValueError) as e_info:
        bridge2._publish_to_helics_bus(1235,[])
    assert str(e_info.value) == "goss_message must be a nonempty string.\ngoss_message = 1235.\ngoss_message type = <class 'int'>."
    #test helics state is not in execution mode
    bridge3 = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge3._create_cim_object_map(map_file=mfile)
    with pytest.raises(RuntimeError) as e_info:
        bridge3._publish_to_helics_bus("hello",[])
    assert str(e_info.value) == "Cannot publish message as there is no connection to the HELICS message bus."
    #test goss message is not a json formatted string
    bridge4 = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    mock_helicsFederateGetState.return_value = 2
    bridge4._create_cim_object_map(map_file=mfile)
    with pytest.raises(ValueError) as e_info:
        bridge4._publish_to_helics_bus("hello",[])
    assert str(e_info.value) == "goss_message is not a json formatted string of a python dictionary.\ngoss_message = hello"
    #test goss message translation with empty command filter
    bridge5 = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    mock_helicsFederateGetState.return_value = 2
    mock_gad_connection.query_object_dictionary.return_value = {
        "data":[
            {
                "IdentifiedObject.name":"object_name",
                "type":"ACLineSegment"
            }
        ]
    }
    bridge5._create_cim_object_map(map_file=mfile)
    goss_message_str = json.dumps(goss_message_dict)
    bridge5._publish_to_helics_bus(goss_message_str, [])
    mock_helicsFederateCreateMessageObject.assert_called_once_with(
        bridge5.get_helics_federate())
    mock_helicsFederateGetEndpoint.assert_called_once_with(
        bridge5.get_helics_federate(), "helics_input")
    mock_helicsMessageSetString.assert_called_once_with(None,json.dumps(
        goss_message_converted_dict,indent=4,sort_keys=True))
    mock_helicsEndpointSendMessageObject("helics_input_endpoint", None)
    #test goss message translation with nonempty command filter
    mock_helicsFederateGetEndpoint.reset_mock()
    mock_helicsFederateCreateMessageObject.reset_mock()
    mock_helicsMessageSetString.reset_mock()
    mock_helicsEndpointSendMessageObject.reset_mock()
    bridge6 = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge6._create_cim_object_map(map_file=mfile)
    goss_message_str = json.dumps(goss_message_dict)
    bridge6._publish_to_helics_bus(goss_message_str, [
        {
            "objectMRID":"_2C2316E3-C3FC-4B24-BBDE-1982F4B24138",
            "attribute":"RegulatingControl.targetDeadband"}])
    mock_helicsFederateGetEndpoint.assert_called_once_with(
        bridge6.get_helics_federate(), "helics_input")
    mock_helicsFederateCreateMessageObject.assert_called_once_with(
        bridge6.get_helics_federate())
    message_dict = goss_message_converted_dict
    del message_dict["123"]["rcon_Reg"]["band_width"]
    mock_helicsMessageSetString.assert_called_once_with(None,json.dumps(
        message_dict,indent=4,sort_keys=True))
    mock_helicsEndpointSendMessageObject("helics_input_endpoint", None)
    

@patch.object(HelicsGossBridge,'_register_with_goss')
@patch.object(HelicsGossBridge,'_register_with_helics')
@patch.object(helics,'helicsFederateGetEndpoint', return_value="helics_output_endpoint")
@patch.object(helics,'helicsEndpointHasMessage', return_value=True)
@patch.object(HelicsGossBridge,'_gad_connection')
@unittest.mock.patch('service.helics_goss_bridge.datetime')
@patch.object(helics,'helicsEndpointGetMessageObject')
@patch.object(helics,'helicsMessageGetString', return_value = json.dumps(
    helics_simulation_output_message_dict))
def test_helics_goss_bridge_get_helics_bus_messages(
        mock_helicsMessageGetString,
        mock_helicsEndpointGetMessageObject,mock_datetime,mock_gad_connection,
        mock_helicsEndpointHasMessage,mock_helicsFederateGetEndpoint,
        mock_register_with_helics,mock_register_with_goss):
    #test with empty measurement_filter
    mock_datetime.utcnow.return_value = datetime(2017,8,25,10,33,6,150642)
    bridge = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge._create_cim_object_map(map_file=mfile)
    measurement_message = bridge._get_helics_bus_messages([])
    assert measurement_message == cim_measurement_message
    #test with nonempty measurement_filter
    measurement_message = bridge._get_helics_bus_messages(["_ff284d40-d818-427f-9497-bdaead163780"])
    del cim_measurement_message["message"]["measurements"]["_ff284d40-d818-427f-9497-bdaead163780"]
    assert measurement_message == cim_measurement_message


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(helics,'helicsFederateRequestTime', return_value=23)
@patch.object(HelicsGossBridge,'_gad_connection')
def test_helics_goss_bridge_done_with_time_step(mock_gad_connection,
        mock_helicsFederateRequestTime,mock_init):
    bridge = HelicsGossBridge("123",5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge._done_with_time_step(23)
    mock_helicsFederateRequestTime.assert_called_once_with(
        bridge.get_helics_federate(),24)
    mock_gad_connection.send_simulation_status.assert_called_once()
    mock_helicsFederateRequestTime.reset_mock()
    mock_gad_connection.reset_mock()
    mock_helicsFederateRequestTime.return_value = 24
    bridge._done_with_time_step(23)
    mock_helicsFederateRequestTime.assert_called_once_with(
        bridge.get_helics_federate(),24)
    mock_gad_connection.assert_not_called()


@patch.object(HelicsGossBridge,'__init__', return_value=None)
@patch.object(HelicsGossBridge,'_gad_connection')
def test_helics_goss_bridge_create_cim_object_map(mock_gad_connection,
        mock_init):
    bridge = bridge = HelicsGossBridge(123,5570,
        {"simulation_config":{"run_realtime":0,"duration":5,
            "simulation_start":0}})
    bridge._create_cim_object_map(map_file=mfile)
    assert bridge.get_model_mrid() == "_49AD8E07-3BF9-A4E2-CB8F-C3722F837B62"
    assert bridge.get_object_property_to_measurement_id() == object_property_to_measurement_id
    assert bridge.get_object_mrid_to_name() == object_mrid_to_name
    bridge._create_cim_object_map()
    mock_gad_connection.send_simulation_status.assert_called_once()
    
    
    
    
    
    
    
    
    