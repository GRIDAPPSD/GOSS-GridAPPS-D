from durable.lang import *

testInput = ruleset('input')


with ruleset('test'):
    # antecedent
    @when_all((m.message.measurement.magnitude <= 3410.456) & (m.message.measurement.measurement_mrid == "123a456b-789c-012d-345e-678f901a234b"))
    def say_hello(c):
        # consequent
        print 'Magnitude {0}'.format(c.m.message.measurement.magnitude)


    @when_all(m.simulation_id == "12ae2345")
    def say_hello_id(c):
        # consequent
        print 'Hello {0}'.format(c.m.simulation_id )

# with ruleset('input'):
with testInput:
    ## Get start and end from TestConfig
    run_start= "2017-07-21 12:00:00"
    run_end = "2017-07-22 12:00:00"

    @when_all((m.message.measurement.magnitude <= 3410.456) & (m.message.measurement.measurement_mrid == "123a456b-789c-012d-345e-678f901a234b"))
    def say_hello(c):
        # consequent
        print 'Magnitude {0}'.format(c.m.message.measurement.magnitude)


    # A Reverse and a Forward differance is a state change.
    @when_all((m.message.reverse_difference.attribute == 'Switch.open') & (m.message.reverse_difference.attribute == 'Switch.open'))
    def switch_open(c):
        # consequent
        # print('approved {0}'.format(c.m))
        # print 'Switch id {0}'.format(c.m.message.difference_mrid)
        c.post({'mrid': c.m.message.difference_mrid, 'action':c.m.message.reverse_difference.attribute})

    @when_all(count(3), +m.mrid)
    def output(c):
        # print (c)
        print ("For Posting: 3 changes at different times at the same switch.")
        for f in c.m:
            print ('Count MRID Fact: {0} '.format(f))
            
        file = open('testfile.txt','w') 
        for f in c.m:
			file.write('Count MRID Fact: {0} '.format(f)) 
        file.close() 
        #post to database?

    @when_start
    def start(host):
        # host.post('input', {'id': 1, 'sid': 1, 'subject': 'World'})
        host.post('input', {'mrid': 1})
        host.assert_fact('input', {'mrid': 123})


run_all(port=5000)
# run_all([{"host":"127.0.0.1","port":5511 }])
# run_all([{'host': 'host_name', 'port': port, 'password': 'password'}]);

# curl -H "Content-type: application/json" -X POST -d '{ "simulation_id" : "12ae2345", "message" : { "timestamp" : "YYYY-MM-DDThh:mm:ss.sssZ", "measurement" : { "measurement_mrid" : "123a456b-789c-012d-345e-678f901a234b", "magnitude" : 3410.456, "angle" : -123.456 } }}' http://localhost:5000/test/events
# curl -H "Content-type: application/json" -X POST -d '{"simulation_id" : "12ae2345", "message" : { "timestamp" : "YYYY-MM-DDThh:mm:ss.sssZ", "difference_mrid" : "123a456b-789c-012d-345e-678f901a234b", "reverse_difference" : { "attribute" : "Switch.open", "value" : "0" }, "forward_difference" : { "attribute" : "Switch.open", "value" : "1" } }}' http://localhost:5000/input/events
# curl -H "Content-type: application/json" -X POST -d '{"subject": "World"}' http://localhost:5000/test/events