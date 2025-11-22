use tokio::net::TcpListener;
use session::AsyncSession;
use controller::AsyncController;

use crate::config::ServerConfig;

pub mod session;
pub mod controller;
pub mod message;
pub async fn start_server(config:&ServerConfig) -> anyhow::Result<()> {
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
    let mut session = AsyncSession::new(socket);
    loop {
        match session.read_message().await {
            Ok(message) => {
                if let Err(e) = AsyncController::process(&mut session, message).await {
                    eprintln!("Error handling message: {:?}", e);
                    break;
                }
            }
            Err(e) => {
                eprintln!("Error reading message: {:?}", e);
                break;
            }
        }
    }
    
    println!("Connection closed");
    Ok(())
}
