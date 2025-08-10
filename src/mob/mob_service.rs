use std::collections::HashMap;
use std::sync::Arc;
use tokio::sync::RwLock;
use crate::mob::RtMob;
use crate::mob::MobDao;
use crate::network::async_net::session::AsyncSession;
use crate::network::async_net::message::Message;
use sea_orm::DatabaseConnection;

pub struct MobService {
    mobs: Arc<RwLock<HashMap<u64, RtMob>>>,
    next_mob_id: Arc<RwLock<u64>>,
    database: Option<DatabaseConnection>,
}

impl MobService {
    pub fn new() -> Self {
        Self {
            mobs: Arc::new(RwLock::new(HashMap::new())),
            next_mob_id: Arc::new(RwLock::new(1)),
            database: None,
        }
    }

    pub fn set_database(&mut self, database: DatabaseConnection) {
        self.database = Some(database);
    }

    pub async fn create_mob(&self, template_id: i32, map_id: u32, zone_id: u32, x: i16, y: i16) -> Option<RtMob> {
        if let Some(ref database) = self.database {
            let mut next_id = self.next_mob_id.write().await;
            let mob_id = *next_id;
            *next_id += 1;

            if let Ok(Some(mut mob)) = MobDao::create_mob_from_template(database, template_id, mob_id).await {
                mob.set_location(map_id, zone_id, x, y);
                
                let mut mobs = self.mobs.write().await;
                mobs.insert(mob_id, mob.clone());
                
                println!("[MOB_SERVICE] Created mob {} (ID: {}) at ({}, {}) on map {}", 
                         mob.name, mob_id, x, y, map_id);
                
                return Some(mob);
            }
        }
        None
    }

    pub async fn get_mob(&self, mob_id: u64) -> Option<RtMob> {
        let mobs = self.mobs.read().await;
        mobs.get(&mob_id).cloned()
    }

    pub async fn get_mobs_in_map(&self, map_id: u32, zone_id: u32) -> Vec<RtMob> {
        let mobs = self.mobs.read().await;
        mobs.values()
            .filter(|mob| mob.get_map_id() == map_id && mob.get_zone_id() == zone_id)
            .cloned()
            .collect()
    }

    pub async fn remove_mob(&self, mob_id: u64) -> bool {
        let mut mobs = self.mobs.write().await;
        if let Some(mob) = mobs.remove(&mob_id) {
            println!("[MOB_SERVICE] Removed mob {} (ID: {})", mob.name, mob_id);
            true
        } else {
            false
        }
    }

    pub async fn update_mob(&self, mob_id: u64, update_fn: impl FnOnce(&mut RtMob)) -> bool {
        let mut mobs = self.mobs.write().await;
        if let Some(mob) = mobs.get_mut(&mob_id) {
            update_fn(mob);
            true
        } else {
            false
        }
    }

    pub async fn damage_mob(&self, mob_id: u64, damage: i32) -> bool {
        self.update_mob(mob_id, |mob| {
            mob.take_damage(damage);
            println!("[MOB_SERVICE] Mob {} (ID: {}) took {} damage, HP: {}/{}", 
                     mob.name, mob_id, damage, mob.hp, mob.max_hp);
        }).await
    }

    pub async fn heal_mob(&self, mob_id: u64, amount: i32) -> bool {
        self.update_mob(mob_id, |mob| {
            mob.heal(amount);
            println!("[MOB_SERVICE] Mob {} (ID: {}) healed {} HP, HP: {}/{}", 
                     mob.name, mob_id, amount, mob.hp, mob.max_hp);
        }).await
    }

    pub async fn move_mob(&self, mob_id: u64, new_x: i16, new_y: i16) -> bool {
        self.update_mob(mob_id, |mob| {
            mob.location.set_position(new_x, new_y);
            println!("[MOB_SERVICE] Mob {} (ID: {}) moved to ({}, {})", 
                     mob.name, mob_id, new_x, new_y);
        }).await
    }

    pub async fn send_mob_info(&self, session: &mut AsyncSession, mob: &RtMob) -> Result<(), Box<dyn std::error::Error>> {
        let mut msg = Message::new_for_writing(-32); // Assuming -32 is mob info command
        msg.write_int(mob.id as i32)?;
        msg.write_int(mob.template_id)?;
        msg.write_utf(&mob.name)?;
        msg.write_int(mob.level)?;
        msg.write_int(mob.hp)?;
        msg.write_int(mob.max_hp)?;
        msg.write_int(mob.mp)?;
        msg.write_int(mob.max_mp)?;
        msg.write_int(mob.get_map_id() as i32)?;
        msg.write_int(mob.get_zone_id() as i32)?;
        msg.write_short(mob.get_x())?;
        msg.write_short(mob.get_y())?;
        msg.write_byte(if mob.is_alive { 1 } else { 0 })?;
        
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        println!("[MOB_SERVICE] Sent mob info for {} (ID: {})", mob.name, mob.id);
        Ok(())
    }

    pub async fn send_mobs_in_map(&self, session: &mut AsyncSession, map_id: u32, zone_id: u32) -> Result<(), Box<dyn std::error::Error>> {
        let mobs = self.get_mobs_in_map(map_id, zone_id).await;
        let mob_count = mobs.len();
        
        for mob in mobs {
            self.send_mob_info(session, &mob).await?;
        }
        
        println!("[MOB_SERVICE] Sent {} mobs in map {} zone {}", mob_count, map_id, zone_id);
        Ok(())
    }

    pub async fn cleanup_dead_mobs(&self) -> usize {
        let mut mobs = self.mobs.write().await;
        let initial_count = mobs.len();
        
        mobs.retain(|_, mob| !mob.is_dead());
        
        let removed_count = initial_count - mobs.len();
        if removed_count > 0 {
            println!("[MOB_SERVICE] Cleaned up {} dead mobs", removed_count);
        }
        
        removed_count
    }

    pub async fn get_mob_count(&self) -> usize {
        let mobs = self.mobs.read().await;
        mobs.len()
    }

    pub async fn get_alive_mob_count(&self) -> usize {
        let mobs = self.mobs.read().await;
        mobs.values().filter(|mob| mob.is_alive).count()
    }
}
