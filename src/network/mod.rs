use tokio::net::TcpListener;
use std::io;
use crate::network::async_net::{session::AsyncSession, controller::AsyncController};

pub mod async_net;

pub async fn start_server() -> Result<(), Box<dyn std::error::Error>> {
    let listener = TcpListener::bind("127.0.0.1:14445").await?;
    println!("Server listening on 127.0.0.1:14445");
    
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
    println!("Connection established, waiting for messages...");
    loop {
        match session.read_message().await {
            Ok(message) => {
                println!("Received message - Command: {}, Data size: {} bytes", 
                        message.command, message.data.len());
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
