.PHONY: build force-build delete-jar format install

java-files := $(shell find src -name '*.java')
jar-file = target/AUSMPBedrockBreaking-1.0.0.jar

build: $(jar-file)
$(jar-file): $(java-files) src/plugin.yml pom.xml
	@mvn

force-build: delete-jar build
delete-jar:
	@rm -f $(jar-file)

format: src/.format
src/.format: $(java-files) pom.xml
	@src/format_xml.py pom.xml
	@uncrustify --replace --no-backup -c uncrustify.cfg -q $(java-files)
	@google-java-format --replace $(java-files)
	@touch $@
