use controller::AsyncController;
use session::{AsyncSession, SessionArc};
use std::sync::Arc;
use tokio::net::TcpListener;
use tokio::sync::mpsc;

use crate::config::ServerConfig;
pub mod controller;
pub mod message;
pub mod session;
pub mod session_manager;

use crate::player::player_manager::PLAYER_MANAGER;
use once_cell::sync::Lazy;
use session_manager::SessionManager;

pub static SESSION_MANAGER: Lazy<SessionManager> = Lazy::new(|| SessionManager::new());

pub async fn start_server(config: &ServerConfig) -> anyhow::Result<()> {
    let host = &config.listen_host;
    let port = &config.listen_port;
    let addr = format!("{}:{}", host, port);
    let listener = TcpListener::bind(&addr).await?;
    println!("Server listening on {}", addr);
    loop {
        match listener.accept().await {
            Ok((socket, addr)) => {
                println!("New connection from: {}", addr);
                tokio::spawn(async move {
                    if let Err(()) = handle_connection(socket).await {
                        eprintln!("Error handling connection");
                    }
                });
            }
            Err(e) => {
                eprintln!("Error accepting connection: {}", e);
            }
        }
    }
}

async fn handle_connection(socket: tokio::net::TcpStream) -> Result<(), ()> {
    let session = AsyncSession::new(socket);
    let (tx, rx) = mpsc::channel::<message::Message>(2024);
    session.set_message_channel(tx).await;
    let session_arc: SessionArc = Arc::new(session);
    let write_task = spawn_write_task(session_arc.get_writer(), rx);
    run_read_loop(session_arc.clone()).await;
    cleanup_session(session_arc, write_task).await;
    println!("Connection closed");
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
                eprintln!("[WRITE_TASK] Error: {:?}", e);
                break;
            }
        }
    })
}

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
                    eprintln!("Error handling message: {:?}", e);
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
            println!("Client disconnected normally - Error: {}", e);
        }
        _ => {
            eprintln!("[Error] {} (kind={:?})", e, e.kind());
        }
    }
}

async fn cleanup_session(session: SessionArc, write_task: tokio::task::JoinHandle<()>) {
    write_task.abort();

    if let Some(mut player) = session.get_player().await {
        use crate::map::ChangeMapService;
        let change_map_service = ChangeMapService::new();
        if let Err(e) = change_map_service.exit_map(&mut player) {
            eprintln!("Error exiting map on disconnect: {:?}", e);
        }

        if let Err(e) = crate::services::player_service::save_player(&player).await {
            eprintln!("Error saving player {} on disconnect: {:?}", player.name, e);
        }

        (&*SESSION_MANAGER).remove_session(player.id as i64);
        PLAYER_MANAGER.remove(player.id);
        println!("Player {} disconnected and session removed", player.id);
    }
}
