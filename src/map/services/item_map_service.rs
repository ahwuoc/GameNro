use crate::map::item_map::ItemMap;
use crate::network::message::Message;
use std::sync::atomic::{AtomicI32, Ordering};

static NEXT_ITEM_MAP_ID: AtomicI32 = AtomicI32::new(1);
pub fn next_item_map_id() -> i32 {
    let id = NEXT_ITEM_MAP_ID.fetch_add(1, Ordering::Relaxed);
    if id >= 30_000 {
        NEXT_ITEM_MAP_ID.store(1, Ordering::Relaxed);
    }
    id
}

pub struct ItemMapService;

impl ItemMapService {
    pub fn build_item_appear_message(item: &ItemMap) -> Message {
        let mut msg = Message::new(68);
        let _ = msg.write_short(item.item_map_id as i16);
        let _ = msg.write_short(item.get_item_id());
        let _ = msg.write_short(item.x as i16);
        let _ = msg.write_short(item.y as i16);
        let _ = msg.write_int(item.player_id as i32);
        msg
    }

    pub fn build_item_appear_for_me_message(item: &ItemMap) -> Message {
        let mut msg = Message::new(68);
        let _ = msg.write_short(item.item_map_id as i16);
        let _ = msg.write_short(item.get_item_id());
        let _ = msg.write_short(item.x as i16);
        let _ = msg.write_short(item.y as i16);
        let _ = msg.write_int(3);
        msg
    }

    pub fn build_item_disappear_message(item_map_id: i32) -> Message {
        let mut msg = Message::new(-21);
        let _ = msg.write_short(item_map_id as i16);
        msg
    }

    pub fn build_pickup_notification_message(item_map_id: i32, player_id: u64) -> Message {
        let mut msg = Message::new(-19);
        let _ = msg.write_short(item_map_id as i16);
        let _ = msg.write_int(player_id as i32);
        msg
    }
}
