BIN_DIR := bin
BACKEND_JAR := itx-backend/build/libs/itx-backend.jar
WORKER_JAR  := itx-worker/build/libs/itx-worker.jar

.PHONY: all build bootjars wrappers fmt check test clean

all: build

# Build does what `cargo build` does for itx-rs: produce executable artifacts at
# bin/itx-backend, bin/itx-control-worker, and bin/itx-compute-worker so that
# the integration test driver can spawn them.
build: bootjars wrappers

bootjars:
	./gradlew :itx-backend:bootJar :itx-worker:bootJar

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

test:
	./gradlew test

clean:
	./gradlew clean
	rm -rf $(BIN_DIR)
