use crate::matches::{TypeLosePvp, TypePvp};
use crate::player::player_actor::message::PlayerMessage;
use crate::player::player_actor::TypePk;
use crate::player::player_manager::PLAYER_MANAGER;
use crate::services::ServiceHandles;

pub trait PvpMatch: Send + Sync {
    fn pvp_type(&self) -> TypePvp;
    fn player1_id(&self) -> i64;
    fn player2_id(&self) -> i64;

    fn is_started(&self) -> bool;
    fn start(&mut self);
    fn finish(&mut self);
    fn update(&mut self);
    fn reward(&mut self, winner_id: i64);
    fn send_result(&self, loser_id: i64, type_lose: TypeLosePvp);

    fn pk_type(&self) -> TypePk {
        TypePk::PkPvp
    }

    fn is_in_pvp(&self, player_id: i64) -> bool {
        self.player1_id() == player_id || self.player2_id() == player_id
    }

    fn get_winner_id(&self, loser_id: i64) -> i64 {
        if loser_id == self.player1_id() {
            self.player2_id()
        } else {
            self.player1_id()
        }
    }

    fn lose(&mut self, loser_id: i64, type_lose: TypeLosePvp) {
        if !self.is_started() {
            return;
        }
        let winner_id = self.get_winner_id(loser_id);
        self.finish();
        self.reward(winner_id);
        self.send_result(loser_id, type_lose);
    }

    fn dispose(&self) {
        change_type_pk(self.player1_id(), TypePk::PkNon);
        change_type_pk(self.player2_id(), TypePk::PkNon);
    }
}

pub fn change_type_pk(player_id: i64, type_pk: TypePk) {
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        handle.send_forget(PlayerMessage::Modify(Box::new(move |player| {
            player.type_pk = type_pk;
            let _ = ServiceHandles::send_type_pk(player);
        })));
    }
}

pub fn send_thong_bao(player_id: i64, text: &str) {
    let text = text.to_string();
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        let handle = handle.clone();
        tokio::spawn(async move {
            if let Some(snapshot) = handle.get_snapshot().await {
                let _ = ServiceHandles::send_thong_bao_to_player(&snapshot, &text);
            }
        });
    }
}

pub fn send_money(player_id: i64) {
    if let Some(handle) = PLAYER_MANAGER.get(player_id as u64) {
        let handle = handle.clone();
        tokio::spawn(async move {
            if let Some(snapshot) = handle.get_snapshot().await {
                let _ = ServiceHandles::send_gold_gem_ruby_to_client(&snapshot);
            }
        });
    }
}
