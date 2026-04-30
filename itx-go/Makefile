BIN_DIR := bin

.PHONY: all build build-backend build-worker clean

all: build

build: build-backend build-worker

build-backend:
	go build -o $(BIN_DIR)/itx-backend ./itx-backend/cmd/itx-backend

build-worker:
	go build -o $(BIN_DIR)/itx-worker ./itx-worker/cmd/itx-worker

clean:
	rm -rf $(BIN_DIR)
