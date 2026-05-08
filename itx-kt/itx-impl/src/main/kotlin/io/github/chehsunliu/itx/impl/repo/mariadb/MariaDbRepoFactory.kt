package io.github.chehsunliu.itx.impl.repo.mariadb

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.chehsunliu.itx.contract.repo.PostRepo
import io.github.chehsunliu.itx.contract.repo.RepoFactory
import io.github.chehsunliu.itx.contract.repo.SubscriptionRepo
import io.github.chehsunliu.itx.contract.repo.UserRepo
import io.github.chehsunliu.itx.impl.requireEnv
import javax.sql.DataSource

class MariaDbRepoFactory(
    private val dataSource: DataSource,
) : RepoFactory {
    companion object {
        fun fromEnv(): MariaDbRepoFactory = MariaDbRepoFactory(dataSourceFromEnv())

        fun dataSourceFromEnv(): DataSource {
            val cfg =
                HikariConfig().apply {
                    jdbcUrl =
                        "jdbc:mariadb://${requireEnv(
                            "ITX_MARIADB_HOST",
                        )}:${requireEnv("ITX_MARIADB_PORT")}/${requireEnv("ITX_MARIADB_DB_NAME")}?useSsl=false"
                    username = requireEnv("ITX_MARIADB_USER")
                    password = requireEnv("ITX_MARIADB_PASSWORD")
                    driverClassName = "org.mariadb.jdbc.Driver"
                    maximumPoolSize = 10
                }
            return HikariDataSource(cfg)
        }
    }

    override fun createPostRepo(): PostRepo = MariaDbPostRepo(dataSource)

    override fun createUserRepo(): UserRepo = MariaDbUserRepo(dataSource)

    override fun createSubscriptionRepo(): SubscriptionRepo = MariaDbSubscriptionRepo(dataSource)
}
