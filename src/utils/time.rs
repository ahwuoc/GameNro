use std::time::UNIX_EPOCH;

pub fn current_time_millis() -> u64 {
    std::time::SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_millis() as u64
}

pub fn is_after_midnight(timestamp_millis: i64) -> bool {
    if timestamp_millis <= 0 {
        return true;
    }
    const TIMEZONE_OFFSET: i64 = 7 * 3600;
    let now_secs = current_time_millis() as i64 / 1000;
    let ts_secs = timestamp_millis / 1000;
    let today = (now_secs + TIMEZONE_OFFSET) / 86400;
    let ts_day = (ts_secs + TIMEZONE_OFFSET) / 86400;

    today > ts_day
}
pub fn get_time_left(last_time_millis: i64, second_target: i32) -> String {
    let now = current_time_millis() as i64;
    let second_passed = ((now - last_time_millis) / 1000) as i32;
    let seconds_left = (second_target - second_passed).max(0);

    if seconds_left > 60 {
        format!("{} phút", seconds_left / 60)
    } else {
        format!("{} giây", seconds_left)
    }
}
