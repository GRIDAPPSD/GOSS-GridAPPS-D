import stomp
import time

class BridgeListener(object):
  def on_message(self, headers, msg):
    print(msg)
 
#TODO: read ip address and stomp port from pnnl.goss.core.client.cfg
ipaddress = 'localhost'
stomp_port = 61613
input_from_goss_topic = 'goss/gridappsd/fncs/input' #this should match GridAppsDConstants.topic_FNCS_input
output_to_goss_topic = 'goss/gridappsd/fncs/output' #this should match GridAppsDConstants.topic_FNCS_output


conn = stomp.Connection12([(ipaddress, stomp_port)])



#conn.set_listener('BridgeListener', BridgeListener())
 
conn.start()
 
conn.connect('system','manager')

conn.send(output_to_goss_topic , 'test1 output')

conn.subscribe(input_from_goss_topic,1)
 
#time.sleep(10) # secs
 
#conn.disconnect()