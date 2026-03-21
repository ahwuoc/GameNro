#![allow(dead_code)]
use crate::map::item_map::ItemMap;
use crate::mob::RtMob;
use crate::network::message::Message;
use crate::network::session::SessionArc;
use crate::player::player_actor::PlayerHandle;
use crate::utils::location::Location;
use anyhow::Result;
use tokio::sync::{mpsc, oneshot};

pub enum ZoneMessage {
    AddPlayer {
        handle: PlayerHandle,
    },
    RemovePlayer {
        id: u64,
    },
    AddMob {
        mob: RtMob,
    },
    RemoveMob {
        id: u64,
    },
    AddItem {
        item: ItemMap,
    },
    RemoveItem {
        id: i32,
        tx: oneshot::Sender<Option<ItemMap>>,
    },
    GetPlayer {
        id: u64,
        tx: oneshot::Sender<Option<PlayerHandle>>,
    },
    GetAllPlayers {
        tx: oneshot::Sender<Vec<PlayerHandle>>,
    },
    GetAllMobs {
        tx: oneshot::Sender<Vec<RtMob>>,
    },
    GetAllItems {
        tx: oneshot::Sender<Vec<ItemMap>>,
    },
    GetZoneInfo {
        tx: oneshot::Sender<ZoneInfo>,
    },
    MapInfo {
        session: SessionArc,
        player_id: u64,
        x: i16,
        y: i16,
        task_info: Option<(i32, i32)>,
        spaceship_id: i8,
    },
    LoadAnotherToMe {
        player_id: u64,
    },
    LoadMeToAnother {
        player_id: u64,
    },
    UpdateTick,
    StartStunMob {
        mob_id: u64,
        time_stun: u64,
    },
    AttackMob {
        player_id: u64,
        mob_id: u64,
        damage: i32,
        is_crit: bool,
        die_when_hp_full: bool,
        player_power: i64,
    },
    SyncMobEffects {
        mob_id: u64,
        effect_skill: crate::models::EffectSkill,
    },
    RemoveMobHold {
        mob_id: u64,
        caster_id: u64,
    },
    RemovePlayerHold {
        target_id: u64,
        caster_id: u64,
    },
    Broadcast {
        msg: Message,
        except_id: Option<u64>,
    },
    AreaDamage {
        attacker_id: u64,
        x: i16,
        y: i16,
        range: i16,
        damage: i64,
        is_player: bool,
        die_when_hp_full: bool,
        player_power: i64,
    },
    CheckSpawnTaskItem {
        player_id: u64,
        task_info: (i32, i32),
    },
}

#[derive(Clone, Debug, Default)]
pub struct ZonePublicState {
    pub mob_alive_count: i32,
    pub player_count: i32,
    pub has_boss: bool,
}

#[derive(Clone, Debug)]
pub struct ZoneHandle {
    pub map_id: i32,
    pub zone_id: i32,
    pub tx: mpsc::Sender<ZoneMessage>,
    pub public_state: std::sync::Arc<tokio::sync::RwLock<ZonePublicState>>,
}

impl ZoneHandle {
    pub async fn add_player(&self, handle: PlayerHandle) -> Result<()> {
        self.tx.send(ZoneMessage::AddPlayer { handle }).await?;
        Ok(())
    }

    pub async fn remove_player(&self, id: u64) -> Result<()> {
        self.tx.send(ZoneMessage::RemovePlayer { id }).await?;
        Ok(())
    }

    pub async fn add_mob(&self, mob: RtMob) -> Result<()> {
        self.tx.send(ZoneMessage::AddMob { mob }).await?;
        Ok(())
    }

    pub async fn remove_mob(&self, id: u64) -> Result<()> {
        self.tx.send(ZoneMessage::RemoveMob { id }).await?;
        Ok(())
    }

    pub async fn start_stun_mob(&self, mob_id: u64, time_stun: u64) -> Result<()> {
        self.tx
            .send(ZoneMessage::StartStunMob { mob_id, time_stun })
            .await?;
        Ok(())
    }

    pub async fn get_all_mobs(&self) -> Result<Vec<RtMob>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllMobs { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn add_item(&self, item: ItemMap) -> Result<()> {
        self.tx.send(ZoneMessage::AddItem { item }).await?;
        Ok(())
    }

    pub async fn remove_item(&self, id: i32) -> Result<Option<ItemMap>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::RemoveItem { id, tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_item(&self, id: i32) -> Result<Option<ItemMap>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllItems { tx }).await?;
        let items = rx.await?;
        Ok(items.into_iter().find(|i| i.item_map_id == id))
    }

    pub async fn get_all_items(&self) -> Result<Vec<ItemMap>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllItems { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_player(&self, id: u64) -> Result<Option<PlayerHandle>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetPlayer { id, tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_all_players(&self) -> Result<Vec<PlayerHandle>> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetAllPlayers { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn get_zone_info(&self) -> Result<ZoneInfo> {
        let (tx, rx) = oneshot::channel();
        self.tx.send(ZoneMessage::GetZoneInfo { tx }).await?;
        Ok(rx.await?)
    }

    pub async fn map_info(
        &self,
        session: SessionArc,
        player_id: u64,
        x: i16,
        y: i16,
        task_info: Option<(i32, i32)>,
        spaceship_id: i8,
    ) -> Result<()> {
        self.tx
            .send(ZoneMessage::MapInfo {
                session,
                player_id,
                x,
                y,
                task_info,
                spaceship_id,
            })
            .await?;
        Ok(())
    }

    pub async fn load_another_to_me(&self, player_id: u64) -> anyhow::Result<()> {
        self.tx
            .send(ZoneMessage::LoadAnotherToMe { player_id })
            .await?;
        Ok(())
    }

    pub async fn load_me_to_another(&self, player_id: u64) -> Result<()> {
        self.tx
            .send(ZoneMessage::LoadMeToAnother { player_id })
            .await?;
        Ok(())
    }

    pub fn broadcast(&self, msg: Message) {
        let _ = self.tx.try_send(ZoneMessage::Broadcast {
            msg,
            except_id: None,
        });
    }

    pub fn mob_effects(&self, mob_id: u64, effect_skill: crate::models::EffectSkill) {
        let _ = self.tx.try_send(ZoneMessage::SyncMobEffects {
            mob_id,
            effect_skill,
        });
    }

    pub fn remove_mob_hold(&self, mob_id: u64, caster_id: u64) {
        let _ = self
            .tx
            .try_send(ZoneMessage::RemoveMobHold { mob_id, caster_id });
    }

    pub fn remove_player_hold(&self, target_id: u64, caster_id: u64) {
        let _ = self.tx.try_send(ZoneMessage::RemovePlayerHold {
            target_id,
            caster_id,
        });
    }

    pub fn broadcast_except(&self, msg: Message, except_id: u64) {
        let _ = self.tx.try_send(ZoneMessage::Broadcast {
            msg,
            except_id: Some(except_id),
        });
    }

    pub async fn check_spawn_task_item(&self, player_id: u64, task_info: (i32, i32)) -> Result<()> {
        self.tx
            .send(ZoneMessage::CheckSpawnTaskItem {
                player_id,
                task_info,
            })
            .await?;
        Ok(())
    }
}

pub struct ZoneInfo {
    pub map_id: i32,
    pub zone_id: i32,
    pub max_player: i32,
    pub current_players: i32,
    pub mob_count: i32,
    pub item_count: i32,
}
