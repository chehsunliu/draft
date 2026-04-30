package auth

import (
	"net/http"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/itxctx"
	"github.com/gin-gonic/gin"
)

func RequireUser() gin.HandlerFunc {
	return func(c *gin.Context) {
		ctx, ok := itxctx.From(c)
		if !ok || ctx.UserID == nil {
			// Use Status+Abort (not AbortWithStatus) so wrap can still
			// rewrite the response — AbortWithStatus flushes headers eagerly.
			c.Status(http.StatusUnauthorized)
			c.Abort()
			return
		}
		c.Next()
	}
}
