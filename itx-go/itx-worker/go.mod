module github.com/chehsunliu/itx/itx-go/itx-worker

go 1.25.4

require (
	github.com/chehsunliu/itx/itx-go/itx-contract v0.0.0
	github.com/chehsunliu/itx/itx-go/itx-impl v0.0.0
)

require (
	filippo.io/edwards25519 v1.2.0 // indirect
	github.com/aws/aws-sdk-go-v2 v1.41.7 // indirect
	github.com/aws/aws-sdk-go-v2/config v1.32.17 // indirect
	github.com/aws/aws-sdk-go-v2/credentials v1.19.16 // indirect
	github.com/aws/aws-sdk-go-v2/feature/ec2/imds v1.18.23 // indirect
	github.com/aws/aws-sdk-go-v2/internal/configsources v1.4.23 // indirect
	github.com/aws/aws-sdk-go-v2/internal/endpoints/v2 v2.7.23 // indirect
	github.com/aws/aws-sdk-go-v2/internal/v4a v1.4.24 // indirect
	github.com/aws/aws-sdk-go-v2/service/internal/accept-encoding v1.13.9 // indirect
	github.com/aws/aws-sdk-go-v2/service/internal/presigned-url v1.13.23 // indirect
	github.com/aws/aws-sdk-go-v2/service/signin v1.0.11 // indirect
	github.com/aws/aws-sdk-go-v2/service/sqs v1.42.27 // indirect
	github.com/aws/aws-sdk-go-v2/service/sso v1.30.17 // indirect
	github.com/aws/aws-sdk-go-v2/service/ssooidc v1.35.21 // indirect
	github.com/aws/aws-sdk-go-v2/service/sts v1.42.1 // indirect
	github.com/aws/smithy-go v1.25.1 // indirect
	github.com/go-sql-driver/mysql v1.10.0 // indirect
	github.com/google/uuid v1.6.0 // indirect
	github.com/jackc/pgpassfile v1.0.0 // indirect
	github.com/jackc/pgservicefile v0.0.0-20240606120523-5a60cdf6a761 // indirect
	github.com/jackc/pgx/v5 v5.9.2 // indirect
	github.com/jackc/puddle/v2 v2.2.2 // indirect
	github.com/rabbitmq/amqp091-go v1.11.0 // indirect
	golang.org/x/sync v0.20.0 // indirect
	golang.org/x/text v0.29.0 // indirect
)

replace (
	github.com/chehsunliu/itx/itx-go/itx-contract => ../itx-contract
	github.com/chehsunliu/itx/itx-go/itx-impl => ../itx-impl
)
