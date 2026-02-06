ARG GRIDAPPSD_BASE_VERSION=:java-update
FROM gridappsd/gridappsd_base${GRIDAPPSD_BASE_VERSION}

# Install additional utilities (killall is in psmisc)
RUN apt-get update && apt-get install -y --no-install-recommends psmisc \
    && rm -rf /var/lib/apt/lists/*

# Upgrade pip first - base image has ancient pip
RUN python3 -m pip install --no-cache-dir --root-user-action=ignore --upgrade pip

# Clone and install all services in a single layer to minimize image size
# Using --depth 1 for shallow clones (no git history)
RUN set -ex \
    && mkdir -p ${TEMP_DIR} /gridappsd/services /gridappsd/applications \
    # sensor-simulator
    && git clone --depth 1 -b develop https://github.com/GRIDAPPSD/gridappsd-sensor-simulator ${TEMP_DIR}/sensor-simulator \
    && python3 -m pip install --no-cache-dir --root-user-action=ignore -r ${TEMP_DIR}/sensor-simulator/requirements.txt \
    && rm -rf ${TEMP_DIR}/sensor-simulator/.git \
    && mv ${TEMP_DIR}/sensor-simulator /gridappsd/services/gridappsd-sensor-simulator \
    && cp /gridappsd/services/gridappsd-sensor-simulator/sensor_simulator.config /gridappsd/services/ \
    # voltage-violation
    && git clone --depth 1 -b develop https://github.com/GRIDAPPSD/gridappsd-voltage-violation ${TEMP_DIR}/voltage-violation \
    && rm -rf ${TEMP_DIR}/voltage-violation/.git \
    && mv ${TEMP_DIR}/voltage-violation /gridappsd/services/gridappsd-voltage-violation \
    && cp /gridappsd/services/gridappsd-voltage-violation/voltage-violation.config /gridappsd/services/ \
    # dnp3
    && git clone --depth 1 -b develop https://github.com/GRIDAPPSD/gridappsd-dnp3 ${TEMP_DIR}/dnp3 \
    && mkdir -p /gridappsd/services/gridappsd-dnp3 \
    && cp -r ${TEMP_DIR}/dnp3/dnp3/* /gridappsd/services/gridappsd-dnp3/ \
    && cp /gridappsd/services/gridappsd-dnp3/dnp3.config /gridappsd/services/ \
    # alarms
    && git clone --depth 1 -b develop https://github.com/GRIDAPPSD/gridappsd-alarms ${TEMP_DIR}/alarms \
    && rm -rf ${TEMP_DIR}/alarms/.git \
    && mv ${TEMP_DIR}/alarms /gridappsd/services/gridappsd-alarms \
    && cp /gridappsd/services/gridappsd-alarms/gridappsd-alarms.config /gridappsd/services/ \
    # topology-processor
    && git clone --depth 1 -b main https://github.com/GRIDAPPSD/topology-processor ${TEMP_DIR}/topology \
    && rm -rf ${TEMP_DIR}/topology/.git \
    && mv ${TEMP_DIR}/topology /gridappsd/services/gridappsd-topology-background-service \
    && cp /gridappsd/services/gridappsd-topology-background-service/topo_background.config /gridappsd/services/ \
    # toolbox
    && git clone --depth 1 -b main https://github.com/GRIDAPPSD/gridappsd-toolbox ${TEMP_DIR}/toolbox \
    && rm -rf ${TEMP_DIR}/toolbox/.git \
    && mv ${TEMP_DIR}/toolbox /gridappsd/services/gridappsd-toolbox \
    && cp /gridappsd/services/gridappsd-toolbox/static-ybus/gridappsd-static-ybus-service.config /gridappsd/services/ \
    && cp /gridappsd/services/gridappsd-toolbox/dynamic-ybus/gridappsd-dynamic-ybus-service.config /gridappsd/services/ \
    # distributed-ybus
    && git clone --depth 1 -b main https://github.com/GRIDAPPSD/gridappsd-distributed-static-ybus-service.git ${TEMP_DIR}/dist-ybus \
    && rm -rf ${TEMP_DIR}/dist-ybus/.git \
    && mv ${TEMP_DIR}/dist-ybus /gridappsd/services/gridappsd-distributed-static-ybus-service \
    && cp /gridappsd/services/gridappsd-distributed-static-ybus-service/gridappsd-distributed-static-ybus-service.config /gridappsd/services/ \
    # Cleanup
    && rm -rf ${TEMP_DIR}

# Copy local services, configuration, and scripts
COPY ./services /gridappsd/services
COPY ./gov.pnnl.goss.gridappsd/conf /gridappsd/conf
COPY ./entrypoint.sh /gridappsd/entrypoint.sh
COPY ./run-gridappsd.sh /gridappsd/run-gridappsd.sh
COPY ./requirements.txt /gridappsd/requirements.txt

# Install base Python requirements and set permissions
RUN python3 -m pip install --no-cache-dir --root-user-action=ignore -r /gridappsd/requirements.txt \
    && chmod +x /gridappsd/entrypoint.sh /gridappsd/run-gridappsd.sh

# Add OpenDSS binaries
COPY ./opendss/opendsscmd /usr/local/bin/
COPY ./opendss/liblinenoise.so ./opendss/libklusolve.so /usr/local/lib/
RUN chmod +x /usr/local/bin/opendsscmd && ldconfig

# Copy the Felix launcher distribution built using ./gradlew dist
# Note: Using /gridappsd/launcher to avoid overriding /gridappsd/lib which contains GridLAB-D modules
COPY ./build/launcher/gridappsd-launcher.jar /gridappsd/launcher/gridappsd-launcher.jar
COPY ./build/launcher/bundle /gridappsd/launcher/bundle
COPY ./build/launcher/config.properties /gridappsd/launcher/config.properties

WORKDIR /gridappsd

# Create symlink for backwards compatibility (must be after WORKDIR)
RUN ln -s run-gridappsd.sh run-docker.sh

# Exposed ports: ActiveMQ (61616), STOMP (61613, 61614), Dynamic app ports (8000-9000)
EXPOSE 61616 61613 61614 8000-9000

# Add gridappsd user and configure permissions
# User may already exist in base image, so only create if not present
RUN if ! id -u gridappsd > /dev/null 2>&1; then useradd -m gridappsd; fi \
    && mkdir -p /home/gridappsd /gridappsd/log \
    && if [ -d /etc/sudoers.d ]; then echo "gridappsd    ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/gridappsd; fi \
    && printf '[client]\nuser=gridappsd\npassword=gridappsd1234\ndatabase=gridappsd\nhost=mysql\n' > /home/gridappsd/.my.cnf \
    && chown -R gridappsd:gridappsd /home/gridappsd /gridappsd/log

# Build timestamp - placed last to avoid cache busting
ARG TIMESTAMP
RUN echo $TIMESTAMP > /gridappsd/dockerbuildversion.txt

USER gridappsd

ENTRYPOINT ["/gridappsd/entrypoint.sh"]
CMD ["gridappsd"]
