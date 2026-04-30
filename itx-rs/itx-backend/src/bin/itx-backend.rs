use clap::Parser;
use itx_backend::{create_app, shutdown_signal, state::AppState};
use std::str::FromStr;
use tracing::Level;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(long, default_value = "INFO")]
    log_level: String,

    #[arg(long, default_value = "127.0.0.1")]
    host: String,

    #[arg(long, default_value = "8080")]
    port: i32,
}

#[tokio::main]
async fn main() {
    let args = Args::parse();
    let addr = format!("{}:{}", args.host, args.port);

    tracing_subscriber::fmt()
        .with_max_level(Level::from_str(&args.log_level).unwrap())
        .json()
        .flatten_event(true)
        .with_current_span(true)
        .with_span_list(false)
        .init();

    let listener = tokio::net::TcpListener::bind(addr).await.unwrap();
    let state = AppState {};

    axum::serve(listener, create_app(state))
        .with_graceful_shutdown(shutdown_signal())
        .await
        .unwrap();
}
