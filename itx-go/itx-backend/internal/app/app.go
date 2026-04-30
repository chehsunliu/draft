package app

import (
	"context"
	"os"
	"os/signal"
	"syscall"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/feature/health"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/wrap"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/state"
	"github.com/gin-gonic/gin"
)

func NewRouter(_ state.AppState) *gin.Engine {
	r := gin.New()
	r.HandleMethodNotAllowed = true
	r.Use(gin.Recovery())
	r.Use(wrap.Response())

	v1 := r.Group("/api/v1")
	health.Register(v1)

	return r
}

func ShutdownContext() (context.Context, context.CancelFunc) {
	return signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
}
