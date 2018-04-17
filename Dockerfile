ARG GRIDAPPSD_VERSION_LABEL=:dev

FROM gridappsd/gridappsd_base${GRIDAPPSD_VERSION_LABEL}

ARG TIMESTAMP

# Add specific pip requirements files for installation onto this image in
# the following location. 
#
# NOTE: as an example the fncsgossbridge requirements file can be used as a
#       reference point.
RUN apt-get update && \
  apt-get install -y python-pip && \
  rm -rf /var/cache/apt/archives/* && \
  rm -rf /root/.cache/pip/wheels

# Copy initial applications and services into the container.
# 
# NOTE: developers should mount a volume over the top of these or
#       mount other items specifically in the /gridappsd/appplication
#       and/or /gridappsd/services location in order for gridappsd
#       to be able to "see" and ultimately start them.
COPY ./applications /gridappsd/applications
COPY ./services /gridappsd/services
COPY ./gov.pnnl.goss.gridappsd/conf /gridappsd/conf
COPY ./entrypoint.sh /gridappsd/entrypoint.sh
COPY ./requirements.txt /gridappsd/requirements.txt
RUN chmod +x /gridappsd/entrypoint.sh

COPY ./run-docker.sh /gridappsd/run-docker.sh
RUN chmod +x /gridappsd/run-docker.sh

# Add mysql configuration
RUN echo "[client]\nuser=gridappsd\npassword=gridappsd1234\ndatabase=gridappsd\nhost=mysql" > /root/.my.cnf

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

ENTRYPOINT ["/gridappsd/entrypoint.sh"]
CMD ["gridappsd"]
