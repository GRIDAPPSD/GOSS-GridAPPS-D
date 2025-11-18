ARG GRIDAPPSD_BASE_VERSION=:rc2
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
  && git clone https://github.com/GRIDAPPSD/topology-processor -b main gridappsd-topology-background-service\
  && cd gridappsd-topology-background-service/ \
  && mkdir -p /gridappsd/services/gridappsd-topology-background-service \
  && rm .git -rf \
  && cp -r * /gridappsd/services/gridappsd-topology-background-service \
  && cp /gridappsd/services/gridappsd-topology-background-service/topo_background.config /gridappsd/services/ \
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

# Get the gridapspd-distributed-ybus-service from the proper repository
RUN mkdir ${TEMP_DIR} \
  && cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-distributed-static-ybus-service.git -b main \
  && cd gridappsd-distributed-static-ybus-service \
  && mkdir -p /gridappsd/services/gridappsd-distributed-static-ybus-service \
  && rm .git -rf \
  && cp -r * /gridappsd/services/gridappsd-distributed-static-ybus-service \
  && cp /gridappsd/services/gridappsd-distributed-static-ybus-service/gridappsd-distributed-static-ybus-service.config /gridappsd/services/ \
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


# Copy the Felix launcher distribution built using ./gradlew dist
# This replaces the old BND export which is incompatible with Java 21
COPY ./build/launcher/gridappsd-launcher.jar /gridappsd/lib/gridappsd-launcher.jar
COPY ./build/launcher/bundle /gridappsd/lib/bundle
COPY ./build/launcher/config.properties /gridappsd/lib/config.properties

RUN pip install --pre -r /gridappsd/requirements.txt && \
  pip install -r /gridappsd/services/fncsgossbridge/requirements.txt && \
  rm -rf /root/.cache/pip/wheels

# Should match what is in conf/pnnl.goss.core.server.cfg and
# conf/pnnl.goss.core.client.cfg
EXPOSE 61616 61613 61614 8000-9000

WORKDIR /gridappsd

RUN echo $TIMESTAMP > /gridappsd/dockerbuildversion.txt

# Add gridappsd user , sudoers, mysql configuration, log directory
# User may already exist in base image, so only create if not present
RUN if ! id -u gridappsd > /dev/null 2>&1; then useradd -m gridappsd; fi \
    && mkdir -p /home/gridappsd \
    && if [ -d /etc/sudoers.d ] ; then echo "gridappsd    ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/gridappsd ; fi \
    && echo "[client]\nuser=gridappsd\npassword=gridappsd1234\ndatabase=gridappsd\nhost=mysql" > /home/gridappsd/.my.cnf \
    && chown -R gridappsd:gridappsd /home/gridappsd \
    && mkdir -p /gridappsd/log \
    && chown gridappsd:gridappsd /gridappsd/log

USER gridappsd

ENTRYPOINT ["/gridappsd/entrypoint.sh"]
CMD ["gridappsd"]
