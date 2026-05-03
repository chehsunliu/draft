package user

import (
	"errors"
	"net/http"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/itxctx"
	contractsubscription "github.com/chehsunliu/itx/itx-go/itx-contract/repo/subscription"
	contractuser "github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	"github.com/gin-gonic/gin"
	"github.com/google/uuid"
)

type userDto struct {
	ID    string `json:"id"`
	Email string `json:"email"`
}

type listSubscriptionsResponse struct {
	Items []userDto `json:"items"`
}

type Handler struct {
	userRepo         contractuser.Repo
	subscriptionRepo contractsubscription.Repo
}

func NewHandler(userRepo contractuser.Repo, subscriptionRepo contractsubscription.Repo) *Handler {
	return &Handler{userRepo: userRepo, subscriptionRepo: subscriptionRepo}
}

func (h *Handler) getMe(c *gin.Context) {
	ctx, _ := itxctx.From(c)
	if ctx.UserEmail == "" {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "missing X-Itx-User-Email"})
		c.Abort()
		return
	}

	u, err := h.userRepo.Upsert(c.Request.Context(), contractuser.UpsertParams{
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

func (h *Handler) listSubscriptions(c *gin.Context) {
	raw := c.Param("id")
	id, err := uuid.Parse(raw)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "invalid user id"})
		c.Abort()
		return
	}

	if _, err := h.userRepo.Get(c.Request.Context(), id); err != nil {
		if errors.Is(err, contractuser.ErrNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
			c.Abort()
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	authors, err := h.subscriptionRepo.ListAuthors(c.Request.Context(), id)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	items := make([]userDto, 0, len(authors))
	for _, a := range authors {
		items = append(items, userDto{ID: a.ID.String(), Email: a.Email})
	}
	c.JSON(http.StatusOK, listSubscriptionsResponse{Items: items})
}

func (h *Handler) Register(router gin.IRouter) {
	router.GET("/users/me", h.getMe)
	router.GET("/users/:id/subscriptions", h.listSubscriptions)
}
