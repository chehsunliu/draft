package io.github.chehsunliu.itx.impl.repo.postgres

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.chehsunliu.itx.contract.repo.PostRepo
import io.github.chehsunliu.itx.contract.repo.RepoFactory
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.github.chehsunliu.itx.impl.requireEnv
import javax.sql.DataSource

class PostgresRepoFactory(
    private val dataSource: DataSource,
) : RepoFactory {
    companion object {
        fun fromEnv(): PostgresRepoFactory = PostgresRepoFactory(dataSourceFromEnv())

        fun dataSourceFromEnv(): DataSource {
            val cfg =
                HikariConfig().apply {
                    jdbcUrl =
                        "jdbc:postgresql://${requireEnv(
                            "ITX_POSTGRES_HOST",
                        )}:${requireEnv("ITX_POSTGRES_PORT")}/${requireEnv("ITX_POSTGRES_DB_NAME")}"
                    username = requireEnv("ITX_POSTGRES_USER")
                    password = requireEnv("ITX_POSTGRES_PASSWORD")
                    driverClassName = "org.postgresql.Driver"
                    maximumPoolSize = 10
                }
            return HikariDataSource(cfg)
        }
    }

    override fun createPostRepo(): PostRepo = PostgresPostRepo(dataSource)

    override fun createUserRepo(): UserRepo = PostgresUserRepo(dataSource)

    override fun createSubscriptionRepo(): SubscriptionRepo = PostgresSubscriptionRepo(dataSource)
}
