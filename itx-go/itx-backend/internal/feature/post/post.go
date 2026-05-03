package post

import (
	"encoding/json"
	"errors"
	"net/http"
	"strconv"
	"time"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/itxctx"
	contractqueue "github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	contractmessage "github.com/chehsunliu/itx/itx-go/itx-contract/queue/message"
	contractpost "github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/gin-gonic/gin"
)

type postDto struct {
	ID        int64     `json:"id"`
	AuthorID  string    `json:"authorId"`
	Title     string    `json:"title"`
	Body      string    `json:"body"`
	Tags      []string  `json:"tags"`
	CreatedAt time.Time `json:"createdAt"`
}

func toDto(p contractpost.Post) postDto {
	tags := p.Tags
	if tags == nil {
		tags = []string{}
	}
	return postDto{
		ID:        p.ID,
		AuthorID:  p.AuthorID.String(),
		Title:     p.Title,
		Body:      p.Body,
		Tags:      tags,
		CreatedAt: p.CreatedAt.UTC(),
	}
}

type listResponse struct {
	Items []postDto `json:"items"`
}

type listQuery struct {
	Limit  uint32 `form:"limit"`
	Offset uint32 `form:"offset"`
}

type createBody struct {
	Title string   `json:"title"`
	Body  string   `json:"body"`
	Tags  []string `json:"tags"`
}

type updateBody struct {
	Title *string   `json:"title,omitempty"`
	Body  *string   `json:"body,omitempty"`
	Tags  *[]string `json:"tags,omitempty"`
}

type Handler struct {
	repo                 contractpost.Repo
	controlStandardQueue contractqueue.MessageQueue
}

func NewHandler(repo contractpost.Repo, controlStandardQueue contractqueue.MessageQueue) *Handler {
	return &Handler{repo: repo, controlStandardQueue: controlStandardQueue}
}

func parseID(c *gin.Context) (int64, bool) {
	idStr := c.Param("id")
	id, err := strconv.ParseInt(idStr, 10, 64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": "invalid post id"})
		c.Abort()
		return 0, false
	}
	return id, true
}

func (h *Handler) list(c *gin.Context) {
	q := listQuery{Limit: 50}
	if err := c.ShouldBindQuery(&q); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	ctx, _ := itxctx.From(c)
	posts, err := h.repo.List(c.Request.Context(), contractpost.ListParams{
		AuthorID: ctx.UserID,
		Limit:    q.Limit,
		Offset:   q.Offset,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	items := make([]postDto, 0, len(posts))
	for _, p := range posts {
		items = append(items, toDto(p))
	}
	c.JSON(http.StatusOK, listResponse{Items: items})
}

func (h *Handler) get(c *gin.Context) {
	id, ok := parseID(c)
	if !ok {
		return
	}

	ctx, _ := itxctx.From(c)
	p, err := h.repo.Get(c.Request.Context(), contractpost.GetParams{ID: id})
	if err != nil {
		if errors.Is(err, contractpost.ErrNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
			c.Abort()
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	if ctx.UserID == nil || p.AuthorID != *ctx.UserID {
		c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
		c.Abort()
		return
	}
	c.JSON(http.StatusOK, toDto(p))
}

func (h *Handler) create(c *gin.Context) {
	var body createBody
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	tags := body.Tags
	if tags == nil {
		tags = []string{}
	}

	ctx, _ := itxctx.From(c)
	p, err := h.repo.Create(c.Request.Context(), contractpost.CreateParams{
		AuthorID: *ctx.UserID,
		Title:    body.Title,
		Body:     body.Body,
		Tags:     tags,
	})
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	msgBytes, err := json.Marshal(contractmessage.NewPostCreated(p.ID, p.AuthorID))
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	if err := h.controlStandardQueue.Publish(c.Request.Context(), string(msgBytes)); err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	c.JSON(http.StatusCreated, toDto(p))
}

func (h *Handler) update(c *gin.Context) {
	id, ok := parseID(c)
	if !ok {
		return
	}
	var body updateBody
	if err := c.ShouldBindJSON(&body); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"message": err.Error()})
		c.Abort()
		return
	}

	ctx, _ := itxctx.From(c)
	p, err := h.repo.Update(c.Request.Context(), contractpost.UpdateParams{
		ID:       id,
		AuthorID: *ctx.UserID,
		Title:    body.Title,
		Body:     body.Body,
		Tags:     body.Tags,
	})
	if err != nil {
		if errors.Is(err, contractpost.ErrNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
			c.Abort()
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	c.JSON(http.StatusOK, toDto(p))
}

func (h *Handler) delete(c *gin.Context) {
	id, ok := parseID(c)
	if !ok {
		return
	}

	ctx, _ := itxctx.From(c)
	if err := h.repo.Delete(c.Request.Context(), contractpost.DeleteParams{
		ID:       id,
		AuthorID: *ctx.UserID,
	}); err != nil {
		if errors.Is(err, contractpost.ErrNotFound) {
			c.JSON(http.StatusNotFound, gin.H{"message": "not found"})
			c.Abort()
			return
		}
		c.JSON(http.StatusInternalServerError, gin.H{"message": err.Error()})
		c.Abort()
		return
	}
	c.Status(http.StatusNoContent)
}

func (h *Handler) Register(router gin.IRouter) {
	router.GET("/posts", h.list)
	router.POST("/posts", h.create)
	router.GET("/posts/:id", h.get)
	router.PATCH("/posts/:id", h.update)
	router.DELETE("/posts/:id", h.delete)
}
