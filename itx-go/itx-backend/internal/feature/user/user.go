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

func parseAuthorID(c *gin.Context) (uuid.UUID, bool) {
	raw := c.Param("id")
	id, err := uuid.Parse(raw)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "invalid user id"})
		c.Abort()
		return uuid.Nil, false
	}
	return id, true
}

func (h *Handler) subscribe(c *gin.Context) {
	authorID, ok := parseAuthorID(c)
	if !ok {
		return
	}
	ctx, _ := itxctx.From(c)
	if ctx.UserEmail == "" {
		c.JSON(http.StatusInternalServerError, gin.H{"message": "missing X-Itx-User-Email"})
		c.Abort()
		return
	}
	if *ctx.UserID == authorID {
		c.JSON(http.StatusBadRequest, gin.H{"message": "cannot subscribe to yourself"})
		c.Abort()
		return
	}

	if _, err := h.userRepo.Get(c.Request.Context(), authorID); err != nil {
		if errors.Is(err, contractuser.ErrNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
			c.Abort()
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	if _, err := h.userRepo.Upsert(c.Request.Context(), contractuser.UpsertParams{
		ID:    *ctx.UserID,
		Email: ctx.UserEmail,
	}); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	if err := h.subscriptionRepo.Subscribe(c.Request.Context(), contractsubscription.SubscribeParams{
		SubscriberID: *ctx.UserID,
		AuthorID:     authorID,
	}); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	c.Status(http.StatusNoContent)
}

func (h *Handler) unsubscribe(c *gin.Context) {
	authorID, ok := parseAuthorID(c)
	if !ok {
		return
	}
	ctx, _ := itxctx.From(c)
	if *ctx.UserID == authorID {
		c.JSON(http.StatusBadRequest, gin.H{"message": "cannot unsubscribe from yourself"})
		c.Abort()
		return
	}

	if _, err := h.userRepo.Get(c.Request.Context(), authorID); err != nil {
		if errors.Is(err, contractuser.ErrNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
			c.Abort()
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	if err := h.subscriptionRepo.Unsubscribe(c.Request.Context(), contractsubscription.UnsubscribeParams{
		SubscriberID: *ctx.UserID,
		AuthorID:     authorID,
	}); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	c.Status(http.StatusNoContent)
}

func (h *Handler) Register(router gin.IRouter) {
	router.GET("/users/me", h.getMe)
	router.POST("/users/:id/subscriptions", h.subscribe)
	router.DELETE("/users/:id/subscriptions", h.unsubscribe)
}
