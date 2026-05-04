BIN_DIR := bin

.PHONY: all build build-backend build-control-worker build-compute-worker clean

all: build

build: build-backend build-control-worker build-compute-worker

build-backend:
	go build -o $(BIN_DIR)/itx-backend ./itx-backend/cmd/itx-backend

build-control-worker:
	go build -o $(BIN_DIR)/itx-control-worker ./itx-worker/cmd/itx-control-worker

build-compute-worker:
	go build -o $(BIN_DIR)/itx-compute-worker ./itx-worker/cmd/itx-compute-worker

clean:
	rm -rf $(BIN_DIR)
