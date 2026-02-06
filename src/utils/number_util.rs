pub fn number_to_money(power: i64) -> String {
    let (val, unit) = if power >= 1_000_000_000 {
        (power as f64 / 1_000_000_000.0, " Tỷ")
    } else if power >= 1_000_000 {
        (power as f64 / 1_000_000.0, " Tr")
    } else if power >= 1_000 {
        (power as f64 / 1_000.0, " k")
    } else {
        return power.to_string();
    };

    let s = format!("{:.1}", val);
    let s = s.trim_end_matches('0').trim_end_matches('.');
    format!("{}{}", s, unit)
}
