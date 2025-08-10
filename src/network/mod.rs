use tokio::net::TcpListener;
use std::{env, io};
use dotenv::dotenv;
use crate::network::async_net::{session::AsyncSession, controller::AsyncController};

pub mod async_net;
pub async fn start_server() -> Result<(), Box<dyn std::error::Error>> {
    dotenv().ok();
    let host = env::var("HOST").unwrap_or_else(|_| "127.0.0.1".to_string());
    let port = env::var("PORT").unwrap_or_else(|_| "14445".to_string());
    let addr = format!("{}:{}", host, port);
    let listener = TcpListener::bind(&addr).await?;
    println!("Server listening on {}", addr);
    loop {
        match listener.accept().await {
            Ok((socket, addr)) => {
                println!("New connection from: {}", addr);
                tokio::spawn(async move {
                    if let Err(e) = handle_connection(socket).await {
                        eprintln!("Error handling connection: {}", e);
                    }
                });
            }
            Err(e) => {
                eprintln!("Error accepting connection: {}", e);
            }
        }
    }
}

async fn handle_connection(socket: tokio::net::TcpStream) -> Result<(), Box<dyn std::error::Error>> {
    let mut session = AsyncSession::new(socket);
    loop {
        match session.read_message().await {
            Ok(message) => {
                if let Err(e) = AsyncController::handle_message(&mut session, message.command, message.data).await {
                    eprintln!("Error handling message: {}", e);
                    break;
                }
            }
            Err(e) => {
                eprintln!("Error reading message: {}", e);
                break;
            }
        }
    }
    
    println!("Connection closed");
    Ok(())
}
