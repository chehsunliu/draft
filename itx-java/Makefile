BIN_DIR := bin
BACKEND_JAR := itx-backend/build/libs/itx-backend.jar
WORKER_JAR  := itx-worker/build/libs/itx-worker.jar

.PHONY: all build shadowjars wrappers fmt check clean

all: build

build: shadowjars wrappers

shadowjars:
	./gradlew :itx-backend:shadowJar :itx-worker:shadowJar

wrappers:
	@mkdir -p $(BIN_DIR)
	@printf '#!/bin/sh\nexec java -jar "$$(dirname "$$0")/../%s" "$$@"\n' "$(BACKEND_JAR)" > $(BIN_DIR)/itx-backend
	@printf '#!/bin/sh\nexec java -jar "$$(dirname "$$0")/../%s" --mode=control "$$@"\n' "$(WORKER_JAR)" > $(BIN_DIR)/itx-control-worker
	@printf '#!/bin/sh\nexec java -jar "$$(dirname "$$0")/../%s" --mode=compute "$$@"\n' "$(WORKER_JAR)" > $(BIN_DIR)/itx-compute-worker
	@chmod +x $(BIN_DIR)/itx-backend $(BIN_DIR)/itx-control-worker $(BIN_DIR)/itx-compute-worker

fmt:
	./gradlew spotlessApply

check:
	./gradlew spotlessCheck

clean:
	./gradlew clean
	rm -rf $(BIN_DIR)
