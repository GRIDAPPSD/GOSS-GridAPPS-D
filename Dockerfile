ARG GRIDAPPSD_BASE_VERSION=:master
FROM gridappsd/gridappsd_base${GRIDAPPSD_BASE_VERSION}

ARG TIMESTAMP

# Update to use python 3.6 as default for python3.
RUN sudo apt-get update && sudo apt-get install -y software-properties-common \ 
  && sudo add-apt-repository ppa:deadsnakes/ppa \
  && sudo apt-get update && sudo apt-get install -y python3.6 python3.6-dev \
  && sudo /usr/bin/update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.6 1 \
  && sudo /usr/bin/update-alternatives --install /usr/bin/python python /usr/bin/python3.6 1 \
  && sudo sudo /usr/bin/update-alternatives  --set python /usr/bin/python3.6 \
  && sudo sudo /usr/bin/update-alternatives  --set python3 /usr/bin/python3.6 \
  && rm -rf /var/cache/apt/archives/* \
  && rm -rf /root/.cache/pip/wheels

# TODO remove after we modify the base container to do this properly
RUN mkdir -p /usr/local/lib/python3.6/dist-packages/fncs
RUN cp /usr/local/lib/python3.5/dist-packages/fncs/fncs.py /usr/local/lib/python3.6/dist-packages/fncs/fncs.py


# Get the gridappsd-python from the proper repository
RUN cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-python -b develop \
  && cd gridappsd-python \
  && pip3 install . \
  && rm -rf /root/.cache/pip/wheels

# Get the gridappsd-sensor-simulator from the proper repository
RUN cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-sensor-simulator -b develop  \
  && cd gridappsd-sensor-simulator \
  && pip3 install -r requirements.txt \
  && mkdir -p /gridappsd/services/gridappsd-sensor-simulator \
  && rm .git -rf \ 
  && cp * /gridappsd/services/gridappsd-sensor-simulator \
  && cp /gridappsd/services/gridappsd-sensor-simulator/sensor_simulator.config /gridappsd/services/ \
  && rm -rf /root/.cache/pip/wheels


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
