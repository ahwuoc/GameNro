use controller::AsyncController;
use session::AsyncSession;
use std::sync::Arc;
use tokio::net::TcpListener;
use tokio::sync::RwLock;

use crate::config::ServerConfig;
pub mod controller;
pub mod message;
pub mod session;
pub mod session_manager;

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
    let session_arc = Arc::new(RwLock::new(session));

    loop {
        let mut session_guard = session_arc.write().await;
        match session_guard.read_message().await {
            Ok(message) => {
                if let Err(e) =
                    AsyncController::process(&mut *session_guard, message, Arc::clone(&session_arc))
                        .await
                {
                    eprintln!("Error handling message: {:?}", e);
                    break;
                }
            }
            Err(e) => {
                match e.kind() {
                    std::io::ErrorKind::UnexpectedEof
                    | std::io::ErrorKind::ConnectionReset
                    | std::io::ErrorKind::BrokenPipe => {
                        println!("Client disconnected normally - Error: {}", e);
                    }
                    _ => {
                        eprintln!(
                            "[Error] {} (kind={:?}) at {}:{}",
                            e,
                            e.kind(),
                            file!(),
                            line!()
                        );
                    }
                }
                break;
            }
        }
        drop(session_guard); // Release lock trước khi loop lại
    }

    let player_id = {
        let session = session_arc.read().await;
        session.get_player().map(|p| p.id)
    };

    if let Some(player_id) = player_id {
        (&*SESSION_MANAGER).remove_session(player_id as i64).await;
        println!("Player {} disconnected and session removed", player_id);
    }

    println!("Connection closed");
    Ok(())
}
