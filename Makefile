.KEEP_STATE:

# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY, to the extent permitted by law; without
# even the implied warranty of MERCHANTABILITY or FITNESS FOR A
# PARTICULAR PURPOSE.

all: cfiles jar

VERSION = 1.0
PACKAGE = YajaLife

#JC = javac
JC = jikes
JAVA = java
JAR = jar
JCFLAGS = -O -nowarn -g:none -classpath acme.jar:${CLASSPATH}
JDOC = javadoc

JFILES = djr/bugs/BitVector.java	djr/bugs/Resource.java \
	djr/bugs/YajaLifeApplet.java    djr/bugs/YajaLifeControlPanel.java \
	djr/bugs/YajaLife.java          djr/bugs/Constants.java \
	djr/bugs/Dictionary.java        djr/bugs/Genome.java \
	djr/bugs/Globals.java           djr/bugs/Griddable.java \
	djr/util/array/MyVector.java    djr/bugs/Grid.java \
	djr/util/array/ObjVector.java   djr/bugs/IntGriddable.java \
	djr/bugs/ResourceGrid.java      djr/util/MyUtils.java

CFILES = djr/bugs/BitVector.class	djr/bugs/Resource.class \
	djr/bugs/YajaLifeApplet.class    djr/bugs/YajaLifeControlPanel.class \
	djr/bugs/YajaLife.class          djr/bugs/Constants.class \
	djr/bugs/Dictionary.class        djr/bugs/Genome.class \
	djr/bugs/Globals.class           djr/bugs/Griddable.class \
	djr/util/array/MyVector.class    djr/bugs/Grid.class \
	djr/util/array/ObjVector.class   djr/bugs/IntGriddable.class \
	djr/bugs/ResourceGrid.class      djr/util/MyUtils.class

INFOFILES = README COPYING COPYING.LIB Authors TODO yajalife.bat yajalife.sh acme-*
JARFILE = yajalife.jar
INTOJAR = ${CFILES} *.dat *.properties
HFILES = *.html
OTHERSRC = djr/util/MakeJarRunnable.java

cfiles: ${CFILES}
	${JC} ${JCFLAGS} ${JFILES}

jar: ${JARFILE} ${CFILES}
	${JAR} -cf ${JARFILE} ${INTOJAR} 
	${JAVA} djr.util.MakeJarRunnable ${JARFILE} djr.bugs.YajaLife temp_r.jar
	mv -f temp_r.jar ${JARFILE}

doc:
	echo '<body><pre>' > README.html
	cat README >>README.html
	echo '</pre></body>' >> README.html
	mkdir -p temp
	cp -rLf --parents ${JFILES} temp
	rpl -R 'astrodud@' 'astrodud@' temp/
	cd temp && javadoc -d ../docs -author -private -overview ../README.html ${JFILES}
	rm -rf temp README.html

dist: jar ${CFILES} ${INFOFILES} ${HFILES} 
	mkdir -p ${PACKAGE}-${VERSION}-bin
	cp -rL --parents ${JARFILE} ${INFOFILES} ${HFILES} ${PACKAGE}-${VERSION}-bin
	tar cf - ${PACKAGE}-${VERSION}-bin | gzip -c >${PACKAGE}-${VERSION}-bin.tar.gz
	rm -rf ${PACKAGE}-${VERSION}-bin

srcdist: jar doc ${CFILES} ${INFOFILES} ${HFILES} 
	mkdir -p ${PACKAGE}-${VERSION}-src
	cp -rLf --parents Makefile docs ${INFOFILES} ${JFILES} ${HFILES} ${OTHERSRC} ${PACKAGE}-${VERSION}-src
	rpl -R 'astrodud@' 'astrodud@' ${PACKAGE}-${VERSION}-src/ 
	tar chf - ${PACKAGE}-${VERSION}-src | gzip -c >${PACKAGE}-${VERSION}-src.tar.gz
	rm -rf ${PACKAGE}-${VERSION}-src docs

clean: 
	rm -f ${CFILES} ${JARFILE} 

.SUFFIXES: .java .class
.java.class:
	${JC} ${JCFLAGS} $<
