package post

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type listResponse struct {
	Items []item `json:"items"`
}

type item struct{}

func list(c *gin.Context) {
	c.JSON(http.StatusOK, listResponse{Items: []item{}})
}

func Register(router gin.IRouter) {
	router.GET("/posts", list)
}
