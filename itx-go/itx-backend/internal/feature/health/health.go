package health

import (
	"net/http"

	"github.com/gin-gonic/gin"
)

type response struct {
	Status string `json:"status"`
}

func get(c *gin.Context) {
	c.JSON(http.StatusOK, response{Status: "ok"})
}

func Register(router gin.IRouter) {
	router.GET("/health", get)
}
