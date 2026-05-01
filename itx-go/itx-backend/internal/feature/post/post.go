package post

import (
	"net/http"
	"time"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/middleware/itxctx"
	contractpost "github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/gin-gonic/gin"
)

type listResponse struct {
	Items []item `json:"items"`
}

type item struct {
	ID        int64     `json:"id"`
	AuthorID  string    `json:"authorId"`
	Title     string    `json:"title"`
	Body      string    `json:"body"`
	Tags      []string  `json:"tags"`
	CreatedAt time.Time `json:"createdAt"`
}

type listQuery struct {
	Limit  uint32 `form:"limit"`
	Offset uint32 `form:"offset"`
}

type Handler struct {
	repo contractpost.Repo
}

func NewHandler(repo contractpost.Repo) *Handler {
	return &Handler{repo: repo}
}

func (h *Handler) list(c *gin.Context) {
	q := listQuery{Limit: 50}
	if err := c.ShouldBindQuery(&q); err != nil {
		c.String(http.StatusBadRequest, err.Error())
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
		c.String(http.StatusInternalServerError, err.Error())
		c.Abort()
		return
	}

	items := make([]item, 0, len(posts))
	for _, p := range posts {
		items = append(items, item{
			ID:        p.ID,
			AuthorID:  p.AuthorID.String(),
			Title:     p.Title,
			Body:      p.Body,
			Tags:      p.Tags,
			CreatedAt: p.CreatedAt.UTC(),
		})
	}
	c.JSON(http.StatusOK, listResponse{Items: items})
}

func (h *Handler) Register(router gin.IRouter) {
	router.GET("/posts", h.list)
}
