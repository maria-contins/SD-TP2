# base image
FROM nunopreguica/sd2122tpbase2

# working directory inside docker image
WORKDIR /home/sd

# copy the jar created by assembly to the docker image
COPY target/*jar-with-dependencies.jar sd2122.jar

# copy the file of properties to the docker image
COPY trab.props trab.props

# copy the keystore and truststore to the docker image
COPY *.*ks /home/sd/

CMD ["java", "-cp", "/home/sd/sd2122.jar", "tp2.impl.service.rest.DirectoryRestServer"]
