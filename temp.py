import json
import sys
import time
import logging
logging.basicConfig(level=logging.DEBUG)

from gridappsd import GridAPPSD
import gridappsd.topics as topics


# gapps = GridAPPSD(username="system", password="manager", address=("localhost", 61613))
# assert gapps is not None
# assert isinstance(gapps, GridAPPSD)
# assert gapps.connected is True

# obj = gapps.query_object_types()

# print(json.dumps(obj, indent=2))

# from gridappsd import GridAPPSD
import os

os.environ["GRIDAPPSD_USER"] = "system"
os.environ["GRIDAPPSD_PASSWORD"] = "manager"


gapps = GridAPPSD()

simulation_request_topic = 'goss.gridappsd.process.request.simulation'


request = {"power_system_configs":
                        [{"SubGeographicalRegion_name": "Medium",
                        "GeographicalRegion_name": "IEEE",
                        "Line_name": "C1C3E687-6FFD-C753-582B-632A27E28507",
                        "simulator_config": {"simulator": "GridLAB-D",
                                                "simulation_output": {},
                                                "power_flow_solver_method": "NR",
                                                "model_creation_config":
                                                        {"load_scaling_factor": 1.0,
                                                        "triplex": "y",
                                                        "encoding": "u",
                                                        "system_frequency": 60,
                                                        "voltage_multiplier": 1.0,
                                                        "power_unit_conversion": 1.0,
                                                        "unique_names": "y",
                                                        "z_fraction": 0.0,
                                                        "i_fraction": 1.0,
                                                        "p_fraction": 0.0,
                                                        "randomize_zipload_fractions": False,
                                                        "schedule_name": "ieeezipload",
                                                        "use_houses": False}}},
                        {"SubGeographicalRegion_name": "Small",
                        "GeographicalRegion_name": "IEEE",
                        "Line_name": "49AD8E07-3BF9-A4E2-CB8F-C3722F837B62",
                        "simulator_config": {"simulator": "GridLAB-D",
                                                "simulation_output": {},
                                                "power_flow_solver_method": "NR",
                                                "model_creation_config":
                                                        {"load_scaling_factor": 1.0,
                                                        "triplex": "y",
                                                        "encoding": "u",
                                                        "system_frequency": 60,
                                                        "voltage_multiplier": 1.0,
                                                        "power_unit_conversion": 1.0,
                                                        "unique_names": "y",
                                                        "z_fraction": 0.0,
                                                        "i_fraction": 1.0,
                                                        "p_fraction": 0.0,
                                                        "randomize_zipload_fractions": False,
                                                        "schedule_name": "ieeezipload",
                                                        "use_houses": False}}}],
        "simulation_config": {"duration": 7200,
                        "start_time": 1758234830,
                        "run_realtime": True,
                        "pause_after_measurements": False,
                        "simulation_broker_port": 5570,
                        "simulation_broker_location": "127.0.0.1",
                        "simulation_name":"testing-simulation"}
}

# #        "simulation_id": "simulation-id-12345"}


response = gapps.get_response(simulation_request_topic, request, timeout=30)
dumps = json.dumps(request)


#output = json.dumps(dumps, indent=2)
#sys.stdout.write(output)




def subscription_callback(headers, message):
    print("Received message:")
    print(message)


simulation_id = response.get('simulationId')

#simulation_id=1094795126
print(f"Subscribing to simulation output topic...{topics.simulation_output_topic(simulation_id=int(simulation_id))}")
gapps.subscribe(topic=topics.simulation_output_topic(simulation_id=int(simulation_id)),
                callback=subscription_callback)

# #print(response)

while True:
    time.sleep(1)

# #print(json.dumps(status, indent=2))
