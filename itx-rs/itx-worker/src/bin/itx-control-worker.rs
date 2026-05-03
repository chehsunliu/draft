use std::str::FromStr;
use std::sync::Arc;

use clap::Parser;
use itx_contract::queue::MessageHandler;
use itx_worker::control::dispatcher::ControlDispatcher;
use itx_worker::control::state::ControlWorkerState;
use itx_worker::run::run;
use tracing::Level;

#[derive(Parser, Debug)]
#[command(version, about, long_about = None)]
struct Args {
    #[arg(long, default_value = "INFO")]
    log_level: String,
}

#[tokio::main]
async fn main() {
    let args = Args::parse();

    tracing_subscriber::fmt()
        .with_max_level(Level::from_str(&args.log_level).unwrap())
        .json()
        .flatten_event(true)
        .with_current_span(true)
        .with_span_list(false)
        .init();

    let state = ControlWorkerState::from_env().await.unwrap();
    let queues = vec![
        state.control_standard_queue.clone(),
        state.control_premium_queue.clone(),
    ];
    let handler: Arc<dyn MessageHandler> = Arc::new(ControlDispatcher::new(state));

    run(queues, handler).await;
}
