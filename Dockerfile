ARG GRIDAPPSD_BASE_VERSION=:master
FROM gridappsd/gridappsd_base${GRIDAPPSD_BASE_VERSION}

ARG TIMESTAMP

# Get the gridappsd-sensor-simulator from the proper repository
RUN if [ ! -d ${TEMP_DIR} ]; then mkdir ${TEMP_DIR}; fi \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-sensor-simulator -b develop  \
  && cd gridappsd-sensor-simulator \
  && pip3 install -r requirements.txt \
  && mkdir -p /gridappsd/services/gridappsd-sensor-simulator \
  && rm .git -rf \ 
  && cp -r * /gridappsd/services/gridappsd-sensor-simulator \
  && cp /gridappsd/services/gridappsd-sensor-simulator/sensor_simulator.config /gridappsd/services/ \
  && rm -rf /root/.cache/pip/wheels \
  && cd \ 
  && rm -rf ${TEMP_DIR}

# Get the gridappsd-voltage-violation from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-voltage-violation -b develop \
  && cd gridappsd-voltage-violation \
  && mkdir -p /gridappsd/services/gridappsd-voltage-violation \
  && rm .git -rf \ 
  && cp -r * /gridappsd/services/gridappsd-voltage-violation \
  && cp /gridappsd/services/gridappsd-voltage-violation/voltage-violation.config /gridappsd/services/ \
  && cd \
  && rm -rf ${TEMP_DIR}

# Get the gridappsd-dnp3 from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-dnp3 -b develop \
  && cd gridappsd-dnp3 \
  && mkdir -p /gridappsd/services/gridappsd-dnp3 \
  && rm .git -rf \ 
  && cp -r dnp3/* /gridappsd/services/gridappsd-dnp3 \
  && cp /gridappsd/services/gridappsd-dnp3/dnp3.config /gridappsd/services/ \
  && cd \
  && rm -rf ${TEMP_DIR}

# Get the gridappsd-alarms from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-alarms -b develop \
  && cd gridappsd-alarms \
  && mkdir -p /gridappsd/services/gridappsd-alarms \
  && rm .git -rf \ 
  && cp -r * /gridappsd/services/gridappsd-alarms \
  && cp /gridappsd/services/gridappsd-alarms/gridappsd-alarms.config /gridappsd/services/ \
  && cd \
  && rm -rf ${TEMP_DIR}

# Get the topology-processor from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/topology-processor -b main \
  && cd topology-processor/topology_processor \
  && mkdir -p /gridappsd/services/gridappsd-topology-processor \
  && rm .git -rf \ 
  && cp -r * /gridappsd/services/gridappsd-topology-processor \
  && cp /gridappsd/services/gridappsd-topology-processor/gridappsd-topology-service.config /gridappsd/services/ \
  && cp /gridappsd/services/gridappsd-topology-processor/gridappsd-topology-daemon.config /gridappsd/services/ \
  && cd \
  && rm -rf ${TEMP_DIR}

# Get the gridappsd-toolbox from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-toolbox -b main \
  && cd gridappsd-toolbox \
  && mkdir -p /gridappsd/services/gridappsd-toolbox \
  && rm .git -rf \ 
  && cp -r * /gridappsd/services/gridappsd-toolbox \
  && cp /gridappsd/services/gridappsd-toolbox/static-ybus/gridappsd-static-ybus-service.config /gridappsd/services/ \
  && cp /gridappsd/services/gridappsd-toolbox/dynamic-ybus/gridappsd-dynamic-ybus-service.config /gridappsd/services/ \
  && cd \
  && rm -rf ${TEMP_DIR}
  
# Get the CIMHub from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/CIMHub -b main \
  && cd CIMHub \
  && mkdir -p /gridappsd/CIMHub \
  && rm .git -rf \ 
  && cp -r * /gridappsd/CIMHub \
  && cd \
  && rm -rf ${TEMP_DIR}  

# Copy initial applications and services into the container.
# 
# NOTE: developers should mount a volume over the top of these or
#       mount other items specifically in the /gridappsd/appplication
#       and/or /gridappsd/services location in order for gridappsd
#       to be able to "see" and ultimately start them.
COPY ./services /gridappsd/services
COPY ./gov.pnnl.goss.gridappsd/conf /gridappsd/conf
COPY ./entrypoint.sh /gridappsd/entrypoint.sh
COPY ./requirements.txt /gridappsd/requirements.txt
RUN chmod +x /gridappsd/entrypoint.sh

# Add the applications directory which is necessary for gridappsd to operate.
RUN if [ ! -d /gridappsd/applications ] ; then  mkdir /gridappsd/applications ; fi 

COPY ./run-gridappsd.sh /gridappsd/run-gridappsd.sh
RUN chmod +x /gridappsd/run-gridappsd.sh
RUN ln -s run-gridappsd.sh run-docker.sh

# Add the opendss command and library to the container
COPY ./opendss/opendsscmd /usr/local/bin
COPY ./opendss/liblinenoise.so /usr/local/lib
COPY ./opendss/libklusolve.so /usr/local/lib
RUN chmod +x /usr/local/bin/opendsscmd && \
  ldconfig


# This is the location that is built using the ./gradlew export command from
# the command line.  When building this image we must make sure to have run that
# before executing this script.
COPY ./gov.pnnl.goss.gridappsd/generated/distributions/executable/run.bnd.jar /gridappsd/lib/run.bnd.jar

RUN pip install -r /gridappsd/requirements.txt && \
  pip install -r /gridappsd/services/fncsgossbridge/requirements.txt && \
  rm -rf /root/.cache/pip/wheels

# Should match what is in conf/pnnl.goss.core.server.cfg and
# conf/pnnl.goss.core.client.cfg
EXPOSE 61616 61613 61614 8000-9000

WORKDIR /gridappsd

RUN echo $TIMESTAMP > /gridappsd/dockerbuildversion.txt

# Add gridappsd user , sudoers, mysql configuration, log directory
RUN useradd -m gridappsd \
    && if [ -d /etc/sudoers.d ] ; then echo "gridappsd    ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/gridappsd ; fi \
    && echo "[client]\nuser=gridappsd\npassword=gridappsd1234\ndatabase=gridappsd\nhost=mysql" > /home/gridappsd/.my.cnf \
    && chown gridappsd:gridappsd /home/gridappsd/.my.cnf \
    && mkdir /gridappsd/log \
    && chown gridappsd:gridappsd /gridappsd/log

USER gridappsd

ENTRYPOINT ["/gridappsd/entrypoint.sh"]
CMD ["gridappsd"]
