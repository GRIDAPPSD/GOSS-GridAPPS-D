**Python**
The python API requires that you install the stomp.py package, you can do this using pip with the command *pip install stomp.py*  For additional documentation see https://github.com/jasonrbriggs/stomp.py/wiki/Simple-Example   You will need to create a stomp connection, listen to the output topic, and then send a message to start the simulation.


::

  import json
  import sys
  import stomp 
  import time

  goss_output_topic = '/queue/goss/gridappsd/fncs/output'  
  goss_simulation_status_topic = '/topic/goss/gridappsd/simulation/status/'  
  gossConnection= None
  isInitialized = None
  simulationId = None

  class GOSSStatusListener(object):
    def on_message(self, headers, msg):
      message = ''
      print('status ',msg)
    def on_error(self, headers, msg):
      print('simulation status error      ',msg)
  class GOSSSimulationStartListener(object):
    def on_message(self, headers, msg):
      message = ''
      print('simulation start ', msg)    
      _registerWithGOSS('system','manager', msg,gossServer='localhost',stompPort='61613')
    def on_error(self, headers, msg):
      print('simulation start error     ',msg)

  def _registerWithGOSS(username,password,simulationId,gossServer='localhost', 
                        stompPort='61613'):
      '''Register with the GOSS server broker and return.

      Function arguments:
          gossServer -- Type: string. Description: The ip location
          for the GOSS server. It must not be an empty string.
              Default: 'localhost'.
          stompPort -- Type: string. Description: The port for Stomp 
          protocol for the GOSS server. It must not be an empty string.
              Default: '61613'.
          username -- Type: string. Description: User name for GOSS connection.
          password -- Type: string. Description: Password for GOSS connection.

      Function returns:
          None.
      Function exceptions:
          RuntimeError()
      '''
      if (gossServer == None or gossServer == ''
              or type(gossServer) != str):
          raise ValueError(
              'gossServer must be a nonempty string.\n' 
              + 'gossServer = {0}'.format(gossServer))
      if (stompPort == None or stompPort == ''
              or type(stompPort) != str):
          raise ValueError(
              'stompPort must be a nonempty string.\n' 
              + 'stompPort = {0}'.format(stompPort))
      gossConnection = stomp.Connection12([(gossServer, stompPort)])
      gossConnection.start()
      gossConnection.connect(username,password)
      gossConnection.set_listener('GOSSStatusListener', GOSSStatusListener())
      gossConnection.subscribe(goss_output_topic,1)

  def _startSimulation(username,password,gossServer='localhost',stompPort='61613'):
      simulationCfg = '{"power_system_config":{"GeographicalRegion_name":"ieee8500nodecktassets_Region","SubGeographicalRegion_name":"ieee8500nodecktassets_SubRegion","Line_name":"ieee8500"}, "simulation_config":{"start_time":"03/07/2017 00:00:00","duration":"60","simulator":"GridLAB-D","simulation_name":"my test simulation","power_flow_solver_method":"FBS"}}'
      if (gossServer == None or gossServer == ''
              or type(gossServer) != str):
          raise ValueError(
              'gossServer must be a nonempty string.\n' 
              + 'gossServer = {0}'.format(gossServer))
      if (stompPort == None or stompPort == ''
              or type(stompPort) != str):
          raise ValueError(
              'stompPort must be a nonempty string.\n' 
              + 'stompPort = {0}'.format(stompPort))
      gossConnection = stomp.Connection12([(gossServer, stompPort)])
      gossConnection.start()
      gossConnection.connect(username,password, wait=True)
      gossConnection.set_listener('GOSSSimulationStartListener',GOSSSimulationStartListener())
      gossConnection.subscribe(destination='/queue/reply',id=2)
      gossConnection.send(body=simulationCfg, destination=goss_simulation_topic, headers={'reply-to': '/queue/reply'})   
      time.sleep(3) 
      print('sent simulation request')


  if __name__ == "__main__":
      #TODO: send simulationId, fncsBrokerLocation, gossLocation, 
      #stompPort, username and password as command line arguments 

      _startSimulation('username','pw',gossServer='127.0.0.1',stompPort='61613')
    

