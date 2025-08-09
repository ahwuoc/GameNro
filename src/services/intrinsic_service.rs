use crate::models::{Intrinsic, IntrinsicPlayer};
use crate::player::Player as RtPlayer;
use crate::network::async_net::session::AsyncSession;
use crate::network::async_net::message::Message;
use crate::services::manager::Manager;
use rand::Rng;

pub struct IntrinsicService;

impl IntrinsicService {
    const COST_OPEN: [i32; 8] = [10, 20, 40, 80, 160, 320, 640, 1280];

    pub fn get_intrinsics(&self, player_gender: u8) -> Vec<Intrinsic> {
        let manager = Manager::get_instance();
        let manager_guard = manager.lock().unwrap();
        
        // Filter intrinsics by gender
        manager_guard.get_intrinsic_templates()
            .iter()
            .filter(|intrinsic| intrinsic.gender == player_gender as i16)
            .map(|entity| Intrinsic::from_entity(entity))
            .collect()
    }

    pub fn get_intrinsic_by_id(&self, id: i32) -> Option<Intrinsic> {
        let manager = Manager::get_instance();
        let manager_guard = manager.lock().unwrap();
        
        manager_guard.get_intrinsic_template_by_id(id)
            .map(|entity| Intrinsic::from_entity(entity))
    }

    pub async fn send_info_intrinsic(&self, session: &mut AsyncSession, _player: &RtPlayer) -> Result<(), Box<dyn std::error::Error>> {
        let player_intrinsic = IntrinsicPlayer::new();
        let mut msg = Message::new_for_writing(112);
        msg.write_byte(0).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.write_short(player_intrinsic.intrinsic.icon).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.write_utf(&player_intrinsic.intrinsic.get_name()).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.finalize_write();
        session.send_message(&msg).await?;

        msg.cleanup();

        Ok(())
    }

    pub async fn show_all_intrinsic(&self, session: &mut AsyncSession, player_gender: u8) -> Result<(), Box<dyn std::error::Error>> {
        let list_intrinsic = self.get_intrinsics(player_gender);
        let mut msg = Message::new_for_writing(112);
        
        msg.write_byte(1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.write_byte(1).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?; // count tab
        msg.write_utf("Nội tại").map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        msg.write_byte((list_intrinsic.len() - 1) as i8).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        
        for i in 1..list_intrinsic.len() {
            let intrinsic = &list_intrinsic[i];
            msg.write_short(intrinsic.icon).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
            msg.write_utf(&intrinsic.get_description()).map_err(|e| Box::new(std::io::Error::new(std::io::ErrorKind::Other, e)))?;
        }
        
        msg.finalize_write();
        session.send_message(&msg).await?;
        
        msg.cleanup();
        
        Ok(())
    }

    pub async fn show_menu(&self, _session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        // TODO: Implement NPC service menu creation
        // This would typically call NpcService to create a menu
        println!("Showing intrinsic menu");
        Ok(())
    }

    pub async fn show_confirm_open(&self, _session: &mut AsyncSession, count_open: u8) -> Result<(), Box<dyn std::error::Error>> {
        let index = if count_open as usize >= Self::COST_OPEN.len() {
            Self::COST_OPEN.len() - 1
        } else {
            count_open as usize
        };
        
        let cost = Self::COST_OPEN[index];
        println!("Confirming open intrinsic with cost: {} Tr vàng", cost);
        // TODO: Implement NPC service menu creation
        Ok(())
    }

    pub async fn show_confirm_open_vip(&self, _session: &mut AsyncSession) -> Result<(), Box<dyn std::error::Error>> {
        println!("Confirming open intrinsic VIP with cost: 100 ngọc");
        // TODO: Implement NPC service menu creation
        Ok(())
    }

    fn change_intrinsic(&self, player_intrinsic: &mut IntrinsicPlayer, player_gender: u8) {
        let list_intrinsic = self.get_intrinsics(player_gender);
        if list_intrinsic.len() > 1 {
            let mut rng = rand::thread_rng();
            let random_index = rng.gen_range(1..list_intrinsic.len());
            let selected_intrinsic = &list_intrinsic[random_index];
            
            player_intrinsic.intrinsic = Intrinsic::from_intrinsic(selected_intrinsic);
            
            // Set random parameters
            player_intrinsic.intrinsic.param1 = rng.gen_range(selected_intrinsic.param_from_1..=selected_intrinsic.param_to_1);
            player_intrinsic.intrinsic.param2 = rng.gen_range(selected_intrinsic.param_from_2..=selected_intrinsic.param_to_2);
        }
    }

    pub fn open(&self, player_intrinsic: &mut IntrinsicPlayer, player_gender: u8, player_power: i64, player_gold: i64) -> Result<String, String> {
        if player_power < 10_000_000_000 {
            return Err("Yêu cầu sức mạnh tối thiểu 10 tỷ".to_string());
        }

        let index = if player_intrinsic.count_open as usize >= Self::COST_OPEN.len() {
            Self::COST_OPEN.len() - 1
        } else {
            player_intrinsic.count_open as usize
        };

        let gold_require = Self::COST_OPEN[index] as i64 * 1_000_000;
        
        if player_gold >= gold_require {
            self.change_intrinsic(player_intrinsic, player_gender);
            player_intrinsic.count_open += 1;
            
            let intrinsic_name = player_intrinsic.intrinsic.get_name();
            let name_part = if let Some(bracket_pos) = intrinsic_name.find(" [") {
                &intrinsic_name[..bracket_pos]
            } else {
                &intrinsic_name
            };
            
            Ok(format!("Bạn nhận được Nội tại:\n{}", name_part))
        } else {
            let missing = gold_require - player_gold;
            Err(format!("Bạn không đủ vàng, còn thiếu {} vàng nữa", missing))
        }
    }

    pub fn open_vip(&self, player_intrinsic: &mut IntrinsicPlayer, player_gender: u8, player_power: i64, player_gem: i32) -> Result<String, String> {
        if player_power < 10_000_000_000 {
            return Err("Yêu cầu sức mạnh tối thiểu 10 tỷ".to_string());
        }

        let gem_require = 100;
        
        if player_gem >= gem_require {
            self.change_intrinsic(player_intrinsic, player_gender);
            player_intrinsic.count_open = 0;
            
            let intrinsic_name = player_intrinsic.intrinsic.get_name();
            let name_part = if let Some(bracket_pos) = intrinsic_name.find(" [") {
                &intrinsic_name[..bracket_pos]
            } else {
                &intrinsic_name
            };
            
            Ok(format!("Bạn nhận được Nội tại:\n{}", name_part))
        } else {
            let missing = gem_require - player_gem;
            Err(format!("Bạn không có đủ ngọc, còn thiếu {} ngọc nữa", missing))
        }
    }
}
