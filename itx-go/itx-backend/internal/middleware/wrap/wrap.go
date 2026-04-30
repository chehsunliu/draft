package wrap

import (
	"bytes"
	"encoding/json"
	"net/http"
	"strconv"
	"strings"

	"github.com/gin-gonic/gin"
)

type bodyCapture struct {
	gin.ResponseWriter
	body *bytes.Buffer
}

func (w *bodyCapture) Write(b []byte) (int, error) {
	return w.body.Write(b)
}

func (w *bodyCapture) WriteString(s string) (int, error) {
	return w.body.WriteString(s)
}

func Response() gin.HandlerFunc {
	return func(c *gin.Context) {
		capture := &bodyCapture{ResponseWriter: c.Writer, body: &bytes.Buffer{}}
		c.Writer = capture

		c.Next()

		contentType := capture.Header().Get("Content-Type")
		isJSON := strings.HasPrefix(contentType, "application/json")
		status := capture.Status()
		isSuccess := status >= 200 && status < 300

		if isSuccess && !isJSON {
			_, _ = capture.ResponseWriter.Write(capture.body.Bytes())
			return
		}

		var wrapped any
		if isJSON {
			var inner any
			if err := json.Unmarshal(capture.body.Bytes(), &inner); err != nil {
				_, _ = capture.ResponseWriter.Write(capture.body.Bytes())
				return
			}
			if isSuccess {
				wrapped = gin.H{"data": inner}
			} else {
				wrapped = gin.H{"error": inner}
			}
		} else {
			message := capture.body.String()
			if message == "" {
				message = http.StatusText(status)
				if message == "" {
					message = "unknown error"
				}
			}
			wrapped = gin.H{"error": gin.H{"message": message}}
		}

		newBytes, err := json.Marshal(wrapped)
		if err != nil {
			_, _ = capture.ResponseWriter.Write(capture.body.Bytes())
			return
		}

		capture.ResponseWriter.Header().Set("Content-Type", "application/json")
		capture.ResponseWriter.Header().Set("Content-Length", strconv.Itoa(len(newBytes)))
		_, _ = capture.ResponseWriter.Write(newBytes)
	}
}
