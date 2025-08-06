use chrono::Local;
use std::collections::HashMap;
use std::io::{self, BufRead, BufReader, Cursor, Read, Write};
use std::net::{TcpListener, TcpStream};
pub mod DataGame;
pub mod Friend;
pub mod Inventory;
pub mod Item;
pub mod Location;
pub mod OptionCard;
pub mod Player;
pub mod PlayerSkill;
pub mod SideTaskTemplate;
pub mod Skill;
pub mod TaskMain;
pub mod TaskPlayer;
pub mod Zone;
pub mod controller;
pub mod game_session;
pub mod message;
pub mod nPoint;
use crate::controller::MessageController;
use crate::game_session::GameSession;
use std::sync::{
    Arc, Mutex,
    atomic::{AtomicBool, AtomicUsize, Ordering},
};
use std::thread;
use std::time::{Duration, SystemTime};

const SERVER_NAME: &str = "Ahwuodz";
const DEFAULT_PORT: u16 = 14445;
const MAX_PER_IP: usize = 3;
#[derive(Debug, Clone)]
struct ClientConnection {
    count: usize,
    last_seen: SystemTime,
}

pub struct ServerManager {
    is_running: Arc<AtomicBool>,
    time_start: String,
    port: u16,

    clients: Arc<Mutex<HashMap<String, ClientConnection>>>,
    active_players: Arc<AtomicUsize>,
}

impl ServerManager {
    fn new() -> Self {
        ServerManager {
            is_running: Arc::new(AtomicBool::new(true)),
            time_start: Local::now().format("%d/%m/%Y %H:%M:%S").to_string(),
            port: DEFAULT_PORT,
            clients: Arc::new(Mutex::new(HashMap::new())),
            active_players: Arc::new(AtomicUsize::new(0)),
        }
    }

    fn init(&self) {
        println!("Initializing basic server components...");
        println!("Server initialization complete");
    }

    // Main entry point
    fn run(&self) {
        println!("Starting server...");
        self.print_banner();

        // Start network socket
        self.start_network_server();

        // Start command line interface
        self.start_command_line();

        // Simple main server loop
        self.main_server_loop();
    }

    fn print_banner(&self) {
        println!("\x1b[32mTime start server: {}\x1b[0m", self.time_start);
        println!(
            "\x1b[30m{}\x1b[0m",
            r#"
  _   _ _____   ____   __          __     _____   _____
 | \ | |  __ \ / __ \  \ \        / /\   |  __ \ / ____|
 |  \| | |__) | |  | |  \ \  /\  / /  \  | |__) | (___
 | . ` |  _  /| |  | |   \ \/  \/ / /\ \ |  _  / \___ \
 | |\  | | \ \| |__| |    \  /\  / ____ \| | \ \ ____) |
 |_| \_|_|  \_\\____/      \/  \/_/    \_\_|  \_\_____/
        "#
        );
    }

    // Network server - equivalent to activeServerSocket() in Java
    fn start_network_server(&self) {
        let is_running = Arc::clone(&self.is_running);
        let clients = Arc::clone(&self.clients);
        let active_players = Arc::clone(&self.active_players);
        let port = self.port;

        thread::spawn(move || {
            Self::run_server_socket(is_running, clients, active_players, port);
        });
    }

    fn run_server_socket(
        is_running: Arc<AtomicBool>,
        clients: Arc<Mutex<HashMap<String, ClientConnection>>>,
        active_players: Arc<AtomicUsize>,
        port: u16,
    ) {
        let addr = format!("0.0.0.0:{}", port);

        match TcpListener::bind(&addr) {
            Ok(listener) => {
                println!("Listening on {}", addr);

                // Set non-blocking for graceful shutdown
                if let Err(e) = listener.set_nonblocking(true) {
                    println!("Warning: Could not set non-blocking: {}", e);
                }

                while is_running.load(Ordering::Relaxed) {
                    match listener.accept() {
                        Ok((stream, peer_addr)) => {
                            let ip = peer_addr.ip().to_string();
                            println!("Connection attempt from: {}", ip);

                            if Self::can_connect_with_ip(&ip, &clients) {
                                let clients_clone = Arc::clone(&clients);
                                let active_players_clone = Arc::clone(&active_players);

                                thread::spawn(move || {
                                    Self::handle_client(
                                        stream,
                                        ip,
                                        clients_clone,
                                        active_players_clone,
                                    );
                                });
                            } else {
                                println!("Connection rejected from {}: IP limit exceeded", ip);
                                let _ = stream.shutdown(std::net::Shutdown::Both);
                            }
                        }
                        Err(ref e) if e.kind() == io::ErrorKind::WouldBlock => {
                            // Non-blocking mode, no new connections
                            thread::sleep(Duration::from_millis(100));
                        }
                        Err(e) => {
                            println!("Error accepting connection: {}", e);
                            thread::sleep(Duration::from_millis(1000));
                        }
                    }
                }
                println!("Server socket closed");
            }
            Err(e) => {
                println!("Error binding to {}: {}", addr, e);
            }
        }
    }

    fn can_connect_with_ip(
        ip: &str,
        clients: &Arc<Mutex<HashMap<String, ClientConnection>>>,
    ) -> bool {
        let mut clients_map = clients.lock().unwrap();

        match clients_map.get_mut(ip) {
            Some(connection) => {
                if connection.count < MAX_PER_IP {
                    connection.count += 1;
                    connection.last_seen = SystemTime::now();
                    println!("IP {} now has {} connections", ip, connection.count);
                    true
                } else {
                    println!(
                        "IP {} rejected: {} connections already",
                        ip, connection.count
                    );
                    false
                }
            }
            None => {
                clients_map.insert(
                    ip.to_string(),
                    ClientConnection {
                        count: 1,
                        last_seen: SystemTime::now(),
                    },
                );
                println!("New IP {} connected (first connection)", ip);
                true
            }
        }
    }
    fn handle_client(
        mut stream: TcpStream,
        ip: String,
        clients: Arc<Mutex<HashMap<String, ClientConnection>>>,
        active_players: Arc<AtomicUsize>,
    ) {
        let ip_clone = ip.clone();
        let session = GameSession::new(ip_clone.clone(), stream.try_clone().unwrap(), ip_clone);

        let session_id = session.id.clone();
        println!("New session created: {} from {}", session_id, ip.clone());

        active_players.fetch_add(1, Ordering::Relaxed);

        let controller = MessageController::new();
        let game_session = Arc::new(Mutex::new(GameSession::new(
            session_id.clone(),
            stream.try_clone().expect("Failed to clone stream"),
            ip.clone(),
        )));
        controller.add_session(game_session);
        if let Err(e) = stream.set_nonblocking(true) {
            println!("Warning: Could not set stream non-blocking: {}", e);
        }

        let mut buffer = vec![0u8; 1024];
        let mut message_buffer = Vec::new();

        loop {
            match stream.read(&mut buffer) {
                Ok(0) => {
                    println!("Client {} disconnected gracefully", ip);
                    break;
                }
                Ok(n) => {
                    message_buffer.extend_from_slice(&buffer[..n]);

                    while message_buffer.len() >= 3 {
                        let command = message_buffer[0] as i8;
                        let length =
                            u16::from_be_bytes([message_buffer[1], message_buffer[2]]) as usize;

                        if message_buffer.len() >= 3 + length {
                            let message_data = message_buffer[3..3 + length].to_vec();
                            let message = message::Message::new(command, message_data);

                            controller.handle_message(&session_id, message);

                            message_buffer.drain(..3 + length);
                        } else {
                            break;
                        }
                    }
                }
                Err(ref e) if e.kind() == io::ErrorKind::WouldBlock => {
                    thread::sleep(Duration::from_millis(10));
                }
                Err(e) => {
                    println!("Error reading from stream {}: {}", ip, e);
                    break;
                }
            }
        }
        controller.remove_session(&session_id);
        Self::disconnect_client(&ip, &clients);
        active_players.fetch_sub(1, Ordering::Relaxed);

        let _ = stream.shutdown(std::net::Shutdown::Both);
        println!("Session {} disconnected", session_id);
    }

    // Clean up client connection - equivalent to disconnect() in Java
    fn disconnect_client(ip: &str, clients: &Arc<Mutex<HashMap<String, ClientConnection>>>) {
        let mut clients_map = clients.lock().unwrap();

        if let Some(connection) = clients_map.get_mut(ip) {
            if connection.count > 0 {
                connection.count -= 1;
                println!("IP {} now has {} connections", ip, connection.count);

                if connection.count == 0 {
                    clients_map.remove(ip);
                    println!("IP {} removed from client list", ip);
                }
            }
        }
    }

    fn start_command_line(&self) {
        let is_running = Arc::clone(&self.is_running);
        let active_players = Arc::clone(&self.active_players);

        thread::spawn(move || {
            let stdin = io::stdin();
            let reader = BufReader::new(stdin);

            println!("Command line active. Available commands:");
            println!("  baotri  - Start maintenance");
            println!("  athread - Show thread count");
            println!("  nplayer - Show player count");
            println!("  quit    - Shutdown server");

            for line in reader.lines() {
                if let Ok(command) = line {
                    let command = command.trim().to_lowercase();

                    match command.as_str() {
                        "baotri" => {
                            println!("Đang Bảo Trì (Maintenance mode not implemented yet)");
                        }
                        "athread" => {
                            // Rust doesn't have direct thread count, but we can estimate
                            println!("Thread count: [Estimated based on active connections]");
                        }
                        "nplayer" => {
                            let count = active_players.load(Ordering::Relaxed);
                            println!("Số lượng người chơi hiện tại: {}", count);
                        }
                        "quit" | "exit" | "stop" => {
                            println!("Shutting down server...");
                            is_running.store(false, Ordering::Relaxed);
                            break;
                        }
                        "" => {
                            // Empty command, do nothing
                        }
                        _ => {
                            println!("Unknown command: '{}'", command);
                            println!("Available: baotri, athread, nplayer, quit");
                        }
                    }
                }
            }
        });
    }
    fn main_server_loop(&self) {
        println!("Server running. Type commands or 'quit' to stop.");

        while self.is_running.load(Ordering::Relaxed) {
            thread::sleep(Duration::from_secs(1));
        }

        self.shutdown();
    }

    fn shutdown(&self) {
        println!("Server shutting down...");
        println!("SUCCESSFULLY MAINTENANCE!");
        println!("Server stopped");
    }

    fn get_active_players(&self) -> usize {
        self.active_players.load(Ordering::Relaxed)
    }
}

fn main() {
    let server = ServerManager::new();
    server.init();
    server.run();
}
