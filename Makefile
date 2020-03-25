# @file Makefile
# @author Stefan Wilhelm (wile)
# @license MIT
#
# GNU Makefile based build relay around gradle.
#
# Note: On Windows, unix tools are required, therefore GIT
#				should be installed globally (the "red text option"
#				in the installer), so that these files are in the
#				exec file search PATH.
#
MOD_JAR_PREFIX=minecoprocessors-
MOD_JAR=$(filter-out %-sources.jar,$(wildcard build/libs/${MOD_JAR_PREFIX}*.jar))

ifeq ($(OS),Windows_NT)
GRADLE=gradlew.bat --no-daemon
GRADLE_STOP=gradlew.bat --stop
DJS=djs
else
GRADLE=./gradlew --no-daemon
GRADLE_STOP=./gradlew --stop
DJS=djs
endif

wildcardr=$(foreach d,$(wildcard $1*),$(call wildcardr,$d/,$2) $(filter $(subst *,%,$2),$d))

#
# Targets
#
.PHONY: default mod data init clean clean-all mrproper all install

default: mod

all: clean-all mod | install

mod:
	@echo "[1.15] Building mod using gradle ..."
	@$(GRADLE) build $(GRADLE_OPTS)

clean:
	@echo "[1.15] Cleaning ..."
	@rm -rf src/generated
	@rm -rf mcmodsrepo
	@rm -f build/libs/*
	@$(GRADLE) clean

clean-all:
	@echo "[1.15] Cleaning using gradle ..."
	@rm -rf mcmodsrepo
	@rm -rf run/logs/
	@rm -rf run/crash-reports/
	@$(GRADLE) clean

mrproper: clean-all
	@rm -rf run/
	@rm -rf out/
	@rm -f .project
	@rm -f .classpath

init:
	@echo "[1.15] Initialising eclipse workspace using gradle ..."
	@$(GRADLE) eclipse
