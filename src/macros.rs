// Macro để viết message ngắn gọn
#[macro_export]
macro_rules! msg_write {
    ($msg:expr, $method:ident($($arg:expr),*)) => {
        $msg.$method($($arg),*)
            .map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?
    };
}

#[macro_export]
macro_rules! msg_write_custom {
    ($msg:expr, $method:ident($($arg:expr),*), $err_msg:expr) => {
        $msg.$method($($arg),*)
            .map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, format!("{}: {}", $err_msg, e))))?
    };
}
