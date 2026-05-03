package app

import (
	"context"
	"os"
	"os/signal"
	"syscall"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/feature/health"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/feature/post"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/feature/user"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/auth"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/itxctx"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/wrap"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/state"
	"github.com/gin-gonic/gin"
)

func NewRouter(s state.AppState) *gin.Engine {
	r := gin.New()
	r.HandleMethodNotAllowed = true
	r.Use(gin.Recovery())
	r.Use(wrap.Response())
	r.Use(itxctx.Extract())

	v1 := r.Group("/api/v1")
	health.Register(v1)

	protected := v1.Group("")
	protected.Use(auth.RequireUser())
	post.NewHandler(s.PostRepo).Register(protected)
	user.NewHandler(s.UserRepo, s.SubscriptionRepo).Register(protected)

	return r
}

func ShutdownContext() (context.Context, context.CancelFunc) {
	return signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
}
