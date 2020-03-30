FROM maven

RUN cd /opt && \
	svn co svn://150.146.207.10/software/luigi/workspace/lgu.commons --non-interactive --username luigi --password _7u:16i_ && \
	cd /opt/lgu.commons && \
	mvn clean install && \
	cd /opt  && \
	svn co svn://150.146.207.10/trunk/software/pss --non-interactive --username luigi --password _7u:16i_ && \
	cd /opt/pss/pss.harvester  && \
    mvn clean install

VOLUME /opt/pss/pss.harvester/data    

CMD ["/bin/bash", "/opt/pss/pss.harvester/scripts/pss.sh"]
