package itxctx

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

const (
	HeaderRequestID = "X-Itx-Request-Id"
	HeaderUserID    = "X-Itx-User-Id"
	HeaderUserEmail = "X-Itx-User-Email"
	contextKey      = "itxctx"
)

type Context struct {
	RequestID uuid.UUID
	UserID    *uuid.UUID
	UserEmail string
}

func parseUUIDHeader(c *gin.Context, name string) (*uuid.UUID, bool) {
	raw := c.GetHeader(name)
	if raw == "" {
		return nil, true
	}
	id, err := uuid.Parse(raw)
	if err != nil {
		c.String(http.StatusBadRequest, "invalid "+name)
		c.Abort()
		return nil, false
	}
	return &id, true
}

func Extract() gin.HandlerFunc {
	return func(c *gin.Context) {
		reqID, ok := parseUUIDHeader(c, HeaderRequestID)
		if !ok {
			return
		}
		userID, ok := parseUUIDHeader(c, HeaderUserID)
		if !ok {
			return
		}

		var rid uuid.UUID
		if reqID != nil {
			rid = *reqID
		} else {
			rid = uuid.New()
		}

		c.Set(contextKey, Context{
			RequestID: rid,
			UserID:    userID,
			UserEmail: c.GetHeader(HeaderUserEmail),
		})
		c.Next()
	}
}

func From(c *gin.Context) (Context, bool) {
	v, ok := c.Get(contextKey)
	if !ok {
		return Context{}, false
	}
	ctx, ok := v.(Context)
	return ctx, ok
}
