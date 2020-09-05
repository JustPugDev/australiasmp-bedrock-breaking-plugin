.PHONY: build force-build delete-jar format install

java-files := $(shell find src -name '*.java')
dist-path = ../../server/plugins
jar-file = $(dist-path)/australiasmp-bedrock-breaking-plugin.jar

build: $(jar-file)
$(dist-path)/australiasmp-bedrock-breaking-plugin.jar: $(java-files) plugin.yml
	@rm -rf bin
	@mkdir -p bin
	@cp plugin.yml bin
	javac -d bin -cp lib/spigot-api-1.16.2-R0.1-SNAPSHOT-shaded.jar:lib/ProtocolLib.jar $(java-files)
	@cd bin && jar --verbose --create --file ../$(jar-file) .

force-build: delete-jar build
delete-jar:
	@rm -f $(jar-file)

format: src/.format
src/.format: $(java-files)
	@uncrustify --replace --no-backup -c uncrustify.cfg -q $(java-files)
	@google-java-format --replace $(java-files)
	@touch $@
