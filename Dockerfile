ARG GRIDAPPSD_BASE_VERSION=:master
FROM gridappsd/gridappsd_base${GRIDAPPSD_BASE_VERSION}

ARG TIMESTAMP

# Get the gridappsd-python from the proper repository
RUN cd ${TEMP_DIR} \
  && git clone https://github.com/GRIDAPPSD/gridappsd-python -b master \
  && cd gridappsd-python \
  && python setup.py sdist \
  && pip3 install dist/gridappsd-1.0.tar.gz \
  && pip install dist/gridappsd-1.0.tar.gz \
  && rm -rf /root/.cache/pip/wheels

# Copy initial applications and services into the container.
# 
# NOTE: developers should mount a volume over the top of these or
#       mount other items specifically in the /gridappsd/appplication
#       and/or /gridappsd/services location in order for gridappsd
#       to be able to "see" and ultimately start them.
# comment this out until the applications are removed from GOSS-GridAPPS-D repo
#COPY ./applications /gridappsd/applications
COPY ./services /gridappsd/services
COPY ./gov.pnnl.goss.gridappsd/conf /gridappsd/conf
COPY ./entrypoint.sh /gridappsd/entrypoint.sh
COPY ./requirements.txt /gridappsd/requirements.txt
RUN chmod +x /gridappsd/entrypoint.sh

# Add the gridlabd-vvo app & sample app
RUN cd ${TEMP_DIR} \
  && [ ! -d /gridappsd/applications ] && mkdir /gridappsd/applications \
  && git clone https://github.com/GRIDAPPSD/gridappsd-sample-app -b master \
  && cp -rp gridappsd-sample-app/sample_app/sample_app /gridappsd/applications \
  && cp -p  gridappsd-sample-app/sample_app/sample_app.config /gridappsd/applications \
  && git clone https://github.com/GRIDAPPSD/gridappsd-gridlabd-vvo -b master \
  && cp -rp gridappsd-gridlabd-vvo/vvo/vvo /gridappsd/applications \
  && cp -p  gridappsd-gridlabd-vvo/vvo/vvo.config /gridappsd/applications \
  && rm -r gridappsd-sample-app \
  && rm -r gridappsd-gridlabd-vvo

COPY ./run-gridappsd.sh /gridappsd/run-gridappsd.sh
RUN chmod +x /gridappsd/run-gridappsd.sh
RUN ln -s run-gridappsd.sh run-docker.sh

# Add the opendss command and library to the container
COPY ./opendss/opendsscmd /usr/local/bin
COPY ./opendss/liblinenoise.so /usr/local/lib
RUN chmod +x /usr/local/bin/opendsscmd && \
  ldconfig

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
RUN useradd -m gridappsd
#RUN mkdir /etc/sudoers.d  \
#        && echo "gridappsd    ALL=(ALL:ALL) NOPASSWD: ALL" > /etc/sudoers.d/gridappsd
RUN mkdir /gridappsd/log \
        && chown gridappsd:gridappsd /gridappsd/log
USER gridappsd

ENTRYPOINT ["/gridappsd/entrypoint.sh"]
CMD ["gridappsd"]
