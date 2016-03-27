LCSIM_JAR = $(ILC)/lcsim/distribution/target/lcsim-distribution-3.1.1-SNAPSHOT-bin.jar
SCIPPCODE = $(ILC)/scipp_ilc_framework
SOURCE    = $(SCIPPCODE)/src
TARGET    = $(SCIPPCODE)/target
PACKAGE   = scipp_ilc

GEOMETRY  = $(SOURCE)/base/beamcal/geometry/*java
UTIL      = $(SOURCE)/base/util/*java
DRIVERS   = $(SOURCE)/drivers/*java
SOURCE_FILES = $(GEOMETRY) $(UTIL) $(DRIVERS)

OUTPUT_JAR = $(PACKAGE)-3.0.jar

output: classes
	-make -s clean 2> /dev/null
	echo -e '\nINSTALLING:\n'
	cd $(TARGET); jar -cf $(OUTPUT_JAR) $(PACKAGE);
	echo -e '\nJAR FILE SUCCESSFULLY INSTALLED!\n'

classes: $(LCSIM_JAR)
	-make -s spotless 2> /dev/null
	echo -e '\nCOMPILING:\n'
	javac -Xlint:none $(SOURCE_FILES) -cp $(LCSIM_JAR) -d $(TARGET)

clean:
	-rm $(TARGET)/$(OUTPUT_JAR)

spotless:
	-rm -r $(TARGET)/$(PACKAGE)
