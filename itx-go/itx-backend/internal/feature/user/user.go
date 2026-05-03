package user

import (
	"net/http"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/itxctx"
	contractuser "github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	"github.com/gin-gonic/gin"
)

type userDto struct {
	ID    string `json:"id"`
	Email string `json:"email"`
}

type Handler struct {
	repo contractuser.Repo
}

func NewHandler(repo contractuser.Repo) *Handler {
	return &Handler{repo: repo}
}

func (h *Handler) getMe(c *gin.Context) {
	ctx, _ := itxctx.From(c)
	if ctx.UserEmail == "" {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "missing X-Itx-User-Email"})
		c.Abort()
		return
	}

	u, err := h.repo.Upsert(c.Request.Context(), contractuser.UpsertParams{
		ID:    *ctx.UserID,
		Email: ctx.UserEmail,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	c.JSON(http.StatusOK, userDto{ID: u.ID.String(), Email: u.Email})
}

func (h *Handler) Register(router gin.IRouter) {
	router.GET("/users/me", h.getMe)
}
