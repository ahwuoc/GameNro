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
pub mod split_session;

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
    // Create session with internal locks - no RwLock<AsyncSession> needed!
    let session = AsyncSession::new(socket);

    // Setup message queue
    let (tx, mut rx) = mpsc::channel::<message::Message>(256);
    session.set_message_channel(tx).await;

    // Wrap in Arc for sharing (just Arc, not RwLock!)
    let session_arc: SessionArc = Arc::new(session);

    // Get separate locks for tasks
    let writer = session_arc.get_writer();
    let reader = session_arc.get_reader();

    // Spawn write task - uses only writer lock
    let write_task = tokio::spawn(async move {
        while let Some(msg) = rx.recv().await {
            let mut w = writer.lock().await;
            if let Err(e) = w.send_message(&msg).await {
                eprintln!("[WRITE_TASK] Error: {:?}", e);
                break;
            }
            // Writer lock released immediately
        }
    });

    // Main read loop - uses only reader lock for reading
    loop {
        // Read uses SEPARATE reader lock - doesn't block writer!
        let message = {
            let mut r = reader.lock().await;
            r.read_message().await
        };
        // Reader lock released here

        match message {
            Ok(msg) => {
                // Process uses session (internal locks as needed)
                if let Err(e) = AsyncController::process(session_arc.clone(), msg).await {
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
                        eprintln!("[Error] {} (kind={:?})", e, e.kind());
                    }
                }
                break;
            }
        }
    }

    // Cleanup
    write_task.abort();

    let player = session_arc.get_player().await;

    if let Some(mut player) = player {
        use crate::map::ChangeMapService;
        let change_map_service = ChangeMapService::new();
        if let Err(e) = change_map_service.exit_map_async(&mut player).await {
            eprintln!("Error exiting map on disconnect: {:?}", e);
        }

        (&*SESSION_MANAGER).remove_session(player.id as i64).await;
        println!("Player {} disconnected and session removed", player.id);
    }

    println!("Connection closed");
    Ok(())
}
