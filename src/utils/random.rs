pub fn is_true(chance: u32, max: u32) -> bool {
    rand::random::<u32>() % max < chance
}

pub fn next_int(min: i32, max: i32) -> i32 {
    if min >= max {
        return min;
    }
    min + (rand::random::<i32>().abs() % (max - min + 1))
}
