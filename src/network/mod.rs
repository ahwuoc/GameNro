use controller::AsyncController;
use session::{AsyncSession, SessionArc};
use std::sync::Arc;
use tokio::net::TcpListener;
use tokio::sync::mpsc;
use tracing::{error, info, instrument, warn};

use crate::config::ServerConfig;
pub mod controller;
pub mod message;
pub mod session;
pub mod session_manager;

use crate::player::player_manager::PLAYER_MANAGER;
use once_cell::sync::Lazy;
use session_manager::SessionManager;

pub static SESSION_MANAGER: Lazy<SessionManager> = Lazy::new(|| SessionManager::new());

#[instrument(skip(config))]
pub async fn start_server(config: &ServerConfig) -> anyhow::Result<()> {
    let host = &config.listen_host;
    let port = &config.listen_port;
    let addr = format!("{}:{}", host, port);
    let listener = TcpListener::bind(&addr).await?;
    info!("Server listening on {}", addr);
    loop {
        match listener.accept().await {
            Ok((socket, addr)) => {
                info!("New connection from: {}", addr);
                tokio::spawn(async move {
                    if let Err(()) = handle_connection(socket).await {
                        error!("Error handling connection from {}", addr);
                    }
                });
            }
            Err(e) => {
                error!("Error accepting connection: {}", e);
            }
        }
    }
}

#[instrument(skip(socket))]
async fn handle_connection(socket: tokio::net::TcpStream) -> Result<(), ()> {
    let session = AsyncSession::new(socket);
    let (tx, rx) = mpsc::channel::<message::Message>(2024);
    session.set_message_channel(tx).await;
    let session_arc: SessionArc = Arc::new(session);
    let write_task = spawn_write_task(session_arc.get_writer(), rx);
    run_read_loop(session_arc.clone()).await;
    cleanup_session(session_arc, write_task).await;
    info!("Connection closed");
    Ok(())
}

fn spawn_write_task(
    writer: Arc<tokio::sync::Mutex<session::SessionWriter>>,
    mut rx: mpsc::Receiver<message::Message>,
) -> tokio::task::JoinHandle<()> {
    tokio::spawn(async move {
        while let Some(msg) = rx.recv().await {
            let mut w = writer.lock().await;
            if let Err(e) = w.send_message(&msg).await {
                error!("[WRITE_TASK] Error: {:?}", e);
                break;
            }
        }
    })
}

#[instrument(skip(session))]
async fn run_read_loop(session: SessionArc) {
    let reader = session.get_reader();
    loop {
        let message_result = {
            let mut r = reader.lock().await;
            r.read_message().await
        };

        match message_result {
            Ok(msg) => {
                if let Err(e) = AsyncController::process(session.clone(), msg).await {
                    error!("Error handling message: {:?}", e);
                    break;
                }
            }
            Err(e) => {
                handle_socket_error(e);
                break;
            }
        }
    }
}

fn handle_socket_error(e: std::io::Error) {
    match e.kind() {
        std::io::ErrorKind::UnexpectedEof
        | std::io::ErrorKind::ConnectionReset
        | std::io::ErrorKind::BrokenPipe => {
            info!("Client disconnected normally - Error: {}", e);
        }
        _ => {
            error!("[Error] {} (kind={:?})", e, e.kind());
        }
    }
}

#[instrument(skip(session, write_task))]
async fn cleanup_session(session: SessionArc, write_task: tokio::task::JoinHandle<()>) {
    write_task.abort();

    if let Some(handle) = session.get_player_handle().await {
        let _ = handle
            .send(crate::player::player_actor::PlayerMessage::Logout)
            .await;
        (&*SESSION_MANAGER).remove_session(handle.id as i64);
        info!(
            "Player {} session removed and logout signal sent",
            handle.id
        );
    }
}
