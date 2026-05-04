use async_trait::async_trait;
use itx_contract::email::error::EmailError;
use itx_contract::email::{EmailClient, SendEmailMessage};
use reqwest::Client;

fn err<E: std::fmt::Display>(e: E) -> EmailError {
    EmailError::Unknown(e.to_string())
}

#[derive(serde::Deserialize)]
struct HttpEmailClientConfig {
    pub url: String,
    pub api_key: String,
}

pub struct HttpEmailClient {
    client: Client,
    config: HttpEmailClientConfig,
}

impl HttpEmailClient {
    pub fn from_env() -> Self {
        let config = envy::prefixed("ITX_EMAIL_")
            .from_env::<HttpEmailClientConfig>()
            .expect("failed to read EMAIL environment variables");
        Self {
            client: Client::new(),
            config,
        }
    }
}

#[async_trait]
impl EmailClient for HttpEmailClient {
    async fn send(&self, msg: SendEmailMessage) -> Result<(), EmailError> {
        let resp = self
            .client
            .post(&self.config.url)
            .bearer_auth(&self.config.api_key)
            .json(&msg)
            .send()
            .await
            .map_err(err)?;
        let status = resp.status();
        if !status.is_success() {
            let body = resp.text().await.unwrap_or_default();
            return Err(EmailError::Unknown(format!("email API returned {status}: {body}")));
        }
        Ok(())
    }
}
